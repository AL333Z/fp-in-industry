package projector

import cats.effect.{ Blocker, ExitCode, IO, IOApp }
import mongo.Mongo
import rabbit.Rabbit
import fs2.Stream

object OrderHistoryProjector extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load

      resources = for {
        collection                    <- Mongo.collectionFrom(mongoConfig)
        blocker                       <- Blocker.apply[IO]
        (rabbitAcker, rabbitConsumer) <- Rabbit.consumerFrom(rabbitConfig, blocker)
      } yield (collection, rabbitAcker, rabbitConsumer)

      _ <- Stream
            .resource(resources)
            .map {
              case (collection, rabbitAcker, rabbitConsumer) =>
                ()
            }
            .compile
            .drain

    } yield ExitCode.Success
}
