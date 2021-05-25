package ticket.booking.screening

import spray.json.DefaultJsonProtocol

import ticket.booking.json.DateJsonFormat

object ScreeningJsonFormats {

  import DefaultJsonProtocol._

  import ticket.booking.reservation.ReservationJsonFormats.seatJsonFormat
  implicit val dateJsonFormat = DateJsonFormat

  implicit val screeningInfoJsonFormat = jsonFormat2(ScreeningInfo)
  implicit val roomAndSeatsJsonFormat = jsonFormat2(RoomAndSeats)
  implicit val roomsAndAvalibleSeatsJsonFormat = jsonFormat1(RoomsAndAvalibleSeats)
}
