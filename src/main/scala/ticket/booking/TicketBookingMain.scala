package ticket.booking.main

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{SpawnProtocol, ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.concat

import scala.util.{Failure, Success}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import ticket.booking.screening.{ScreeningRegistry, ScreeningRoutes}
import ticket.booking.reservation.{ReservationRegistry, ReservationRoutes}
import ticket.booking.movie.{MovieRegistry, MovieRoutes}
import ticket.booking.database.{DatabaseConfig, InitialData}

object TicketBookingMain {

  val db = DatabaseConfig.db
  val session = DatabaseConfig.session

  private def startHttpServer(
      routes: Route
  )(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 3.seconds))
    bindingFuture.onComplete {
      case Success(binding) => {
        val address = binding.localAddress
        system.log.info(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      }
      case Failure(ex) => {
        system.log.error("Failed to bind HTTP endpoint, terminating system.", ex)
        system.terminate()
      }
    }
  }

  def main(args: Array[String]): Unit = {

    val rootBehavior = Behaviors.setup[SpawnProtocol.Command] { context =>

      val system = context.system

      val databaseFuture = InitialData
        .createSchemaIfNotExists(db, system)
        .flatMap(_ => InitialData.insertInitialData(db, system))
      Await.ready(databaseFuture, Duration.Inf)

      val screeningRegistryActor = context.spawn(ScreeningRegistry(db), "ScreeningRegistry")
      context.watch(screeningRegistryActor)

      val reservationRegistryActor = context.spawn(ReservationRegistry(db), "ReservationRegistry")
      context.watch(reservationRegistryActor)

      val movieRegistryActor = context.spawn(MovieRegistry(db), "MovieRegistry")
      context.watch(movieRegistryActor)

      val screeningRoutes = new ScreeningRoutes(screeningRegistryActor)(system)
      val reservationRoutes = new ReservationRoutes(reservationRegistryActor)(system)
      val movieRoutes = new MovieRoutes(movieRegistryActor)(system)

      val routes = concat(
        screeningRoutes.screeningRoutes,
        reservationRoutes.reservationRoutes,
        movieRoutes.movieRoutes
      )

      startHttpServer(routes)(system)

      Behaviors.receiveSignal { case (_, Terminated(_)) =>
        session.close()
        Behaviors.stopped
      }
    }

    val system = ActorSystem(rootBehavior, "TicketBookingDemo")
  }
}
