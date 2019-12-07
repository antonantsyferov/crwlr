package com.agileengine.adscrawler.component.impl

import com.agileengine.adscrawler.component.Repository
import com.agileengine.adscrawler.component.impl.Tables._
import com.agileengine.adscrawler.domain.Relationship.Relationship
import com.agileengine.adscrawler.domain.{PublisherData, Relationship, Seller}
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType

import scala.concurrent.{ExecutionContext, Future}

/**
  * H2 in-memory implementation.
  */
class H2MemRepository(implicit ec: ExecutionContext) extends Repository {
  private lazy val db = Database.forConfig("h2mem1")

  override def persist(publisherData: Seq[PublisherData]): Future[Unit] = {
    val publisherEntities = publisherData
      .map(_.publisher)
      .map(p => PublisherEntity(p.name, p.url))

    val setupAction: DBIO[Unit] =
      DBIO.seq(
        (publishersQuery.schema ++ sellersQuery.schema).create,
        publishersQuery ++= publisherEntities
      )

    for {
      _                 <- db.run(setupAction)
      publisherEntities <- db.run(publishersQuery.result)
      sellerEntities    = toSellerEntities(publisherEntities, publisherData)
      _                 <- db.run(sellersQuery ++= sellerEntities)
    } yield ()
  }

  private def toSellerEntities(
    publishers: Seq[Tables.PublisherEntity],
    publisherData: Seq[PublisherData]
  ): Seq[Tables.SellerEntity] = {
    val mapping = publisherData.map(d => (d.publisher.name, d.sellers)).toMap
    def buildSellerEntities(p: PublisherEntity): Seq[SellerEntity] =
      mapping(p.name)
        .map(e => SellerEntity(p.id, e.domain, e.accountId, e.relationship, e.authority))

    publishers.flatMap(buildSellerEntities)
  }

  override def getSellers(publisherName: String): Future[Seq[Seller]] = {
    val sellersByPublisherNameQuery = publishersQuery
      .filter(_.name.toUpperCase === publisherName.toUpperCase)
      .join(sellersQuery)
      .on(_.id === _.publisherId)
      .map {
        case (_, s) => s
      }

    for {
      dbEntities <- db.run(sellersByPublisherNameQuery.result)
      sellers    = dbEntities.map(e => Seller(e.domain, e.accountId, e.relationship, e.authority))
    } yield sellers
  }

  override def close(): Future[Unit] = Future.successful(db.close())
}

/**
  * Schema definition.
  */
object Tables {
  case class SellerEntity(
    publisherId: Int,
    domain: String,
    accountId: String,
    relationship: Relationship,
    authority: Option[String],
    id: Int = 0
  )
  case class PublisherEntity(name: String, url: String, id: Int = 0)

  class Publishers(tag: Tag) extends Table[PublisherEntity](tag, "PUBLISHERS") {
    def id = column[Int]("PUBLISHER_ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME", O.Unique)
    def url = column[String]("URL")
    def nameIndex = index("NAME_IDX", name, unique = true)
    def * = (name, url, id).mapTo[PublisherEntity]
  }

  val publishersQuery = TableQuery[Publishers]

  private implicit val relationshipMapper: JdbcType[Relationship] with BaseTypedType[Relationship] =
    MappedColumnType.base[Relationship, String](e => e.toString, s => Relationship.withName(s))

  class Sellers(tag: Tag) extends Table[SellerEntity](tag, "SELLERS") {
    def id = column[Int]("SELLER_ID", O.PrimaryKey, O.AutoInc)
    def domain = column[String]("DOMAIN")
    def accountId = column[String]("ACCOUNT_ID")
    def relationship = column[Relationship]("RELATIONSHIP")
    def authority = column[Option[String]]("AUTHORITY")
    def publisherId = column[Int]("PUBLISHER_ID")
    def publisherFk =
      foreignKey("PUBLISHER_FK", publisherId, publishersQuery)(
        _.id,
        ForeignKeyAction.Restrict,
        ForeignKeyAction.Cascade
      )
    def publisherIdIndex = index("PUBLISHER_ID_IDX", publisherId)
    def uniqueIndex =
      index(
        "SELLER_UNIQUE_IDX",
        (domain, accountId, relationship, authority, publisherId),
        unique = true
      )

    def * =
      (publisherId, domain, accountId, relationship, authority, id).mapTo[SellerEntity]
  }

  val sellersQuery = TableQuery[Sellers]
}
