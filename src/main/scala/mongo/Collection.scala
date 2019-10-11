package mongo

import cats.effect.{ ConcurrentEffect, IO }
import cats.implicits._
import fs2.interop.reactivestreams._
import mongo.Mongo._
import mongo.ReactiveStreams._
import org.mongodb.scala.{ Document, MongoCollection }
import fs2.Stream

class Collection(private val wrapped: MongoCollection[Document])(implicit ce: ConcurrentEffect[IO]) {

  def insertOne(document: Document): IO[Unit] =
    wrapped
      .insertOne(document)
      .toIO
      .void

  def find(document: Document, skip: Int, limit: Int): Stream[IO, Document] =
    wrapped
      .find(document)
      .skip(skip)
      .limit(limit)
      .toPublisher // FIXME avoid Publisher -> Stream -> IO[List]
      .toStream[IO]()

  def findFirst(document: Document): Stream[IO, Document] =
    wrapped
      .find(document)
      .first()
      .toPublisher // FIXME avoid Publisher -> Stream -> IO[List]
      .toStream[IO]()
}
