import Dependencies._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.ocr"
ThisBuild / organizationName := "ocr"
val scalatestVersion = "3.2.11"

val scalatest        = "org.scalatest" %% "scalatest" % scalatestVersion % Test

lazy val root = (project in file("."))
  .settings(
    name := "pdfocr",
    libraryDependencies ++= Seq(
      // https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
     "org.apache.pdfbox" % "pdfbox" % "2.0.26",
      "org.apache.pdfbox" % "jbig2-imageio" % "3.0.4",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      scalatest
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
