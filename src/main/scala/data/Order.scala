package data

import data.params.{ Email, OrderNo }
import io.circe.generic.semiauto._
import io.circe.{ Encoder, Json }

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
}

object OrderLine {
  implicit val orderLineEncoder: Encoder[OrderLine] = deriveEncoder[OrderLine]
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
