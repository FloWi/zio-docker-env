scalaVersion              := "2.13.1"
name                      := "starterkit"
organization              := "com.organization"
scalafmtOnCompile         := true
fork in Test              := true
parallelExecution in Test := true

lazy val Versions = new {
  val kindProjector = "0.11.0"
  val scalamacros = "2.1.1"
  val http4s = "0.21.0-M5"
  val zio = "1.0.0-RC18-2"
  val zioInteropCats = "2.0.0.0-RC7"
  val circe = "0.12.3"
  val scalaTest = "3.0.8"
  val randomDataGenerator = "2.8"
  val ciris = "0.13.0-RC1"
  val logback = "1.2.3"
  val h2database = "1.4.200"
  val quill = "3.4.10"
  val tapir = "0.11.9"
}
addCompilerPlugin("org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full)

// Scala libraries
libraryDependencies ++= Seq(
  "dev.zio" %% "zio"                                     % Versions.zio,
  "dev.zio"                    %% "zio-test"               % Versions.zio % "test",
  "dev.zio"                    %% "zio-test-sbt"           % Versions.zio % "test"

)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")


// Java libraries
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % Versions.logback
)
