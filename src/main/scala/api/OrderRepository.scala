package api

import cats.effect.IO
import com.mongodb.client.model.Filters
import data.params._
import data.{ Company, Email, Order, OrderNo }
import mongo4cats.database.MongoCollectionF

trait OrderRepository {
  def findBy(email: Email, company: Company, pagingCriteria: PagingCriteria): IO[List[Order]]

  def findBy(company: Company, orderNo: OrderNo): IO[Option[Order]]
}

object OrderRepository {

  def fromCollection(collection: MongoCollectionF[Order]): OrderRepository =
    new OrderRepository {

      def findBy(
        email: Email,
        company: Company,
        pagingCriteria: PagingCriteria
      ): IO[List[Order]] =
        collection
          .find(
            Filters.and(
              Filters.eq("email", email.value),
              Filters.eq("company", company.value)
            )
          )
          .stream[IO] // TODO handle pagination, for real :)
          .drop((pagingCriteria.pageNo.value * pagingCriteria.pageSize.value).longValue) // in the lib there's no skip...
          .take(pagingCriteria.pageSize.value.longValue)
          .compile
          .toList

      def findBy(company: Company, orderNo: OrderNo): IO[Option[Order]] =
        collection
          .find(
            Filters.and(
              Filters.eq("orderNo", orderNo.value),
              Filters.eq("company", company.value)
            )
          )
          .stream[IO]
          .compile
          .toList
          .map(_.headOption)
    }
}
