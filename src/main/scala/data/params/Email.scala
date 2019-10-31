package data.params

import cats.implicits._
import io.circe.{ Encoder, Json }
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.{ ParseFailure, QueryParamDecoder, QueryParameterValue }

case class Email(value: String)

object Email {

  implicit val emailEncoder: Encoder[Email] = Encoder.instance { x =>
    Json.fromString(x.value)
  }

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
