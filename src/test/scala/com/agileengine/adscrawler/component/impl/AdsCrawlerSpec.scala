package com.agileengine.adscrawler.component.impl

import com.agileengine.adscrawler.component.{AdsCrawler, Loader, Parser, Repository}
import com.agileengine.adscrawler.domain.Relationship.{Direct, Reseller}
import com.agileengine.adscrawler.domain.{Publisher, PublisherData, Seller}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class AdsCrawlerSpec extends AsyncFlatSpec with Matchers with AsyncMockFactory with GivenWhenThen {
  it should "run the full workflow: load, parse, and return the data" in {
    Given("test publishers")
    val publisher1 = Publisher("pub1", "http://pub1.com")
    val publisher2 = Publisher("pub2", "http://pub2.com")

    And("Mocked components defined")
    val pub1Sellers = List(Seller("1", "2", Direct, None))
    val pub2Sellers = List(Seller("1", "2", Direct, None), Seller("3", "4", Reseller, Some("5")))

    val parser = mock[Parser]
    (parser.parse _)
      .expects("pub1-content")
      .returns(pub1Sellers)
      .once()

    (parser.parse _)
      .expects("pub2-content")
      .returns(pub2Sellers)
      .once()

    val loader = mock[Loader]
    (loader.load _)
      .expects(publisher1.url)
      .returns(Future.successful("pub1-content"))
      .once()
    (loader.load _)
      .expects(publisher2.url)
      .returns(Future.successful("pub2-content"))
      .once()

    val repository = mock[Repository]
    (repository.persist _)
      .expects(Seq(PublisherData(publisher1, pub1Sellers), PublisherData(publisher2, pub2Sellers)))
      .returns(Future.unit)
      .once()
    (repository.getSellers _)
      .expects(where { v: String =>
        publisher1.name.equalsIgnoreCase(v)
      })
      .returns(Future.successful(pub1Sellers))
      .twice()
    (repository.getSellers _)
      .expects(publisher2.name)
      .returns(Future.successful(pub2Sellers))
      .once()
    (repository.close _)
      .expects()
      .returns(Future.unit)
      .once()

    When("Crawler instance is built with defined components")
    val crawler = new AdsCrawler(parser, loader, repository, List(publisher1, publisher2))

    for {
      _                     <- crawler.init()
      actualPub1Sellers     <- crawler.getSellers(publisher1.name)
      actualPub1SellersCase <- crawler.getSellers(publisher1.name.toUpperCase)
      actualPub2Sellers     <- crawler.getSellers(publisher2.name)
      _                     <- crawler.close()
    } yield {
      Then("Behaviour should be expected")
      actualPub1Sellers should contain theSameElementsAs pub1Sellers
      actualPub1SellersCase should contain theSameElementsAs pub1Sellers
      actualPub2Sellers should contain theSameElementsAs pub2Sellers
    }
  }
}
