name := "ticket-booking-demo"

scalaVersion := "2.13.4"
organization := "ticket.booking"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-Xlint",
)

lazy val akkaHttpVersion = "10.2.4"
lazy val akkaVersion    = "2.6.14"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-http"                 % akkaHttpVersion,
  "com.typesafe.akka"  %% "akka-http-spray-json"      % akkaHttpVersion,
  "com.typesafe.akka"  %% "akka-actor-typed"          % akkaVersion,
  "com.typesafe.akka"  %% "akka-stream"               % akkaVersion,
  "ch.qos.logback"     % "logback-classic"            % "1.2.3",
  
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "3.0.0",
  "com.h2database" % "h2" % "1.4.187",

  "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
  "org.scalatest"     %% "scalatest"                % "3.1.4"         % Test
)