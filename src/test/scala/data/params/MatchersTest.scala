package data.params

import org.http4s.QueryParamDecoder._
import org.scalatest.funsuite.AnyFunSuite

class MatchersTest extends AnyFunSuite {

  test("extract optional with default") {
    val optWithDefaultQueryParam: OptionalWithDefaultQueryParamsDecoderMatcher[String] =
      new OptionalWithDefaultQueryParamsDecoderMatcher[String]("bar", "foo") {}

    assert(optWithDefaultQueryParam.unapply(Map()).get === "foo")
    assert(optWithDefaultQueryParam.unapply(Map("bar" -> List("baz"))).get === "baz")
  }

}
