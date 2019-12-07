package com.agileengine.adscrawler

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import com.agileengine.adscrawler.component.AdsCrawler
import com.agileengine.adscrawler.component.impl.{H2MemRepository, HttpLoader, RegexParser}
import com.agileengine.adscrawler.domain.Publisher
import com.agileengine.adscrawler.server.Server
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Entry point.
  */
object Application extends App with LazyLogging with SprayJsonSupport {
  implicit val system: ActorSystem = ActorSystem("AdsCrawlerService")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  /**
    * Default timeout to wait after graceful shutdown.
    */
  val ShutdownTimeout = 10.seconds

  /**
    * Predefined list of supported publishers.
    */
  val DefaultPublishers = List(
    "http://www.cnn.com/ads.txt",
    "http://www.gizmodo.com/ads.txt",
    "http://www.nytimes.com/ads.txt",
    "https://www.bloomberg.com/ads.txt",
    "https://wordpress.com/ads.txt"
  ).flatMap(parseUrl)

  val Port = Option(System.getProperty("PORT"))
    .flatMap(v => Try(Integer.parseInt(v)).toOption)
    .getOrElse(8080)
  val CustomPublishers = Option(System.getProperty("PUBLISHERS"))
    .map(str => str.split(",").toList)
    .map(urls => urls.flatMap(parseUrl))
    .getOrElse(Nil)

  // DI Wiring
  val crawler =
    new AdsCrawler(
      new RegexParser(),
      new HttpLoader(),
      new H2MemRepository(),
      DefaultPublishers ++ CustomPublishers
    )

  val server = new Server(crawler, port = Port)
  server.start().onComplete {
    case Success(_) =>
      logger.info(s"Server is ready and listening on http://${server.host}:${server.port}...")
    case Failure(t) => logger.error("Failed to start", t)
  }

  import ResilientFutureHolder._

  Runtime.getRuntime.addShutdownHook(new Thread(() => {
    Await.ready(for {
      _ <- server.shutdown().warn("Unable to shutdown the server")
      _ <- crawler.close().warn("Unable to close core component")
      _ <- system.terminate().warn("Unable to terminate ActorSystem")
      _ = logger.info("Terminate operations completed")
    } yield (), ShutdownTimeout)
  }))

  private def parseUrl(urlString: String): Option[Publisher] = {
    def asFailure[U](message: String): PartialFunction[Throwable, Try[U]] = {
      case NonFatal(e) => Failure(e)
    }
    Try(new URL(urlString))
      .recoverWith(asFailure(s"Unable to parse input URL '$urlString'"))
      .toOption
      .map(url => Publisher(url.getHost, urlString))
  }
}

object ResilientFutureHolder extends LazyLogging {
  implicit class ResilientFuture[T](val future: Future[T]) extends AnyVal {
    def warn(message: String)(implicit ec: ExecutionContext): Future[_] =
      future
        .recover {
          case NonFatal(e) => logger.info(message, e)
        }
  }
}
