package ticket.booking.reservation

import scala.concurrent.Future

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import ticket.booking.reservation.{ReservationRegistry => RR}

class ReservationRoutes(reservationRegistry: ActorRef[RR.Command])(implicit
    val system: ActorSystem[_]
) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import ReservationJsonFormats._

  private implicit val requestTimeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def postReservation(reservation: Reservation): Future[RR.PostReservationResponse] =
    reservationRegistry.ask(RR.PostReservation(reservation, _))

  val reservationRoutes: Route =
    pathPrefix("reservation") {
      pathEnd {
        post {
          entity(as[Reservation]) { reservation =>
            onSuccess(postReservation(reservation)) {
              case RR.PostReservationResponse(Right(performed)) => complete((StatusCodes.Created, performed))
              case RR.PostReservationResponse(Left(reason))     => complete((StatusCodes.BadRequest, reason))
            }
          }
        }
      }
    }
}
