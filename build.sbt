ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.CodeCrafters"
ThisBuild / organizationName := "CodeCrafters"

assembly / assemblyJarName := "redis.jar"

val Versions = new {
  val cats = "2.12.0"
  val catsEffect = "3.5.7"
  val slf4j = "2.0.16"
}

lazy val root = (project in file("."))
  .settings(
    name := "codecrafter-redis",
    // List your dependencies here
    libraryDependencies ++= Seq(
      // cats

      "org.typelevel" %% "cats-core" % Versions.cats,
      "org.typelevel" %% "cats-effect" % Versions.catsEffect,

      // logging
      "org.slf4j" % "slf4j-api" % Versions.slf4j,
      "org.slf4j" % "slf4j-simple" % Versions.slf4j
    ),
    assembly / assemblyJarName := "redis.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        xs map {
          _.toLowerCase
        } match {
          case "services" :: xs =>
            MergeStrategy.filterDistinctLines
          case _ => MergeStrategy.discard
        }
      case PathList("module-info.class") =>
        MergeStrategy.last
      case path if path.endsWith("/module-info.class") =>
        MergeStrategy.last
      case _ =>
        MergeStrategy.first
    }
  )
