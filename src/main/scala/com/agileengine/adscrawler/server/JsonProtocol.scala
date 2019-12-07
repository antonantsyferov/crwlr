package com.agileengine.adscrawler.server

import com.agileengine.adscrawler.domain.Relationship.{Direct, Relationship, Reseller}
import com.agileengine.adscrawler.domain._
import spray.json._

/**
  * Spray json format definitions based on the domain model.
  */
object JsonProtocol extends DefaultJsonProtocol {
  private val DirectJsonValue = "DIRECT"
  private val ResellerJsonValue = "RESELLER"

  implicit val relationshipFormat: RootJsonFormat[Relationship] =
    new RootJsonFormat[Relationship] {
      override def read(json: JsValue): Relationship = json match {
        case JsString(DirectJsonValue)   => Direct
        case JsString(ResellerJsonValue) => Reseller
        case _                           => deserializationError(s"Unsupported value '$json'")
      }

      override def write(obj: Relationship): JsValue = obj match {
        case Direct   => JsString(DirectJsonValue)
        case Reseller => JsString(ResellerJsonValue)
      }
    }

  implicit val sellerFormat: RootJsonFormat[Seller] = jsonFormat4(Seller.apply)
  implicit val publisherFormat: RootJsonFormat[Publisher] = jsonFormat2(Publisher.apply)
  implicit val publisherDataFormat: RootJsonFormat[PublisherData] = jsonFormat2(PublisherData.apply)
}
