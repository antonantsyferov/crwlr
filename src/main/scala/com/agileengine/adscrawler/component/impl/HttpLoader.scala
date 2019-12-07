package com.agileengine.adscrawler.component.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.agileengine.adscrawler.component.Loader
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Consumes URLs, handles redirection if needed and reads the received content.
  */
class HttpLoader(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext)
    extends Loader
    with LazyLogging {

  private val simpleClient = Http().singleRequest(_: HttpRequest)
  private val redirectingClient = RichHttpClient.httpClientWithRedirect(simpleClient)

  override def load(uri: String): Future[String] =
    redirectingClient(HttpRequest(uri = uri))
      .flatMap(response => {
        Unmarshal(response.entity).to[String]
      })
      .andThen {
        case Success(_) => logger.info(s"Content from '$uri' loaded successfully")
        case Failure(t) => logger.error(s"Failed to load content from '$uri", t)
      }

  /**
    * Redirect support by akka contributors: https://github.com/akka/akka-http/issues/195#issuecomment-255106927
    * The IAB-standard declares non-2xx statuses as unsupported, however, some publishers break that rule.
    */
  object RichHttpClient {
    type HttpClient = HttpRequest => Future[HttpResponse]

    def redirectOrResult(singleRequest: HttpClient)(response: HttpResponse): Future[HttpResponse] =
      response.status match {
        case StatusCodes.Found | StatusCodes.MovedPermanently | StatusCodes.SeeOther ⇒
          val newUri = response.header[Location].get.uri
          singleRequest(HttpRequest(method = HttpMethods.GET, uri = newUri))
        case _ => Future.successful(response)
      }

    def httpClientWithRedirect(client: HttpClient)(implicit ec: ExecutionContext): HttpClient = {
      lazy val redirectingClient: HttpClient =
        req =>
          client(req).flatMap(redirectOrResult(redirectingClient)) // recurse to support multiple redirects

      redirectingClient
    }
  }

}
