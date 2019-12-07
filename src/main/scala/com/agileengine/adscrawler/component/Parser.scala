package com.agileengine.adscrawler.component

import com.agileengine.adscrawler.domain.Seller

/**
  * A parser definition, which can process ads.txt text content and build a list of Sellers from it.
  */
trait Parser {

  /**
    * Parses the given content into sequence of sellers. The malformed or incomplete entries are ignored.
    */
  def parse(content: String): Seq[Seller]
}
