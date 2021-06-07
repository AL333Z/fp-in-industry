package api

import cats.effect._
import cats.effect.unsafe.implicits.global
import data.params.{ Company, Email, OrderNo, PagingCriteria }
import data.{ ItemId, Order, OrderLine, Price }
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.scalatest.funsuite.AnyFunSuite

class OrderHistoryRoutesTest extends AnyFunSuite {

  test("get orders") {

    val sampleOrder: Order =
      Order(
        orderNo = OrderNo("001"),
        company = Company("ACME"),
        email = Email("test@mail.com"),
        lines = List(
          OrderLine(
            lineNo = 1,
            itemId = ItemId("AAA"),
            price = Price(BigDecimal(10.00))
          )
        )
      )

    val repo: OrderRepository = new OrderRepository {
      override def findBy(email: Email, company: Company, pagingCriteria: PagingCriteria): IO[List[Order]] =
        IO(List(sampleOrder))

      override def findBy(company: Company, orderNo: OrderNo): IO[Option[Order]] = ???
    }

    val sut = OrderHistoryRoutes.fromRepo(repo)

    val response: IO[Response[IO]] = sut.orNotFound.run(
      Request(method = Method.GET, uri = uri"/XXX/orders?email=test@test.com")
    )

    val expectedJson = Json.arr(
      Json.obj(
        ("orderNo", Json.fromString(sampleOrder.orderNo.value)),
        ("company", Json.fromString(sampleOrder.company.value)),
        ("email", Json.fromString(sampleOrder.email.value)),
        ("lines",
         Json.arr(
           Json.obj(
             ("lineNo", Json.fromInt(sampleOrder.lines.head.lineNo)),
             ("itemId", Json.fromString(sampleOrder.lines.head.itemId.value)),
             ("price", Json.fromBigDecimal(sampleOrder.lines.head.price.value))
           )
         ))
      )
    )

    assert(check[Json](response, Status.Ok, Some(expectedJson)))
  }

  def check[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])(
    implicit ev: EntityDecoder[IO, A]
  ): Boolean = {
    val actualResp  = actual.unsafeRunSync()
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck = expectedBody.fold[Boolean](actualResp.body.compile.toVector.unsafeRunSync().isEmpty)(
      expected => actualResp.as[A].unsafeRunSync() === expected
    )
    statusCheck && bodyCheck
  }
}
