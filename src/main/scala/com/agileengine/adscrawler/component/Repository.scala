package com.agileengine.adscrawler.component

import com.agileengine.adscrawler.domain.{PublisherData, Seller}

import scala.concurrent.Future

/**
  * A simple repository definition, which can persist PublisherData and then fetch it asynchronously.
  */
trait Repository {

  /**
    * Persists the given data.
    */
  def persist(publisherData: Seq[PublisherData]): Future[Unit]

  /**
    * Gets all sellers associated with the given publisher name.
    */
  def getSellers(publisherName: String): Future[Seq[Seller]]

  /**
    * Gracefully terminates the work with the repository.
    */
  def close(): Future[Unit]
}
