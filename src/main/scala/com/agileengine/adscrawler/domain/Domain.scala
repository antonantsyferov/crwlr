package com.agileengine.adscrawler.domain

import com.agileengine.adscrawler.domain.Relationship.Relationship

/**
  * Represents a publisher (ads.txt owner).
  * @param name unique publisher identifier.
  * @param url leading to ads.txt file.
  */
case class Publisher(name: String, url: String)

/**
  * Represents a publisher grouped together with its authorized sellers.
  */
case class PublisherData(publisher: Publisher, sellers: Seq[Seller])

/**
  * Represents a seller record within ads.txt.
  * https://iabtechlab.com/~iabtec5/wp-content/uploads/2016/07/IABOpenRTBAds.txtSpecification_Version1_Final.pdf
  * @param domain domain name of the advertising system.
  * @param accountId publisher’s Account ID.
  * @param relationship type of Account/Relationship.
  * @param authority Certification Authority ID.
  */
case class Seller(
  domain: String,
  accountId: String,
  relationship: Relationship,
  authority: Option[String] = None
)

/**
  * An enumeration of the type of account.
  */
object Relationship extends Enumeration {
  type Relationship = Value

  /**
    * Indicates that the Publisher (content owner) directly controls the account.
    */
  val Direct: Relationship.Value = Value("DIRECT")

  /**
    * Indicates that the Publisher has authorized another entity to control the account.
    */
  val Reseller: Relationship.Value = Value("RESELLER")
}
