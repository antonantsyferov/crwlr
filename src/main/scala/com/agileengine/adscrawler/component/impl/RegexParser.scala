package com.agileengine.adscrawler.component.impl

import java.util.regex.{Matcher, Pattern}

import com.agileengine.adscrawler.domain.{Relationship, Seller}
import com.agileengine.adscrawler.component.Parser
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

/**
  * A thread-safe regex-based implementation. Additionally skips duplicate records and parses relationship in a
  * case-insensitive manner (the behavior is based on real cases found).
  */
class RegexParser extends Parser with LazyLogging {

  // TODO possibly needs more strict domain match based on https://tools.ietf.org/html/rfc1123
  // P.S. I'm a regex lover :)
  private val regex: String =
    "^(?<dom>[^\\s,]+)\\s*,\\s*(?<acc>[^\\s,]+)\\s*,\\s*(?<rel>[^\\s,]+)(?:\\s*,\\s*(?<auth>[^\\s,]+)?)?.*"

  private val threadLocalMatcher: ThreadLocal[Matcher] =
    ThreadLocal.withInitial(() => Pattern.compile(regex).matcher(""))

  override def parse(content: String): Seq[Seller] =
    content.lines
      .flatMap(parseRecord)
      .toSeq
      .distinct

  private def parseRecord(str: String): Option[Seller] = {
    def toRelationship(str: String) =
      Option(str)
        .map(_.toUpperCase)
        .flatMap(strUpper => Try(Relationship.withName(strUpper)).toOption)

    val matcher = threadLocalMatcher.get()
    matcher.reset(str.takeWhile(_ != '#').trim)
    if (matcher.matches()) {
      for {
        domain       <- Option(matcher.group("dom"))
        accountId    <- Option(matcher.group("acc"))
        relationship <- toRelationship(matcher.group("rel"))
      } yield Seller(domain, accountId, relationship, Option(matcher.group("auth")))
    } else {
      None
    }
  }
}
