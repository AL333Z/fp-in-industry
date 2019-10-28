package projector

import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, IO, Resource }
import cats.implicits._
import dev.profunktor.fs2rabbit.config.Fs2RabbitConfig
import dev.profunktor.fs2rabbit.model.{ AckResult, AmqpEnvelope }
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
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
    (for {
      _ <- consumer.evalMap { envelope =>
            for {
              _ <- logger.info("Received: " + envelope)
              _ <- envelope.payload match {
                    case Success(event) =>
                      eventRepo.store(event) *>
                        acker(AckResult.Ack(envelope.deliveryTag))
                    case Failure(e) =>
                      logger.error(e)("Error while decoding") *>
                        acker(AckResult.NAck(envelope.deliveryTag))
                  }
            } yield ()
          }
    } yield ()).compile.drain
}

object OrderHistoryProjector {

  type Acker    = AckResult => IO[Unit]
  type Consumer = Stream[IO, AmqpEnvelope[Try[OrderCreatedEvent]]]

  def fromConfigs(
    mongoConfig: Mongo.Config,
    rabbitConfig: Fs2RabbitConfig
  )(implicit ce: ConcurrentEffect[IO], cs: ContextShift[IO]): Resource[IO, OrderHistoryProjector] =
    for {
      collection <- Mongo.collectionFrom(mongoConfig)
      logger     <- Resource.liftF(Slf4jLogger.create[IO])
      // FIXME used only for publish ops, so here it's pretty useless..
      // waiting for https://github.com/profunktor/fs2-rabbit/pull/255 to be released
      blocker <- Blocker.apply[IO]
      (acker, consumer) <- Rabbit.consumerFrom(
                            rabbitConfig,
                            blocker,
                            OrderCreatedEvent.orderCreatedEventEnvelopeDecoder
                          )
      repo = EventRepository.fromCollection(collection)
    } yield new OrderHistoryProjector(repo, consumer, acker, logger)
}
