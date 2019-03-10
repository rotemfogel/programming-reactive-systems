lazy val commonSettings = Seq(
  organization := "me.rotemfo",
  scalaVersion := "2.11.8",
  publishMavenStyle := true,
  scalacOptions := Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-target:jvm-1.8",
    "-unchecked",
    "-Xfuture",
    "-Xlint",
    "-Yrangepos",
    "-Ywarn-dead-code",
    "-Ywarn-nullary-unit",
    "-Ywarn-unused",
    "-Ywarn-unused-import"
  ),
  scalacOptions in(Compile, doc) ++= Seq.empty,
  javacOptions in(Compile, doc) ++= Seq.empty,
  scalacOptions in Test ++= Seq("-Yrangepos"),
  version := s"1.0.0",
  resolvers ++= Seq(
    "confluent" at "http://packages.confluent.io/maven/",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  publishArtifact in (Compile, packageDoc) := false,

  autoAPIMappings := true,
  parallelExecution in Test := false,
  parallelExecution in IntegrationTest := true,

  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in packageDoc := false,
  publishArtifact in Test := false
)

lazy val example = project.in(file("example"))
  .settings(commonSettings: _*)

lazy val async = project.in(file("async"))
  .settings(commonSettings: _*)

lazy val actorbintree = project.in(file("actorbintree"))
  .settings(commonSettings: _*)

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(name := "programming-reactive-systems", publishArtifact := false, publish := {}, publishLocal := {})
  .aggregate(example, async, actorbintree)

updateOptions := updateOptions.value.withLatestSnapshots(false)
