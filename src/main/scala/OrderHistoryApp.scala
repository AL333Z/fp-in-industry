import cats.effect.{ ExitCode, IO, IOApp, Resource }
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

object OrderHistoryApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Resource.make(IO(()))(_ => IO.unit).use { _ =>
      val orderRepo    = OrderRepository.from()
      val orderService = OrderHistoryService.from(orderRepo)
      val loggedApp    = Logger.httpApp[IO](logHeaders = true, logBody = false)(orderService.orNotFound)

      BlazeServerBuilder[IO]
        .bindHttp(80, "0.0.0.0")
        .withHttpApp(loggedApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
}
