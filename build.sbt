Compile / run / fork := true

lazy val app =
  project
    .in(file("app"))
    .settings(
      scalacOptions ++= Seq("-Wunused:all"),
      Compile / mainClass := Some("io.vangogiel.example.finance.Main"),
      libraryDependencies ++= Seq(
        "co.fs2"          %% "fs2-core"          % "3.12.2",
        "org.typelevel"   %% "cats-core"         % "2.8.0",
        "io.grpc"          % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
        "io.grpc"          % "grpc-services"     % scalapb.compiler.Version.grpcJavaVersion,
        "org.scalatest"   %% "scalatest"         % "3.2.20" % Test,
      ),
      Test / testOptions += Tests.Argument(TestFramework("org.scalatest.tools.Framework", "org.scalatest.tools.ScalaTestFramework")),
    )
    .enablePlugins(Fs2Grpc)

addCommandAlias("devBuild", "clean; scalafmtAll; scalafmtSbt; scalafixAll; compile; Test/scalafixAll; test")
addCommandAlias("appRun", "app/run")

inThisBuild(
  List(
    scalaVersion      := "3.3.7",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)
