package com.agileengine.adscrawler.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.ActorMaterializer
import com.agileengine.adscrawler.component.AdsCrawler
import com.agileengine.adscrawler.server.JsonProtocol._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * A simple AkkaHTTP-based server with a single route.
  */
class Server(adsCrawler: AdsCrawler, val host: String = "localhost", val port: Int = 8080)(
  implicit val system: ActorSystem,
  val mat: ActorMaterializer,
  val ec: ExecutionContext
) extends LazyLogging
    with SprayJsonSupport {

  private val BindTimeout = 10.seconds
  private val HardDeadlineTimeout = 5.seconds

  private val route: Route =
    pathPrefix("publishers") {
      pathEndOrSingleSlash {
        get {
          complete(adsCrawler.publishers)
        }
      } ~
        pathPrefix(Segment) { publisherName =>
          pathEndOrSingleSlash {
            onSuccess(adsCrawler.getSellers(publisherName)) {
              case Nil     => complete(StatusCodes.NotFound)
              case sellers => complete(sellers)
            }
          }
        }
    }

  private lazy val binding: Future[ServerBinding] =
    Http().bindAndHandle(route, host, port, new HttpConnectionContext())

  def start(): Future[Unit] =
    for {
      _ <- adsCrawler.init()
      _ <- binding
    } yield ()

  def shutdown(): Future[Http.HttpTerminated] =
    Await
      .result(binding, BindTimeout)
      .terminate(hardDeadline = HardDeadlineTimeout)
}
