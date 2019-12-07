package com.agileengine.adscrawler.component.impl

import com.agileengine.adscrawler.domain.Relationship.{Direct, Reseller}
import com.agileengine.adscrawler.domain.Seller
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class RegexParserSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  it should "parse a single string into valid object" in {
    val testData = Table(
      ("input", "expected"),
      ("google.com, pub-50842344, DIRECT", Seller("google.com", "pub-50842344", Direct, None)),
      ("google.com,pub-50842344,DIRECT", Seller("google.com", "pub-50842344", Direct, None)),
      ("google.com, \tpub-50842344,\t\tDIRECT", Seller("google.com", "pub-50842344", Direct, None)),
      ("google.com,pub-50842344,DIRECT,,", Seller("google.com", "pub-50842344", Direct, None)),
      (
        "  google.com   ,  pub-50842344   , DIRECT   ",
        Seller("google.com", "pub-50842344", Direct, None)
      ),
      ("telaria.com, rwgcv-ic8xk, direct", Seller("telaria.com", "rwgcv-ic8xk", Direct, None)),
      (
        "google.com, pub-4893568787652330, DIRECT # ,commentafter",
        Seller("google.com", "pub-4893568787652330", Direct, None)
      ),
      ("123, 456, DIRECT, 789", Seller("123", "456", Direct, Some("789"))),
      (
        "google.com, pub-4893568787652330, RESELLER, f08c47fec0942fa0 # GJAdx",
        Seller("google.com", "pub-4893568787652330", Reseller, Some("f08c47fec0942fa0"))
      ),
      ("$*&^, !)(@*, RESELLER, !)(*@", Seller("$*&^", "!)(@*", Reseller, Some("!)(*@"))),
      (
        "yieldmo.com, Fusion%20Media%20Group, DIRECT",
        Seller("yieldmo.com", "Fusion%20Media%20Group", Direct, None)
      ),
      (
        "google.com, pub-7439281311086140, DIRECT, f08c47fec0942fa0 # banner, video, native",
        Seller("google.com", "pub-7439281311086140", Direct, Some("f08c47fec0942fa0"))
      ),
      ("test, 123, rEsEllEr", Seller("test", "123", Reseller, None))
    )

    val parser = new RegexParser

    forAll(testData) { (input: String, expected: Seller) =>
      val actual = parser.parse(input)
      actual.headOption shouldBe defined
      actual.head shouldBe expected
    }
  }

  it should "ignore a single malformed or commented string" in {
    val testData = Table(
      "input",
      "google.com, pub-50842344, DERECT",
      "google.com, pub-7439281311086140",
      "google.com, pub-7439281311086140,,,",
      "google.com pub-4893568787652330 DIRECT ",
      "google.com=pub-489356",
      "",
      "      ",
      " ,,,,,,, ",
      "google",
      "# google.com, pub-50842344, DIRECT",
      "##### google.com, pub-50842344, DIRECT",
      "google.com, pub-50842344, # DIRECT",
      "google.com, pub-50842344 #, DIRECT",
      "##### #####",
      "  ##### #####"
    )

    val parser = new RegexParser

    forAll(testData) { input =>
      parser.parse(input) shouldBe empty
    }
  }

  it should "parse multiline content" in {
    val result =
      new RegexParser().parse("""triplelift.com, 876, DIRECT, 6c33edb13117fd86
         | # 
         | # INTERNATIONAL
         |google.com, pub-8566795000208645, DIRECT # banner, video
         |,,,,,
         |bad.com record
         |bad.com, record, DERECT,
         |spotexchange.com, 198856, RESELLER
         |  # spotexchange.com, 9999, RESELLER
         |bad=record
         |openx.com, duplicatecase, RESELLER, 6a698e2ec38604c6
         |districtm.io, 100962, RESELLER
         |openx.com, duplicatecase, RESELLER, 6a698e2ec38604c6
         |
         |""".stripMargin)

    result should contain theSameElementsAs List(
      Seller("triplelift.com", "876", Direct, Some("6c33edb13117fd86")),
      Seller("google.com", "pub-8566795000208645", Direct, None),
      Seller("spotexchange.com", "198856", Reseller, None),
      Seller("openx.com", "duplicatecase", Reseller, Some("6a698e2ec38604c6")),
      Seller("districtm.io", "100962", Reseller, None)
    )
  }
}
