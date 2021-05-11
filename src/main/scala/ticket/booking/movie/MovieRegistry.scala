package ticket.booking.movie

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.DateTime

import slick.jdbc.H2Profile.backend.DatabaseDef

import ticket.booking.database.{SQLStatements => SQL}

final case class MovieScreening(title: String, start: DateTime)
final case class MovieScreenings(screenings: immutable.Seq[MovieScreening])

/** The user selects the day and the time when he/she would like to see the movie.
  *
  * @param from start of the  time interval
  * @param to end of the time interval
  */
final case class ScreeningsInterval(from: DateTime, to: DateTime)

object MovieRegistry {

  sealed trait Command
  final case class GetMovies(
      interval: ScreeningsInterval,
      replyTo: ActorRef[MovieScreenings]
  ) extends Command

  def apply(db: DatabaseDef): Behavior[Command] = registry(db)

  private def registry(db: DatabaseDef): Behavior[Command] =
    Behaviors.receiveMessage { case GetMovies(interval, replyTo) =>
      val avaliableScreeningsSeq = getAvaliableMovieScreenings(db, interval)
      val avaliableScreenings = avaliableScreeningsSorted(avaliableScreeningsSeq)
      replyTo ! avaliableScreenings
      Behaviors.same
    }

  /** The system lists movies available in the given time interval - title and screening times.
    */
  private def getAvaliableMovieScreenings(
      db: DatabaseDef,
      interval: ScreeningsInterval
  ): Seq[(String, DateTime)] = {
    val query = SQL.getMoviesStartingBetween(interval.from, interval.to)
    Await.result(db.run(query), Duration.Inf)
  }

  private def avaliableScreeningsSorted(
      avaliableScreeningsSeq: Seq[(String, DateTime)]
  ): MovieScreenings = {
    MovieScreenings(avaliableScreeningsSeq.sorted.map(MovieScreening.tupled(_)))
  }
}
