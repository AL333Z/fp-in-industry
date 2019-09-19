package data.params

import cats.implicits._
import data.params.PageNo.PageNoQueryParam
import data.params.PageSize.PageSizeQueryParam
import org.http4s.QueryParamDecoder

case class PagingCriteria(pageNo: PageNo, pageSize: PageSize)

object PagingCriteriaQueryParam {

  def unapply(params: Map[String, collection.Seq[String]]): Option[PagingCriteria] = {
    val pageNoOpt   = PageNoQueryParam.unapply(params)
    val pageSizeOpt = PageSizeQueryParam.unapply(params)
    (pageNoOpt, pageSizeOpt).mapN(PagingCriteria.apply)
  }
}

case class PageNo(value: Int)

object PageNo {
  val default: PageNo = PageNo(0)

  implicit val pageNoParamDecoder: QueryParamDecoder[PageNo] =
    QueryParamDecoder[Int].map(PageNo(_))

  object PageNoQueryParam extends OptionalWithDefaultQueryParamsDecoderMatcher[PageNo]("pageNo", default)

}

case class PageSize(value: Int)

object PageSize {
  val default: PageSize = PageSize(10)

  implicit val pageSizeParamDecoder: QueryParamDecoder[PageSize] =
    QueryParamDecoder[Int].map(PageSize(_))

  object PageSizeQueryParam extends OptionalWithDefaultQueryParamsDecoderMatcher[PageSize]("pageSize", default)

}
