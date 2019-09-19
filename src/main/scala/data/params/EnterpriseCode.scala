package data.params

case class EnterpriseCode(value: String)

object EnterpriseCode {

  object EnterpriseCodeVar {
    def unapply(arg: String): Option[EnterpriseCode] = Some(EnterpriseCode(arg))
  }

}
