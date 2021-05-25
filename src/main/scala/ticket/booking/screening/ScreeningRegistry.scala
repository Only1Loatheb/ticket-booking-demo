package ticket.booking.screening

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.DateTime

import slick.jdbc.H2Profile.backend.DatabaseDef

import ticket.booking.database.TableRow
import ticket.booking.database.{SQLStatements => SQL}
import ticket.booking.reservation.{Seat}

final case class RoomAndSeats(room: Int, seats: immutable.Seq[Seat])
final case class RoomsAndAvalibleSeats(
    screeningInfo: immutable.Seq[RoomAndSeats]
)

final case class ScreeningInfo(title: String, start: DateTime)

object ScreeningRegistry {
  sealed trait Command
  final case class GetScreening(
      screening: ScreeningInfo,
      replyTo: ActorRef[RoomsAndAvalibleSeats]
  ) extends Command

  def apply(db: DatabaseDef): Behavior[Command] = registry(db)

  private def registry(db: DatabaseDef): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetScreening(screening, replyTo) =>
        val screeningDetails = getScreeningDetails(db, screening)
        replyTo ! screeningDetails
        Behaviors.same
    }

  /** These would turn into nested collections when executing the query, which is not supported at the moment.
    * https://scala-slick.org/doc/3.3.3/queries.html#aggregation
    * val matchingScreenings = screenings .filter(_.screening_start === screening.start)
    * val reservationsByScreening = (reservations join matchingScreenings on (_.screening_id === _.screening_id))
    *  .groupBy({case (r,s)=> s.screening_id})
    */
  private def getScreeningDetails(
      db: DatabaseDef,
      screeningInfo: ScreeningInfo
  ): RoomsAndAvalibleSeats = {
    val query = SQL.getScreeningsByStartAndMovieTitle(screeningInfo.start,screeningInfo.title)
    val matchingScreenings = Await.result(db.run(query), Duration.Inf)
    val roomAndAvalibleSeatsSeq = matchingScreenings.map(getRoomAndAvalibleSeats(db, _))
    RoomsAndAvalibleSeats(roomAndAvalibleSeatsSeq)
  }

  private def getRoomAndAvalibleSeats(
      db: DatabaseDef,
      screening: TableRow.Screening
  ): RoomAndSeats = {
    val room = getScreeningRoom(db, screening)
    val reservedSeats = getReservedSeats(db, screening)
    val avalibleSeats = getAvalibleSeats(room, reservedSeats)
    RoomAndSeats(room.id, avalibleSeats)
  }

  private def getScreeningRoom(
      db: DatabaseDef,
      screening: TableRow.Screening
  ): TableRow.ScreeningRoom = {
    val query = SQL.getScreeningRoomFromScreening(screening)
    Await.result(db.run(query), Duration.Inf).head
  }

  private def getReservedSeats(
      db: DatabaseDef,
      screening: TableRow.Screening
  ): Seq[TableRow.SeatReservation] = {
    val query = SQL.getSeatReservationsFromScreening(screening)
    Await.result(db.run(query), Duration.Inf)
  }

  private def getAvalibleSeats(
      room: TableRow.ScreeningRoom,
      reservedSeats: Seq[TableRow.SeatReservation]
  ): Seq[Seat] = {
    val reserved = reservedSeats.map(s => (s.seat_row_no, s.seat_collumn_no)).toSet
    for {
      r <- 1 to room.seat_rows_count
      c <- 1 to room.seat_collumns_count if !reserved.contains((r,c))
    }yield(Seat(r, c))
  }
}
