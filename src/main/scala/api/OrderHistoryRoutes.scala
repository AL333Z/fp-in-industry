package api

import cats.effect.IO
import data.Company.CompanyVar
import data.Email.EmailQueryParam
import data.OrderNo.OrderNoVar
import data.params.PagingCritQueryParam
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io.{ +&, ->, /, :?, GET, NoContent, Ok, Root, _ }

object OrderHistoryRoutes {

  def fromRepo(orderRepository: OrderRepository): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / CompanyVar(company) / "orders"
          :? EmailQueryParam(email)
            +& PagingCritQueryParam(pagingCriteria) =>
      orderRepository
        .findBy(email, company, pagingCriteria)
        .flatMap(Ok(_))

    case GET -> Root / CompanyVar(company) / "orders" / OrderNoVar(orderNo) / "details" =>
      orderRepository
        .findBy(company, orderNo)
        .flatMap {
          case Some(order) => Ok(order)
          case None        => NoContent()
        }
  }

}
