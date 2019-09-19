package data.params

import io.circe.{ Encoder, _ }

case class OrderNo(value: String)

object OrderNo {

  implicit val orderNoEncoder: Encoder[OrderNo] = Encoder.instance { x =>
    Json.fromString(x.value)
  }

  object OrderNoVar {
    def unapply(arg: String): Option[OrderNo] = Some(OrderNo(arg))
  }

}
