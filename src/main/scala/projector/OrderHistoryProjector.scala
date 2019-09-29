package projector

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import dev.profunktor.fs2rabbit.model.AckResult
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import mongo.Mongo
import rabbit.Rabbit

object OrderHistoryProjector extends IOApp {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load

      resources = for {
        collection                    <- Mongo.collectionFrom(mongoConfig)
        blocker                       <- Blocker.apply[IO] // used only for publish ops, so here it's pretty useless..
        (rabbitAcker, rabbitConsumer) <- Rabbit.consumerFrom(rabbitConfig, blocker)
      } yield (collection, rabbitAcker, rabbitConsumer)

      _ <- (for {
            (collection, acker, consumer) <- Stream.resource(resources)
            _ <- consumer.evalMap { envelope =>
          logger.info("Received: " + envelope) *>
            acker(AckResult.Ack(envelope.deliveryTag))
        }
          } yield ()).compile.drain
    } yield ExitCode.Success
}
