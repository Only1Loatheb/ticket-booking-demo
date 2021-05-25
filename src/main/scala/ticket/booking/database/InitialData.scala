package ticket.booking.database

import akka.http.scaladsl.model.DateTime
import akka.actor.typed.ActorSystem

import slick.jdbc.H2Profile.api._
import slick.dbio.DBIO
import slick.jdbc.meta.MTable
import slick.jdbc.H2Profile.backend.DatabaseDef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import ticket.booking.database.TableRow._
import ticket.booking.database.DatabaseSchema._

object InitialData {
  def insertInitialData(db: DatabaseDef, system: ActorSystem[_]): Future[Unit] = {
    val setup = DBIO.seq(
      ticketPrices.delete,
      screeningRooms.delete,
      movies.delete,
      screenings.delete,
      reservations.delete,
      seatReservations.delete,

      screeningRooms ++= Seq(
        ScreeningRoom(1, 10, 10),
        ScreeningRoom(2, 20, 20),
        ScreeningRoom(3, 16, 9)
      ),
      movies ++= Seq(
        Movie(1, "Shrek"),
        Movie(2, "Shrek 2"),
        Movie(3, "Kiler-ów 2-óch"),
      ),

      ticketPrices += TicketPrice(1, BigDecimal("25"), BigDecimal("18"), BigDecimal("12.50")),

      screenings ++= Seq(
        Screening(1, 1, 1, 1, DateTime(2021, 6, 1, 18,30,0)),
        Screening(2, 1, 1, 2, DateTime(2021, 6, 3, 18,30,0)),
        Screening(3, 1, 1, 3, DateTime(2021, 6, 1, 18,30,0)),
        Screening(4, 1, 2, 1, DateTime(2021, 6, 2, 18,30,0)),
        Screening(5, 1, 2, 2, DateTime(2021, 6, 2, 18,30,0)),
        Screening(6, 1, 3, 3, DateTime(2021, 6, 3, 18,30,0))
      )
    )

    db.run(setup).andThen {
      case Success(_) => system.log.info("Initial data inserted")
      case Failure(e) => system.log.info(s"Initial data not inserted: ${e.getMessage}")
    }
  }

  def createSchemaIfNotExists(db: DatabaseDef, system: ActorSystem[_]): Future[Unit] = {
    db.run(MTable.getTables).flatMap {
      case tables if tables.isEmpty =>
        val schema = ticketPrices.schema ++ screeningRooms.schema ++ movies.schema ++ screenings.schema ++ reservations.schema ++ seatReservations.schema
        db.run(schema.create).andThen {
          case Success(_) => system.log.info("Schema created")
        }
      case tables if tables.nonEmpty =>
        system.log.info("Schema already exists")
        Future.successful(())
    }
  }
}
