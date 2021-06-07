package api

import cats.effect.{ IO, Resource }
import mongo.Mongo
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

class OrderHistory private (routes: HttpRoutes[IO]) {

  val serve: IO[Unit] =
    BlazeServerBuilder[IO](scala.concurrent.ExecutionContext.global)
      .bindHttp(80, "0.0.0.0")
      .withHttpApp(Logger.httpApp[IO](logHeaders = true, logBody = true)(routes.orNotFound))
      .serve
      .compile
      .drain
}

object OrderHistory {

  def fromConfig(mongoConfig: Mongo.Config): Resource[IO, OrderHistory] =
    Mongo
      .collectionFrom(mongoConfig)
      .map { collection =>
        val orderRepo    = OrderRepository.fromCollection(collection)
        val orderService = OrderHistoryRoutes.fromRepo(orderRepo)
        new OrderHistory(orderService)
      }
}
