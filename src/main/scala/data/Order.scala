package data

import cats.implicits._
import data.params.{ Email, OrderNo }
import io.circe.generic.semiauto._
import io.circe.{ Encoder, Json }
import org.mongodb.scala.bson.collection.immutable.Document

import scala.collection.JavaConverters._
import scala.util.Try

case class Order(
  orderNo: OrderNo,
  email: Email,
  lines: List[OrderLine]
)

case class OrderLine(
  lineNo: Int,
  itemId: ItemId,
  price: Price
)

case class ItemId(value: String)

case class Price(value: BigDecimal)

object Order {
  implicit val orderEncoder: Encoder[Order] = deriveEncoder[Order]

  def fromBson(doc: Document): Try[Order] =
    for {
      lineDocs <- Try(
                   doc // FIXME too ugly to be real..
                     .getList("lines", Document.getClass)
                     .asInstanceOf[java.util.List[Document]]
                     .asScala
                     .toList
                 )
      lines <- lineDocs.traverse[Try, OrderLine](OrderLine.fromBson)
      order <- Try(
                Order(
                  OrderNo(doc.getString("orderNo")),
                  Email(doc.getString("email")),
                  lines
                )
              )
    } yield order
}

object OrderLine {
  implicit val orderLineEncoder: Encoder[OrderLine] = deriveEncoder[OrderLine]

  def fromBson(doc: Document): Try[OrderLine] = Try {
    OrderLine(
      doc.getInteger("lineNo"),
      ItemId(doc.getString("itemId")),
      Price(BigDecimal(doc.getDouble("price")))
    )
  }
}

object ItemId {
  implicit val itemIdEncoder: Encoder[ItemId] = Encoder.instance { x =>
    Json.fromString(x.value)
  }
}

object Price {
  implicit val priceEncoder: Encoder[Price] = Encoder.instance { x =>
    Json.fromBigDecimal(x.value)
  }
}
