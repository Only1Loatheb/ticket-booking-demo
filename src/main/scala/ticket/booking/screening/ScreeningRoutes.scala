package ticket.booking.screening

import scala.concurrent.Future

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import ticket.booking.screening.{ScreeningRegistry => SR}

class ScreeningRoutes(screeningRegistry: ActorRef[SR.Command])(implicit
    val system: ActorSystem[_]
) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import ScreeningJsonFormats._

  private implicit val requestTimeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def getScreening(screeningInfo: ScreeningInfo): Future[RoomsAndAvalibleSeats] =
    screeningRegistry.ask(SR.GetScreening(screeningInfo, _))

  val screeningRoutes: Route =
    pathPrefix("screening") {
      pathEnd {
        get {
          entity(as[ScreeningInfo]) { screeningInfo =>
            complete(getScreening(screeningInfo))
          }
        }
      }
    }
}
