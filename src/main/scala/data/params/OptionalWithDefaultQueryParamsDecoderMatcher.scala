package data.params

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl._

abstract class OptionalWithDefaultQueryParamsDecoderMatcher[T: QueryParamDecoder](name: String, default: => T) {

  def unapply(params: Map[String, collection.Seq[String]]): Option[T] =
    new OptionalQueryParamDecoderMatcher[T](name) {}.unapply(params).map(_.getOrElse(default))
}
