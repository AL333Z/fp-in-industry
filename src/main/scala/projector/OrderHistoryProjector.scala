package projector

import cats.effect.{ IO, Resource }
import dev.profunktor.fs2rabbit.config.Fs2RabbitConfig
import dev.profunktor.fs2rabbit.model.{ AckResult, AmqpEnvelope }
import fs2.Stream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import mongo.Mongo
import projector.OrderHistoryProjector.{ Acker, Consumer }
import projector.event.OrderCreatedEvent
import rabbit.Rabbit

import scala.util.{ Failure, Success, Try }

class OrderHistoryProjector private[projector] (
  eventRepo: EventRepository,
  consumer: Consumer,
  acker: Acker,
  logger: Logger[IO]
) {

  val project: IO[Unit] =
    consumer.evalMap { envelope =>
      envelope.payload match {
        case Success(event) =>
          logger.info("Received: " + envelope) *>
            eventRepo.store(event) *>
            acker(AckResult.Ack(envelope.deliveryTag))
        case Failure(e) =>
          logger.error(e)("Error while decoding") *>
            acker(AckResult.NAck(envelope.deliveryTag))
      }
    }.compile.drain
}

object OrderHistoryProjector {

  type Acker    = AckResult => IO[Unit]
  type Consumer = Stream[IO, AmqpEnvelope[Try[OrderCreatedEvent]]]

  def fromConfigs(
    mongoConfig: Mongo.Config,
    rabbitConfig: Fs2RabbitConfig
  ): Resource[IO, OrderHistoryProjector] =
    for {
      collection <- Mongo.collectionFrom(mongoConfig)
      logger     <- Resource.eval(Slf4jLogger.create[IO])
      (acker, consumer) <- Rabbit.consumerFrom(
                            rabbitConfig,
                            OrderCreatedEvent.orderCreatedEventEnvelopeDecoder
                          )
      repo = EventRepository.fromCollection(collection)
    } yield new OrderHistoryProjector(repo, consumer, acker, logger)
}
