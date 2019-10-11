package projector

import cats.effect.{ Blocker, ExitCode, IO, IOApp }
import cats.implicits._
import dev.profunktor.fs2rabbit.model.AckResult
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mongo.Mongo
import org.mongodb.scala.bson.collection.immutable.Document
import projector.event.OrderCreatedEvent
import rabbit.Rabbit

import scala.util.{ Failure, Success }

object OrderHistoryProjector extends IOApp {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load

      resources = for {
        collection <- Mongo.collectionFrom(mongoConfig)
        // FIXME used only for publish ops, so here it's pretty useless..
        // waiting for https://github.com/profunktor/fs2-rabbit/pull/255 to be released
        blocker <- Blocker.apply[IO]
        (rabbitAcker, rabbitConsumer) <- Rabbit.consumerFrom(
                                          rabbitConfig,
                                          blocker,
                                          OrderCreatedEvent.orderCreatedEventEnvelopeDecoder
                                        )
      } yield (collection, rabbitAcker, rabbitConsumer)

      _ <- (for {
            (collection, acker, consumer) <- Stream.resource(resources)
            _ <- consumer.evalMap { envelope =>
                  for {
                    _ <- logger.info("Received: " + envelope)
                    _ <- envelope.payload match {
                          case Success(event) =>
                            collection.insertOne(toDocument(event)) *>
                              acker(AckResult.Ack(envelope.deliveryTag))
                          case Failure(e) =>
                            logger.error(e)("Error while decoding") *>
                              acker(AckResult.NAck(envelope.deliveryTag))
                        }
                  } yield ()
                }
          } yield ()).compile.drain
    } yield ExitCode.Success

  def toDocument(event: OrderCreatedEvent): Document =
    Document(
      "id"      -> event.id,
      "company" -> event.company,
      "email"   -> event.email,
      "lines" -> event.lines.map(
        line =>
          Document(
            "no"    -> line.no,
            "item"  -> line.item,
            "price" -> line.price
        )
      )
    )
}
