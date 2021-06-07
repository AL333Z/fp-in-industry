package mongo

import cats.effect.IO
import fs2.Stream
//import mongo.Mongo._
import org.mongodb.scala.{ Document, MongoCollection }

// TODO
class Collection private (private val wrapped: MongoCollection[Document]) {

  def insertOne(document: Document): IO[Unit] = ???
//    wrapped
//      .insertOne(document)
//      .toIO
//      .void

  def find(document: Document, skip: Int, limit: Int): Stream[IO, Document] = ???
  //    wrapped
  //      .find(document)
  //      .skip(skip)
  //      .limit(limit)
  //      .toPublisher // FIXME avoid Publisher -> Stream -> IO[List]
  //      .toStream[IO]()

  def findFirst(document: Document): Stream[IO, Document] = ???
  //    wrapped
  //      .find(document)
  //      .first()
  //      .toPublisher // FIXME avoid Publisher -> Stream -> IO[List]
  //      .toStream[IO]()
}

object Collection {

  def apply(wrapped: MongoCollection[Document]): Collection =
    new Collection(wrapped)
}
