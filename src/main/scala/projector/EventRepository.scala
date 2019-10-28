package projector

import cats.effect.IO
import mongo.Collection
import org.mongodb.scala.bson.collection.immutable.Document
import projector.event.OrderCreatedEvent

trait EventRepository {
  def store(event: OrderCreatedEvent): IO[Unit]
}

object EventRepository {

  def fromCollection(collection: Collection): EventRepository =
    new EventRepository {
      override def store(event: OrderCreatedEvent): IO[Unit] =
        collection.insertOne(
          Document(
            "id"      -> event.id,
            "company" -> event.company,
            "email"   -> event.email,
            "lines" -> event.lines.map(
              line =>
                Document(
                  "no"    -> line.no,
                  "item"  -> line.item,
                  "price" -> line.price
              )
            )
          )
        )
    }
}
