package data.params

import io.circe.{ Encoder, Json }

case class Company(value: String)

object Company {
  implicit val companyEncoder: Encoder[Company] = Encoder.instance { x =>
    Json.fromString(x.value)
  }

  object CompanyVar {
    def unapply(arg: String): Option[Company] = Some(Company(arg))
  }

}
