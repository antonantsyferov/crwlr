package com.agileengine.adscrawler.component

import com.agileengine.adscrawler.domain.{Publisher, PublisherData, Seller}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * The core component responsible for ads.txt loading, its persistence and querying workflow.
  * @param publishers sequence of publishers to be crawled, persisted and then queried.
  */
class AdsCrawler(
  val parser: Parser,
  val loader: Loader,
  val repository: Repository,
  val publishers: Seq[Publisher]
)(implicit ec: ExecutionContext)
    extends LazyLogging {

  /**
    * Initializes the component: crawls ads.txt data for all given publishers,
    * saves it into repository for future querying.
    */
  def init(): Future[Unit] = {
    def crawl(p: Publisher): Future[Option[PublisherData]] = {
      logger.info(s"Processing publisher '${p.name}'")
      loader
        .load(p.url)
        .map(parser.parse)
        .map(sellers => Some(PublisherData(p, sellers)))
        .recover {
          case NonFatal(e) =>
            logger.error(s"Publisher '${p.name}' was not loaded", e)
            None
        }
    }

    for {
      publisherData <- Future.sequence(publishers.map(crawl))
      _             = logger.info("All publishers are crawled successfully")
      _             <- repository.persist(publisherData.flatten)
      _             = logger.info("Dataset persisted")
    } yield ()
  }

  def getSellers(publisherName: String): Future[Seq[Seller]] =
    repository.getSellers(publisherName)

  def close(): Future[Unit] =
    repository.close()
}
