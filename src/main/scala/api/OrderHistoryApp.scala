package api

import cats.effect.{ ExitCode, IO, IOApp }
import mongo.Mongo

object OrderHistoryApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig <- Mongo.Config.load
      _ <- OrderHistory
            .from(mongoConfig)
            .use(_.serve)
    } yield ExitCode.Success
}
