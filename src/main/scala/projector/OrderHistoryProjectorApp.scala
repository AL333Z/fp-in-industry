package projector

import cats.effect.{IO, IOApp}
import mongo.Mongo
import rabbit.Rabbit

object OrderHistoryProjectorApp extends IOApp.Simple {

  override def run: IO[Unit] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load
      _ <- OrderHistoryProjector
            .fromConfigs(mongoConfig, rabbitConfig)
            .use(_.project)
    } yield ()

}
