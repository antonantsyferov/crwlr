package com.agileengine.adscrawler.component.impl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}

import scala.concurrent.ExecutionContextExecutor

class HttpLoaderSpec
    extends TestKit(ActorSystem("HttpLoaderSpec"))
    with AsyncFlatSpecLike
    with BeforeAndAfterAll
    with Matchers
    with TableDrivenPropertyChecks
    with GivenWhenThen {

  private val wiremock = new WireMockServer(wireMockConfig().dynamicPort())

  private implicit val _executor: ExecutionContextExecutor = system.dispatcher
  private implicit val _materializer: ActorMaterializer = ActorMaterializer()

  override protected def beforeAll(): Unit = wiremock.start()
  override protected def afterAll(): Unit = {
    wiremock.shutdown()
    TestKit.shutdownActorSystem(system)
  }

  it should "load text content by the given URL" in {
    Given("text content")
    val expected =
      """some multiline text
        |to be returned
        |11398718974811325473789 ####
        |contextweb.com, 561632, RESELLER
        |smartclip.net, 11066, DIRECT
        |smartclip.net, 11069, DIRECT
        |i18n интернационализация!
      """.stripMargin
    And("stubbed URL")
    wiremock.stubFor(
      get("/ads.txt")
        .willReturn(aResponse().withStatus(200).withBody(expected))
    )

    When("call loader to fetch the content")
    val actual = new HttpLoader().load(s"http://localhost:${wiremock.port()}/ads.txt")
    Then("actual content should match the expected")
    actual.map(actualText => {
      actualText shouldBe expected
    })
  }

  it should "follow all redirects and load text content by the given URL" in {
    Given("text content")
    val expected =
      """some multiline text
        |to be returned
        |11398718974811325473789 ####
        |contextweb.com, 561632, RESELLER
        |smartclip.net, 11066, DIRECT
        |smartclip.net, 11069, DIRECT
        |i18n интернационализация!
      """.stripMargin
    And("stubbed URLs with redirects")
    val baseUrl = s"http://localhost:${wiremock.port()}"
    wiremock.stubFor(
      get("/ads.txt")
        .willReturn(
          aResponse().withStatus(301).withHeader("Location", s"$baseUrl/catch-me-if-you-can")
        )
    )
    wiremock.stubFor(
      get("/catch-me-if-you-can")
        .willReturn(
          aResponse().withStatus(302).withHeader("Location", s"$baseUrl/newlocation/ads.txt")
        )
    )
    wiremock.stubFor(
      get("/newlocation/ads.txt")
        .willReturn(aResponse().withStatus(303).withHeader("Location", s"$baseUrl/peekaboo.txt"))
    )
    wiremock.stubFor(
      get("/peekaboo.txt")
        .willReturn(aResponse().withStatus(200).withBody(expected))
    )

    When("call loader to fetch the content")
    val actual = new HttpLoader().load(s"http://localhost:${wiremock.port()}/ads.txt")
    Then("actual content should match the expected")
    actual.map(actualText => {
      actualText shouldBe expected
    })
  }
}
