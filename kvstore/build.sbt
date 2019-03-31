name := course.value ++ "-" ++ assignment.value

scalaVersion := "2.11.12"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Xexperimental"
)

scalacOptions in Test += "-Ywarn-value-discard:false" // since this often appears in expectNext(expected) testing style in streams

val akkaVersion = "2.5.16"
val akkaHttpVersion = "10.1.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-stream"              % akkaVersion,
  "com.typesafe.akka"        %% "akka-stream-testkit"      % akkaVersion % Test,
  "com.typesafe.akka"        %% "akka-stream-typed"        % akkaVersion,

  // to be used slightly in followers example
  "com.typesafe.akka"        %% "akka-actor-typed"         % akkaVersion,

  // Used by protocols assignment
  "org.fusesource.leveldbjni" % "leveldbjni-all"           % "1.8",
  "com.github.romix.akka"    %% "akka-kryo-serialization"  % "0.5.0",
  
  // Test resources
  "org.slf4j"                 % "slf4j-api"                % "1.7.25"    % Test,
  "ch.qos.logback"            % "logback-classic"          % "1.2.3"     % Test,
  "com.typesafe.akka"        %% "akka-slf4j"               % akkaVersion % Test,
  "com.typesafe.akka"        %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalacheck"           %% "scalacheck"               % "1.13.5"    % Test,
  "junit"                    % "junit"                     % "4.10"      % Test
)

courseId := "changeme"

parallelExecution in Test := false
