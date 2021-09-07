scalaVersion := "2.12.12"

scalacOptions := Seq("-deprecation", "-Xsource:2.11")
// latest Chisel 2 version
// libraryDependencies += "edu.berkeley.cs" %% "chisel" % "2.2.38"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.1"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.3.1"
// libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "latest.release"
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"


// Rocket here
