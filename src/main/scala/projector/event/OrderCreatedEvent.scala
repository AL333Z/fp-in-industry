package projector.event

import cats.effect.IO
import cats.implicits._
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{ Decoder, _ }

import java.nio.charset.StandardCharsets.UTF_8
import scala.util.Try

case class OrderCreatedEvent(
  id: String,
  company: String,
  email: String,
  lines: List[OrderLine]
)

case class OrderLine(
  no: Int,
  item: String,
  price: BigDecimal
)

object OrderLine {
  implicit val orderLineEvtDecoder: Decoder[OrderLine] = deriveDecoder[OrderLine]
}

object OrderCreatedEvent {
//  {
//    "id": "001",
//    "company": "ACME",
//    "email": "asdf@asdf.com",
//    "lines": [
//      {
//        "no": 1,
//        "item": "jeans",
//        "price": 100
//      }
//    ]
//  }

  implicit val orderEvtDecoder: Decoder[OrderCreatedEvent] = deriveDecoder[OrderCreatedEvent]

  val orderCreatedEventEnvelopeDecoder: EnvelopeDecoder[IO, Try[OrderCreatedEvent]] = {
    (EnvelopeDecoder.payload[IO], EnvelopeDecoder.properties[IO]).mapN {
      case (payload, props) =>
        parser.decode(new String(payload, props.contentEncoding.getOrElse(UTF_8.toString))).toTry
    }
  }
}
