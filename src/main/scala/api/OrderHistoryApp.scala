package api

import cats.effect.{ ExitCode, IO, IOApp }
import mongo.Mongo

object OrderHistoryApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      config <- Mongo.Config.load
      _ <- OrderHistory
            .fromConfig(config)
            .use(_.serve)
    } yield ExitCode.Success
}
