package com.agileengine.adscrawler.component

import scala.concurrent.Future

/**
  * Loads content using the given URI.
  */
trait Loader {

  /**
    * Loads content using the given URI.
    */
  def load(uri: String): Future[String]
}
