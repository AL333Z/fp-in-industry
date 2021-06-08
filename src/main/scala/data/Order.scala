package data

import cats.implicits._
import io.circe.generic.semiauto._
import io.circe.{ Codec, Decoder, Encoder }
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.{ ParseFailure, QueryParamDecoder, QueryParameterValue }

case class Order(
  orderNo: OrderNo,
  company: Company,
  email: Email,
  lines: List[OrderLine]
)

case class OrderNo(value: String) extends AnyVal

object OrderNo {
  implicit val orderNoCodec: Codec[OrderNo] =
    Codec.from(
      decodeA = Decoder.decodeString.map(OrderNo(_)),
      encodeA = Encoder.encodeString.contramap(_.value)
    )

  object OrderNoVar {
    def unapply(arg: String): Option[OrderNo] = Some(OrderNo(arg))
  }

}

case class Email(value: String) extends AnyVal

object Email {

  implicit val emailCodec: Codec[Email] =
    Codec.from(
      decodeA = Decoder.decodeString.map(Email(_)),
      encodeA = Encoder.encodeString.contramap(_.value)
    )

  // incomplete/wrong..
  private def validate(x: String): Option[String] =
    """(\w+)@([\w\.]+)""".r.findFirstIn(x)

  implicit val emailQueryParamDecoder: QueryParamDecoder[Email] = (value: QueryParameterValue) => {
    validate(value.value)
      .map(Email(_))
      .toValidNel(ParseFailure("Invalid Email", value.value))
  }
  object EmailQueryParam extends QueryParamDecoderMatcher[Email]("email")(emailQueryParamDecoder)

}

case class Company(value: String) extends AnyVal

object Company {
  implicit val companyCodec: Codec[Company] =
    Codec.from(
      decodeA = Decoder.decodeString.map(Company(_)),
      encodeA = Encoder.encodeString.contramap(_.value)
    )

  object CompanyVar {
    def unapply(arg: String): Option[Company] = Some(Company(arg))
  }

}

case class OrderLine(
  lineNo: LineNo,
  itemId: ItemId,
  price: Price
)

case class LineNo(value: Int) extends AnyVal

case class ItemId(value: String) extends AnyVal

case class Price(value: BigDecimal) extends AnyVal

object Order {
  implicit val orderCodec: Codec[Order] =
    deriveCodec[Order]
}

object OrderLine {
  implicit val orderLineCodec: Codec[OrderLine] =
    deriveCodec[OrderLine]
}

object LineNo {
  implicit val lineNoCodec: Codec[LineNo] =
    Codec.from(
      decodeA = Decoder.decodeInt.map(LineNo(_)),
      encodeA = Encoder.encodeInt.contramap(_.value)
    )
}

object ItemId {
  implicit val itemIdCodec: Codec[ItemId] =
    Codec.from(
      decodeA = Decoder.decodeString.map(ItemId(_)),
      encodeA = Encoder.encodeString.contramap(_.value)
    )
}

object Price {
  implicit val priceCodec: Codec[Price] =
    Codec.from(
      decodeA = Decoder.decodeBigDecimal.map(Price(_)),
      encodeA = Encoder.encodeBigDecimal.contramap(_.value)
    )
}
