package com.agileengine.adscrawler.component.impl

import com.agileengine.adscrawler.domain.Relationship.{Direct, Reseller}
import com.agileengine.adscrawler.domain.{Publisher, PublisherData, Seller}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}

class H2MemRepositorySpec
    extends AsyncFlatSpec
    with Matchers
    with GivenWhenThen
    with BeforeAndAfterAll {

  private val repository = new H2MemRepository()
  it should "save objects to database and then return them on demand" in {
    Given("test data")

    val pub1 = Publisher("PUB1", "http://publisher.com/ads.txt")
    val pub1Sellers = List(
      Seller("google.com", "pub-5995202563537249", Direct),
      Seller("openx.com", "540191398", Reseller, Some("6a698e2ec38604c6"))
    )
    val pub2 = Publisher("PUB2", "https://publisher2.com/ads.txt")
    val pub2Sellers = List(Seller("extralongurl" * 100, "1" * 1000, Direct))

    val pub3 = Publisher("PUB3", "http://publisher3.com/ads.txt")
    val pub3Sellers = List(
      Seller("google.com", "pub-5995202563537249", Direct),
      Seller("appnexus.com", "1356", Reseller, Some("1e1d41537f7cad7f")),
      Seller("appnexus.com", "1357", Reseller, Some("1e1d41537f7cad7f"))
    )

    val pub4 = Publisher("PUB4", "http://publisher4.com/ads.txt")

    val data = List(
      PublisherData(pub1, pub1Sellers),
      PublisherData(pub2, pub2Sellers),
      PublisherData(pub3, pub3Sellers)
    )

    When("save and then load the data from DB")
    for {
      _                 <- repository.persist(data)
      actualPub1Sellers <- repository.getSellers(pub1.name)
      actualPub2Sellers <- repository.getSellers(pub2.name.toLowerCase)
      actualPub3Sellers <- repository.getSellers(pub3.name)
      actualPub4Sellers <- repository.getSellers(pub4.name)
      emptySellers      <- repository.getSellers("somenonexistingname")
    } yield {
      Then("results should be expected")
      actualPub1Sellers should contain theSameElementsAs pub1Sellers
      actualPub2Sellers should contain theSameElementsAs pub2Sellers
      actualPub3Sellers should contain theSameElementsAs pub3Sellers
      actualPub4Sellers shouldBe empty
      emptySellers shouldBe empty
    }
  }

  override protected def afterAll(): Unit = repository.close()
}
