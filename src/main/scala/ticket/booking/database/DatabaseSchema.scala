package ticket.booking.database

import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.H2Profile.api._

import akka.http.scaladsl.model.DateTime
import java.sql.Timestamp

object TableRow{
  final case class TicketPrice(id: Int, adult: BigDecimal, student: BigDecimal, child: BigDecimal)
  final case class ScreeningRoom(id: Int, seat_collumns_count: Int, seat_rows_count: Int)
  final case class Movie(id: Int, title: String)
  final case class Screening(id: Int, ticket_price_id: Int, movie_id: Int, screening_room_id: Int, screening_start: DateTime)
  final case class Reservation(id: Option[Int], user_name: String, user_surname: String, screening_id: Int)
  final case class SeatReservation(id: Option[Int], reservation_id: Int, seat_row_no: Int, seat_collumn_no: Int)
}

object DatabaseConfig{
  val db = Database.forConfig("h2mem1")
  val profile = slick.jdbc.H2Profile
  val session = SlickSession.forDbAndProfile(db, profile)
}

object DatabaseSchema{
  import TableRow._
  class TicketPrices (tag: Tag) extends Table[TicketPrice](tag, "ticket_price") {
    def ticket_price_id = column[Int]("ticket_price_id",O.PrimaryKey)
    def adult = column[BigDecimal]("adult")
    def student = column[BigDecimal]("student")
    def child = column[BigDecimal]("child")

    def * = ((ticket_price_id, adult, student, child)
      <>(TicketPrice.tupled, TicketPrice.unapply(_)))
  }

  val ticketPrices = TableQuery[TicketPrices]

  class ScreeningRooms (tag: Tag) extends Table[ScreeningRoom](tag, "screening_room") {
    def screening_room_id = column[Int]("screening_room_id", O.PrimaryKey)
    def seat_collumns_count = column[Int]("seat_collumns_count")
    def seat_rows_count = column[Int]("seat_rows_count")
    
    def * = ((screening_room_id, seat_collumns_count, seat_rows_count)
      <>(ScreeningRoom.tupled, ScreeningRoom.unapply(_)))
  }

  val screeningRooms = TableQuery[ScreeningRooms]

  class Movies (tag: Tag) extends Table[Movie](tag, "movie") {
    def movie_id = column[Int]("movie_id", O.PrimaryKey) 
    def title = column[String]("title") 
    def * = ((movie_id, title)
      <>(Movie.tupled, Movie.unapply(_)))
  }

  val movies = TableQuery[Movies]

  implicit val JavaLocalDateTimeMapper = MappedColumnType.base[DateTime, Timestamp](
    // https://doc.akka.io/japi/akka-http/current/akka/http/scaladsl/model/DateTime.html#clicks--
    dateTime => new Timestamp(dateTime.clicks),
    // https://docs.oracle.com/javase/8/docs/api/java/sql/Timestamp.html#getTime--
    timestamp => DateTime(timestamp.getTime())
  )

  class Screenings (tag: Tag) extends Table[Screening](tag, "screening") {
    def screening_id = column[Int]("screening_id", O.PrimaryKey)
    def ticket_price_id = column[Int]("ticket_price_id")
    def movie_id = column[Int]("movie_id")
    def screening_room_id = column[Int]("screening_room_id")
    def screening_start = column[DateTime]("screening_start")

    def ticket_price = foreignKey("fk_ticket_price", ticket_price_id, ticketPrices)(_.ticket_price_id)
    def movie = foreignKey("fk_movie", movie_id, movies)(_.movie_id)
    def screening_room = foreignKey("fk_screening_room", screening_room_id, screeningRooms)(_.screening_room_id)
    def * = ((screening_id, ticket_price_id, movie_id, screening_room_id, screening_start)
      <>(Screening.tupled, Screening.unapply(_)))
  }

  val screenings = TableQuery[Screenings]

  class Reservations (tag: Tag) extends Table[Reservation](tag, "reservation") {
    def reservation_id = column[Int]("reservation_id", O.PrimaryKey, O.AutoInc)
    def user_name = column[String]("user_name")
    def user_surname = column[String]("user_surname")
    def screening_id = column[Int]("screening_id")

    def screening = foreignKey("fk_screening", screening_id, screenings)(_.screening_id)
    def * = ((reservation_id.?, user_name, user_surname, screening_id)
      <>(Reservation.tupled, Reservation.unapply(_)))
  }

  val reservations = TableQuery[Reservations]

  class SeatReservations (tag: Tag) extends Table[SeatReservation](tag, "seat_reservation") {
    def seat_reservation_id = column[Int]("seat_reservation_id", O.PrimaryKey, O.AutoInc)
    def reservation_id = column[Int]("reservation_id")
    def seat_row_no = column[Int]("seat_row_no")
    def seat_collumn_no = column[Int]("seat_collumn_no")

    def reservation = foreignKey("fk_reservation", reservation_id, reservations)(_.reservation_id)
    def * = ((seat_reservation_id.?, reservation_id, seat_row_no, seat_collumn_no)
      <>(SeatReservation.tupled, SeatReservation.unapply(_)))
  }

  val seatReservations = TableQuery[SeatReservations]
}