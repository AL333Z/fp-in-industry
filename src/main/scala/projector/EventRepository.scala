package projector

import cats.effect.IO
import data._
import mongo4cats.database.MongoCollectionF
import projector.event.OrderCreatedEvent

trait EventRepository {
  def store(event: OrderCreatedEvent): IO[Unit]
}

object EventRepository {

  def fromCollection(collection: MongoCollectionF[Order]): EventRepository =
    new EventRepository {
      override def store(event: OrderCreatedEvent): IO[Unit] =
        collection
          .insertOne[IO](
            Order(
              orderNo = OrderNo(event.id),
              company = Company(event.company),
              email = Email(event.email),
              lines = event.lines.map(
                line =>
                  OrderLine(
                    lineNo = LineNo(line.no),
                    itemId = ItemId(line.item),
                    price = Price(line.price)
                )
              )
            )
          )
          .void
    }
}
