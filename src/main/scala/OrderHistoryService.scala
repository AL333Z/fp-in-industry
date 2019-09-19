import cats.effect.IO
import data.params.Email.EmailQueryParam
import data.params.EnterpriseCode.EnterpriseCodeVar
import data.params.OrderNo.OrderNoVar
import data.params.PagingCriteriaQueryParam
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._

object OrderHistoryService {

  def from(orderRepository: OrderRepository): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / EnterpriseCodeVar(enterpriseCode) / "orders"
          :? EmailQueryParam(email)
            +& PagingCriteriaQueryParam(pagingCriteria) =>
      orderRepository
        .findBy(
          email = email,
          enterpriseCode = enterpriseCode,
          pagingCriteria = pagingCriteria
        )
        .flatMap(Ok(_))

    case GET -> Root / EnterpriseCodeVar(enterpriseCode) / "orders" / OrderNoVar(orderNo) / "details" =>
      orderRepository
        .findBy(enterpriseCode, orderNo)
        .flatMap {
          case Some(order) => Ok(order)
          case None        => NoContent()
        }
  }

}
