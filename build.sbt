ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.CodeCrafters"
ThisBuild / organizationName := "CodeCrafters"

assembly / assemblyJarName := "redis.jar"

lazy val root = (project in file("."))
  .settings(
    name := "codecrafter-redis",
    // List your dependencies here
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.12.0",
      "org.typelevel" %% "cats-effect" % "3.5.7"
    ),
    assembly / assemblyJarName := "redis.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    }
  )
