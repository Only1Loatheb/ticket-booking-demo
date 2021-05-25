package ticket.booking.reservation

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.immutable

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.DateTime

import slick.jdbc.H2Profile.backend.DatabaseDef

import ticket.booking.database.TableRow
import ticket.booking.database.{SQLStatements => SQL}

final case class Screening(title: String, start: DateTime, room: Int)
final case class TicketTypes(
    adultTickets: Int,
    studentTickets: Int,
    childTickets: Int
) {
  def price(ticketPrice: TableRow.TicketPrice): BigDecimal =
    adultTickets * ticketPrice.adult + studentTickets * ticketPrice.student + childTickets * ticketPrice.child

  def ticketCount: Int = adultTickets + studentTickets + childTickets
}

final case class Seat(rowNo: Int, seatNo: Int)

/** The user chooses a particular screening.
  * The user chooses seats, and gives the name of the person doing the reservation
  * (name and surname).
  */
final case class Reservation(
    userName: String,
    userSurname: String,
    seats: immutable.Seq[Seat],
    screening: Screening,
    tickets: TicketTypes
)
final case class Reservations(reservations: immutable.Seq[Reservation])

object ReservationRegistry {
  sealed trait Command

  final case class PostReservation(
      reservation: Reservation,
      replyTo: ActorRef[PostReservationResponse]
  ) extends Command

  /** The system gives back the total amount to pay and reservation expiration time.
    *
    * @param amountToPay total amount to pay
    * @param expiration reservation expiration time
    */
  final case class ReservationDetailsForUser(
      amountToPay: BigDecimal,
      expiration: DateTime
  )

  final case class ErrorReason(val reason: String) extends AnyVal

  final case class PostReservationResponse(
      details: Either[ErrorReason, ReservationDetailsForUser]
  )

  final case class ReservationInfo(screening: TableRow.Screening)

  def apply(db: DatabaseDef): Behavior[Command] = registry(db)

  private def registry(db: DatabaseDef): Behavior[Command] =
    Behaviors.receiveMessage { case PostReservation(reservation, replyTo) =>
      val errorReasonOrReservationInfo = isReservationValid(db, reservation)
      val response =
        PostReservationResponse(errorReasonOrReservationInfo.map({ info =>
          insertReservation(db, reservation, info)
          getDetailsForUser(db, reservation, info)
        }))
      replyTo ! response
      Behaviors.same
    }

  /** Returns Some if seats can be booked
    * Seats can be booked at latest 15 minutes before the screening begins
    * Ticket type for all seats have to be selected
    * There cannot be a single place left over in a row between two already reserved places.
    * Reservation applies to at least one seat.
    * Name and surname should each be at least three characters long, starting
    * with a capital letter. The surname could consist of two parts separated with a
    * single dash, in this case the second part should also start with a capital letter
    */
  private def isReservationValid(
      db: DatabaseDef,
      reservation: Reservation
  ): Either[ErrorReason, ReservationInfo] = {
    val Reservation(userName, userSurname, seats, screening, tickets) =
      reservation
    lazy val screeningOption = doesScreeningExist(db, screening)
    lazy val requestedSeats = seats.toSet
    for {
      _ <- isUserNameValid(userName)
      _ <- isUserSurnameValid(userSurname)
      _ <- isSeatCountValid(requestedSeats, tickets)
      _ <- isBookedOnTime(screening.start)
      dbScreening <- screeningOption.toRight(ErrorReason("Screening does not exist."))
      _ <- isSeatSelectionValid(db, dbScreening, requestedSeats)
    }yield(ReservationInfo(dbScreening))
  }

  private def doesScreeningExist(
      db: DatabaseDef,
      screening: Screening
  ): Option[TableRow.Screening] = {
    val query = SQL.getScreeningsByStartAndRoom(screening.start,screening.room)
    val screenings = Await.result(db.run(query), Duration.Inf)
    screenings.headOption
  }

  private val minNameLength = 3

  private def isNameValid(userName: String): Boolean = {
    (userName.length >= minNameLength
    && userName.forall(_.isLetter)
    && userName.head.isUpper)
  }

  private def isUserNameValid(userName: String): Either[ErrorReason,Unit] = {
    if (isNameValid(userName)) Right(())
    else Left(ErrorReason("Invalid user name."))
  }

  private def isUserSurnameValid(userSurname: String): Either[ErrorReason,Unit]  = {
    val surnameParts = userSurname.split("-")
    if ((surnameParts.length == 1 || surnameParts.length == 2)
      && surnameParts.forall(isNameValid(_))) Right(())
    else Left(ErrorReason("Invalid user surname."))
  }

  private def isSeatCountValid(
      requestedSeats:  Set[Seat],
      tickets: TicketTypes
  ): Either[ErrorReason,Unit] = {
    if(requestedSeats.nonEmpty 
      && requestedSeats.knownSize == tickets.ticketCount) Right(())
    else Left(ErrorReason("Invalid seat count."))
  }

  private val bookInAdvanceTimeDeltaMinutes = 15.minutes
  private val bookInAdvanceTimeDeltaMillis = bookInAdvanceTimeDeltaMinutes.toMillis

  private def isBookedOnTime(screeningStart: DateTime): Either[ErrorReason,Unit] = {
    val canBookScreeningFromDate = DateTime.now + bookInAdvanceTimeDeltaMillis
    if (screeningStart >= canBookScreeningFromDate) Right(())
    else Left(ErrorReason(s"Seats can be booked at latest ${bookInAdvanceTimeDeltaMinutes} before the screening begins."))
  }

  private def isSeatSelectionValid(
      db: DatabaseDef,
      screening: TableRow.Screening,
      requestedSeats: Set[Seat]
  ): Either[ErrorReason,Unit] = {
    val reservedSeats = getReservedSeats(db, screening)
    val newReservedSeats = getNewReservedSeats(requestedSeats, reservedSeats)
    val room = getScreeningRoom(db, screening)
    if (areRequestedSeatsInScreeningRoom(requestedSeats, room)
      && newReservedSeats.knownSize == reservedSeats.size + requestedSeats.size
      && !isSinglePlaceLeftOver(newReservedSeats.toSeq)) Right(())
    else Left(ErrorReason("Invalid seat selection."))
  }

  private def getReservedSeats(
      db: DatabaseDef,
      screening: TableRow.Screening
  ): Seq[TableRow.SeatReservation] = {
    val query = SQL.getSeatReservationsFromScreening(screening)
    Await.result(db.run(query), Duration.Inf)
  }

  private def getNewReservedSeats(
      requestedSeats: Set[Seat],
      reservedSeats: Seq[TableRow.SeatReservation]
  ): Set[Seat] = {
    requestedSeats ++ reservedSeats.map(s =>
      Seat(s.seat_row_no, s.seat_collumn_no)
    )
  } 

  private def getScreeningRoom(
      db: DatabaseDef,
      screening: TableRow.Screening
  ): TableRow.ScreeningRoom = {
    val query = SQL.getScreeningRoomFromScreening(screening)
    Await.result(db.run(query), Duration.Inf).head
  }

  private def areRequestedSeatsInScreeningRoom(
      requestedSeats: Set[Seat],
      room: TableRow.ScreeningRoom
  ): Boolean = {
    requestedSeats.forall(seat =>
      (seat.rowNo >= 1
        && seat.rowNo <= room.seat_rows_count
        && seat.seatNo >= 1
        && seat.seatNo <= room.seat_collumns_count)
    )
  }

  /** There cannot be a single place left over in a row between two already reserved
    * places.
    */
  private def isSinglePlaceLeftOver(seats: Seq[Seat]): Boolean = {
    val rows = seats.groupMap(_.rowNo)(_.seatNo)
    rows.values.exists(isSinglePlaceLeftOverInRow(_))
  }

  private def isSinglePlaceLeftOverInRow(reserved: Seq[Int]): Boolean = {
    reserved.sorted
      .sliding(2, 1)
      .exists(block => block.head == (block.last - 2))
  }

  private def insertReservation(
      db: DatabaseDef,
      reservation: Reservation,
      info: ReservationInfo
  ): Unit = {
    val Reservation(userName, userSurname, seats, _, _) = reservation
    val newReservation = TableRow.Reservation(None, userName, userSurname, info.screening.id)
    val reservationInsert = SQL.insertReservation(newReservation)
    val reservationId = Await.result(db.run(reservationInsert), Duration.Inf)

    val newSeats = seats.map(seat =>
      TableRow.SeatReservation(None, reservationId, seat.rowNo, seat.seatNo)
    )
    val seatReservationsInsert = SQL.insertSeatReservations(newSeats)
    Await.result(db.run(seatReservationsInsert), Duration.Inf)
  }

  private def getDetailsForUser(
      db: DatabaseDef,
      reservation: Reservation,
      info: ReservationInfo
  ): ReservationDetailsForUser = {
    val screening = info.screening
    val ticketPriceQuery = SQL.getTicketPriceFromScreening(screening)
    val ticketPrice = Await.result(db.run(ticketPriceQuery), Duration.Inf).head
    ReservationDetailsForUser(
      reservation.tickets.price(ticketPrice),
      screening.screening_start
    )
  }
}
