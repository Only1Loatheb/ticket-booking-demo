package ticket.booking.database

import slick.jdbc.H2Profile.api._
import slick.sql.{FixedSqlStreamingAction => SqlSA, FixedSqlAction => SqlA}
import slick.dbio.Effect

import akka.http.scaladsl.model.DateTime

import ticket.booking.database.TableRow._
import ticket.booking.database.DatabaseSchema._

object SQLStatements {

  def getMoviesStartingBetween(
      timeFrom: DateTime,
      timeTo: DateTime
  ): SqlSA[Seq[(String, DateTime)], (String, DateTime), Effect.Read] = {
    val query = for {
      screening <- screenings.filter(s =>
        s.screening_start > timeFrom && s.screening_start < timeTo
      )
      movie <- screening.movie
    } yield (movie.title, screening.screening_start)
    query.result
  }

  def getScreeningsByStartAndRoom(
      screeningStart: DateTime,
      screeningRoomId: Int
  ): SqlSA[Seq[Screening], Screening, Effect.Read] = {
    screenings
      .filter(s =>
        s.screening_start === screeningStart
          && s.screening_room_id === screeningRoomId
      )
      .result
  }

  def getScreeningsByStartAndMovieTitle(
      screeningStart: DateTime,
      movieTitle: String
  ): SqlSA[Seq[Screening], Screening, Effect.Read] = {
    val query = for {
      s <- screenings.filter(_.screening_start === screeningStart)
      if s.movie.filter(_.title === movieTitle).exists
    } yield (s)
    query.result
  }

  def getSeatReservationsFromScreening(
      screening: Screening
  ): SqlSA[Seq[SeatReservation], SeatReservation, Effect.Read] = {
    val query = for {
      reservation <- reservations.filter(_.screening_id === screening.id)
      seatReservation <- seatReservations.filter(
        _.reservation_id === reservation.reservation_id
      )
    } yield (seatReservation)

    query.result
  }

  def getScreeningRoomFromScreening(
      screening: Screening
  ): SqlSA[Seq[ScreeningRoom], ScreeningRoom, Effect.Read] = {
    screeningRooms
      .filter(_.screening_room_id === screening.screening_room_id)
      .take(1)
      .result
  }

  def getTicketPriceFromScreening(
      screening: Screening
  ): SqlSA[Seq[TicketPrice], TicketPrice, Effect.Read] = {
    ticketPrices
      .filter(_.ticket_price_id === screening.ticket_price_id)
      .take(1)
      .result
  }

  def insertReservation(
      newReservation: Reservation
  ): SqlA[Int, NoStream, Effect.Write] = {
    val reservationInsert = (reservations returning reservations.map(
      _.reservation_id
    )) += newReservation
    reservationInsert
  }

  def insertSeatReservations(
      newSeats: Seq[SeatReservation]
  ): SqlA[Option[Int], NoStream, Effect.Write] = {
    seatReservations ++= newSeats
  }
}
