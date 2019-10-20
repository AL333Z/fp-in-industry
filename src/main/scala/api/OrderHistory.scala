package api

import cats.effect.{ ContextShift, IO, Resource, Timer }
import mongo.Mongo
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

class OrderHistory private (routes: HttpRoutes[IO])(implicit ce: ContextShift[IO], ti: Timer[IO]) {

  val serve: IO[Unit] =
    BlazeServerBuilder[IO]
      .bindHttp(80, "0.0.0.0")
      .withHttpApp(Logger.httpApp[IO](logHeaders = true, logBody = true)(routes.orNotFound))
      .serve
      .compile
      .drain
}

object OrderHistory {

  def from(mongoConfig: Mongo.Config)(implicit ce: ContextShift[IO], ti: Timer[IO]): Resource[IO, OrderHistory] =
    Mongo
      .collectionFrom(mongoConfig)
      .map { collection =>
        val orderRepo    = OrderRepository.from(collection)
        val orderService = OrderHistoryRoutes.from(orderRepo)
        new OrderHistory(orderService)
      }
}
