
name := "klines"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.3.0",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.0",
    "com.typesafe.akka" %% "akka-http" % "10.1.8",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.8" % Test,
    "com.typesafe.akka" %% "akka-actor" % "2.5.21",
    "com.typesafe.akka" %% "akka-testkit" % "2.5.21" % Test,
    "com.typesafe.akka" %% "akka-stream" % "2.5.21",
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.21" % Test,
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8"
)