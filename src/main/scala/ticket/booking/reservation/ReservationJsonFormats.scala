package ticket.booking.reservation

import spray.json.DefaultJsonProtocol

import ticket.booking.json.DateJsonFormat
import ticket.booking.reservation.ReservationRegistry._

object ReservationJsonFormats {

  import DefaultJsonProtocol._
  
  implicit val dateJsonFormat = DateJsonFormat
    
  implicit val screeningJsonFormat = jsonFormat3(Screening)
  implicit val ticketTypesJsonFormat = jsonFormat3(TicketTypes)
  implicit val seatJsonFormat = jsonFormat2(Seat)
  implicit val reservationJsonFormat = jsonFormat5(Reservation)
  implicit val reservationDetailsForUserJsonFormat = jsonFormat2(ReservationDetailsForUser)
  implicit val errorReasonJsonFormat = jsonFormat1(ErrorReason)
  implicit val postReservationResponseJsonFormat = jsonFormat1(PostReservationResponse)
}
