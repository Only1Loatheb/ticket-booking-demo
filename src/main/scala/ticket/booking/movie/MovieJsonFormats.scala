package ticket.booking.movie

import spray.json.DefaultJsonProtocol

import ticket.booking.json.DateJsonFormat

object MovieJsonFormats {

  import DefaultJsonProtocol._

  implicit val dateJsonFormat = DateJsonFormat

  implicit val movieScreeningJsonFormat = jsonFormat2(MovieScreening)
  implicit val movieScreeningsJsonFormat = jsonFormat1(MovieScreenings)
  implicit val screeningsIntervalJsonFormat = jsonFormat2(ScreeningsInterval)
}