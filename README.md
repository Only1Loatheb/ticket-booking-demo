ticket-booking-demo
===================

Additional assumptions
----------------------

- It can use akka.
- It can use a test database config.
- API uses ISO date format.
- Reservation expiration time is equal to the start of the screening.
- Ticket types for all seats have to be selected.
- Each part of the surname is at least 3 characters long.
- Tail of the name string does not have to consist of lowercase characters.
- Seats in the screening room are arranged in a square grid and the number of seats in every row is the same.
- There can be a single place left over at the side of the row.
- Screenings given in point 2 of the scenario are sorted by implicit ordering of pairs (title, screening time).
- Screenings mentioned in use case 2 are available even if there are no seats left.
- Screenings mentioned in use case 2 are represented by the title of the movie and the starting moment.

Based on
--------

- HTTP template

Docs: <https://doc.akka.io/docs/akka-http/current/introduction.html>

Github: <https://github.com/akka/akka-http-quickstart-scala.g8>

- Reactive Database Mapping with Scala and Slick Talk

Youtube: <https://www.youtube.com/watch?v=Ksobupg60Vk>

Github: <https://github.com/rucek/reactive-database-mapping-with-slick>

- Slick resources

Docs: <https://scala-slick.org/doc/3.1.1/gettingstarted.html>

Template: <https://developer.lightbend.com/start/?group=scala&project=hello-slick-3.1>

<!-- Pipe to self pattern:

<https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#send-future-result-to-self> -->

Run
---

Install sbt

<https://www.scala-sbt.org/1.x/docs/Setup.html>

Build and run the app with this command

```bash
sbt run
```

Call services
-------------

The user selects the day and the time when he/she would like to see the movie.

The system lists movies available in the given time interval - title and screening
times.

```bash
curl -H "Content-Type: application/json" -X GET -d "{\"from\":\"2011-10-05T14:48:00.000Z\",\"to\":\"2021-10-05T14:48:00.000Z\"}" http://localhost:8080/movie
```

The user chooses a particular screening.

The system gives information regarding screening room and available seats.

```bash
curl -H "Content-Type: application/json" -X GET -d "{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\"}" http://localhost:8080/screening
```

The user chooses seats, and gives the name of the person doing the reservation
(name and surname).

The system gives back the total amount to pay and reservation expiration time.

```bash
curl -H "Content-Type: application/json" -X POST -d "{\"userName\":\"Tomasz\",\"userSurname\":\"Zieliński\",\"seats\":[{\"rowNo\":1,\"seatNo\":2}],\"screening\":{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\",\"room\":1},\"tickets\":{\"adultTickets\":1,\"studentTickets\":0,\"childTickets\":0}}" http://localhost:8080/reservation
```

Scripts
-------

Scripts are located in [scripts](scripts) directory.
You can execute them with bash.

I advise you to run the app in one terminal and execute tests or use case in another one.
