package api

import cats.effect.IO
import data.params.Company.CompanyVar
import data.params.Email.EmailQueryParam
import data.params.OrderNo.OrderNoVar
import data.params.PagingCriteriaQueryParam
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io.{ +&, ->, /, :?, GET, NoContent, Ok, Root, _ }

object OrderHistoryService {

  def from(orderRepository: OrderRepository): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / CompanyVar(company) / "orders"
          :? EmailQueryParam(email)
            +& PagingCriteriaQueryParam(pagingCriteria) =>
      orderRepository
        .findBy(
          email = email,
          company = company,
          pagingCriteria = pagingCriteria
        )
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
