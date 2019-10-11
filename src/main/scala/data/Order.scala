package data

import cats.implicits._
import data.params.{ Company, Email, OrderNo }
import io.circe.generic.semiauto._
import io.circe.{ Encoder, Json }
import org.mongodb.scala.bson.collection.immutable.Document

import scala.collection.JavaConverters._
import scala.util.Try

case class Order(
  orderNo: OrderNo,
  company: Company,
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
      lineDocs <- Try(doc("lines").asArray().asScala.toList.map(x => Document(x.asDocument())))
      lines    <- lineDocs.traverse[Try, OrderLine](OrderLine.fromBson)
      order <- Try(
                Order(
                  OrderNo(doc.getString("id")),
                  Company(doc.getString("company")),
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
      doc.getInteger("no"),
      ItemId(doc.getString("item")),
      Price(doc("price").asDecimal128().getValue.bigDecimalValue())
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
