package api

import cats.effect.{ ContextShift, IO, Resource, Timer }
import mongo.{ Collection, Mongo }
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

class OrderHistory private (collection: Collection)(implicit ce: ContextShift[IO], ti: Timer[IO]) {

  val serve: IO[Unit] = {
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
}

object OrderHistory {

  def from(mongoConfig: Mongo.Config)(implicit ce: ContextShift[IO], ti: Timer[IO]): Resource[IO, OrderHistory] =
    Mongo
      .collectionFrom(mongoConfig)
      .map(collection => new OrderHistory(collection))
}
