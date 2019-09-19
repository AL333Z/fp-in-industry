import cats.effect.IO
import data.Order
import data.params._

trait OrderRepository {

  def findBy(
    email: Email,
    enterpriseCode: EnterpriseCode,
    pagingCriteria: PagingCriteria,
  ): IO[List[Order]]

  def findBy(enterpriseCode: EnterpriseCode, orderNo: OrderNo): IO[Option[Order]]
}

object OrderRepository {

  def from(): OrderRepository =
    new OrderRepository {

      def findBy(
        email: Email,
        enterpriseCode: EnterpriseCode,
        pagingCriteria: PagingCriteria,
      ): IO[List[Order]] = ???

      def findBy(enterpriseCode: EnterpriseCode, orderNo: OrderNo): IO[Option[Order]] =
        ???
    }
}
