package api

import cats.effect.{ ConcurrentEffect, IO }
import cats.implicits._
import data.Order
import data.params._
import fs2.interop.reactivestreams._
import mongo.ReactiveStreams._
import org.mongodb.scala.{ Document, MongoCollection }

trait OrderRepository {
  def findBy(email: Email, enterpriseCode: EnterpriseCode, pagingCriteria: PagingCriteria): IO[List[Order]]

  def findBy(enterpriseCode: EnterpriseCode, orderNo: OrderNo): IO[Option[Order]]
}

object OrderRepository {

  def from(collection: MongoCollection[Document])(implicit ce: ConcurrentEffect[IO]): OrderRepository =
    new OrderRepository {

      def findBy(
        email: Email,
        enterpriseCode: EnterpriseCode,
        pagingCriteria: PagingCriteria
      ): IO[List[Order]] =
        collection
          .find(
            Document(
              "email"          -> email.value,
              "enterpriseCode" -> enterpriseCode.value
            )
          )
          .skip(pagingCriteria.pageNo.value * pagingCriteria.pageSize.value)
          .limit(pagingCriteria.pageSize.value)
          .toPublisher // FIXME avoid Publisher -> Stream -> IO[List]
          .toStream[IO]()
          .compile
          .toList
          .flatMap(
            _.traverse(doc => IO.fromTry(Order.fromBson(doc)))
          )

      def findBy(enterpriseCode: EnterpriseCode, orderNo: OrderNo): IO[Option[Order]] =
        collection
          .find(
            Document(
              "orderNo"        -> orderNo.value,
              "enterpriseCode" -> enterpriseCode.value
            )
          )
          .first()
          .toPublisher // FIXME avoid Publisher -> Stream -> IO[List]
          .toStream[IO]()
          .compile
          .toList
          .flatMap(_ match {
            case Nil    => IO.pure(None)
            case x :: _ => IO.fromTry(Order.fromBson(x)).map(Some(_))
          })
    }
}
