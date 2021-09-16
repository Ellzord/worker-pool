scalaVersion := "2.13.5"

organization := "com.pirum"
organizationName := "Pirum Systems"
organizationHomepage := Some(url("https://www.pirum.com"))

libraryDependencies += "io.monix" %% "monix" % "3.4.0"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.3.0"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.9"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % "test"