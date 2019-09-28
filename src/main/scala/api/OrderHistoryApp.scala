package api

import cats.effect.{ ExitCode, IO, IOApp }
import mongo.Mongo
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

object OrderHistoryApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      mongoConfig        <- Mongo.Config.load
      collectionResource = Mongo.collectionFrom(mongoConfig)
      _ <- collectionResource.use { collection =>
            val orderRepo    = OrderRepository.from(collection)
            val orderService = OrderHistoryService.from(orderRepo)
            val loggedApp    = Logger.httpApp[IO](logHeaders = true, logBody = true)(orderService.orNotFound)

            BlazeServerBuilder[IO]
              .bindHttp(80, "0.0.0.0")
              .withHttpApp(loggedApp)
              .serve
              .compile
              .drain
          }
    } yield ExitCode.Success
}
