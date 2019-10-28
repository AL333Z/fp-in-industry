package projector

import cats.effect.{ ExitCode, IO, IOApp }
import mongo.Mongo
import rabbit.Rabbit

object OrderHistoryProjectorApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig  <- Mongo.Config.load
      rabbitConfig <- Rabbit.Config.load
      _ <- OrderHistoryProjector
            .fromConfigs(mongoConfig, rabbitConfig)
            .use(_.project)
    } yield ExitCode.Success

}
