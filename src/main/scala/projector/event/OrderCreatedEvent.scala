package projector.event

import java.nio.charset.StandardCharsets.UTF_8

import cats.effect.IO
import cats.implicits._
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe._
import org.mongodb.scala.bson.collection.immutable.Document

import scala.util.Try

case class OrderCreatedEvent(
  id: String,
  company: String,
  email: String,
  lines: List[OrderLine]
) {

  def toDocument: Document =
    Document(
      "id"      -> id,
      "company" -> company,
      "email"   -> email,
      "lines"   -> lines.map(_.toDocument)
    )
}

case class OrderLine(
  no: Int,
  item: String,
  price: BigDecimal
) {

  def toDocument: Document =
    Document(
      "no"    -> no,
      "item"  -> item,
      "price" -> price
    )
}

object OrderLine {
  implicit val orderLineEvtDecoder: Decoder[OrderLine] = deriveDecoder[OrderLine]
}

object OrderCreatedEvent {
//  {
//  "id": "001",
//  "company": "ACME",
//  "email": "asdf@asdf.com",
//  "lines": [
//  {
//  "no": 1,
//  "item": "jeans",
//  "price": 100
//  }
//  ]
//  }

  implicit val orderEvtDecoder: Decoder[OrderCreatedEvent] = deriveDecoder[OrderCreatedEvent]

  val orderCreatedEventEnvelopeDecoder: EnvelopeDecoder[IO, Try[OrderCreatedEvent]] = {
    (EnvelopeDecoder.payload[IO], EnvelopeDecoder.properties[IO]).mapN {
      case (payload, props) =>
        Try {
          val jsonString = new String(payload, props.contentEncoding.getOrElse(UTF_8.toString))
          parser.parse(jsonString).toTry.get.as[OrderCreatedEvent].toTry.get
        }
    }
  }
}
