package ticket.booking.movie

import scala.concurrent.Future

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import ticket.booking.movie.{MovieRegistry => MR}

class MovieRoutes(movieRegistry: ActorRef[MR.Command])(implicit
    val system: ActorSystem[_]
) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import MovieJsonFormats._

  private implicit val requestTimeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def getMovies(interval: ScreeningsInterval): Future[MovieScreenings] =
    movieRegistry.ask(MR.GetMovies(interval, _))

  val movieRoutes: Route =
    pathPrefix("movie") {
      pathEnd {
        get {
          entity(as[ScreeningsInterval]) { screeningsInterval =>
            complete(getMovies(screeningsInterval))
          }
        }
      }
    }
}
