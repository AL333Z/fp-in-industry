package api

import cats.effect.{ ConcurrentEffect, IO }
import cats.implicits._
import data.Order
import data.params._
import mongo.Collection
import org.mongodb.scala.Document

trait OrderRepository {
  def findBy(email: Email, company: Company, pagingCriteria: PagingCriteria): IO[List[Order]]

  def findBy(company: Company, orderNo: OrderNo): IO[Option[Order]]
}

object OrderRepository {

  def from(collection: Collection)(implicit ce: ConcurrentEffect[IO]): OrderRepository =
    new OrderRepository {

      def findBy(
        email: Email,
        company: Company,
        pagingCriteria: PagingCriteria
      ): IO[List[Order]] =
        collection
          .find(
            document = Document(
              "email"   -> email.value,
              "company" -> company.value
            ),
            skip = pagingCriteria.pageNo.value * pagingCriteria.pageSize.value,
            limit = pagingCriteria.pageSize.value
          )
          .compile
          .toList
          .flatMap(
            _.traverse(doc => IO.fromTry(Order.fromBson(doc)))
          )

      def findBy(company: Company, orderNo: OrderNo): IO[Option[Order]] =
        collection
          .findFirst(
            Document(
              "orderNo" -> orderNo.value,
              "company" -> company.value
            )
          )
          .compile
          .toList
          .flatMap(_ match {
            case Nil    => IO.pure(None)
            case x :: _ => IO.fromTry(Order.fromBson(x)).map(Some(_))
          })
    }
}
