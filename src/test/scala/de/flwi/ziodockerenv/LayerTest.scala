package de.flwi.ziodockerenv

import de.flwi.ziodockerenv.LayerTest.DockerComposeModule.Docker.{DockerComposeService, DockerModule}
import de.flwi.ziodockerenv.LayerTest.DockerComposeModule.Postgres.PostgresDockerModule
import zio._
import zio.console.Console
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}

object LayerTest extends DefaultRunnableSpec {

  object DockerComposeModule {

    // ================= Docker =================
    object Docker {
      type DockerModule = Has[Docker.Service]

      case class DockerComposeService(name: String)
      case class RunningDockerComposeService(name: String, port: Int)

      trait Service {
        def runContainer(containerService: DockerComposeService): ZIO[Any, Nothing, RunningDockerComposeService]
        def stopContainer(running: RunningDockerComposeService): ZIO[Any, Nothing, Unit]

        def log(str: String): ZIO[Any, Nothing, Unit]
      }

      val live: ZLayer[Console, Nothing, DockerModule] = ZLayer.fromFunction(console =>
        new Service {

          override def runContainer(
            dockerComposeService: DockerComposeService): ZIO[Any, Nothing, RunningDockerComposeService] = {
            log(s"starting service ${dockerComposeService.name}") *> ZIO.succeed(
              RunningDockerComposeService(dockerComposeService.name, 5432))
          }

          override def stopContainer(runnning: RunningDockerComposeService): ZIO[Any, Nothing, Unit] = {
            log(s"stopping service $runnning")
          }
          override def log(str: String): ZIO[Any, Nothing, Unit] = zio.console.putStrLn(str).provide(console)
        })
    }

    object Postgres {
      type PostgresDockerModule = Has[Postgres.Service]

      // ================= Postgres =================
      trait Service {
        def runCommandOnPostgres(cmd: String): ZIO[PostgresDockerModule, Nothing, Unit]
      }

      val live: ZLayer[DockerModule, Nothing, PostgresDockerModule] = ZLayer.fromFunctionManaged { docker =>
        Managed
          .make(docker.get.runContainer(DockerComposeService("postgres")))(runningContainer =>
            docker.get.stopContainer(runningContainer))
          .map(runningPostgres =>
            new Service {
              def runCommandOnPostgres(cmd: String): ZIO[PostgresDockerModule, Nothing, Unit] = {
                docker.get.log(s"executing command '$cmd' on running container: $runningPostgres")
              }
            })
      }

      // accessor methods
      def runCommandOnPostgres(cmd: String): ZIO[PostgresDockerModule, Nothing, Unit] =
        ZIO.accessM(_.get.runCommandOnPostgres(cmd))
    }
  }

  val horizontal
    : ZLayer[Console, Nothing, PostgresDockerModule] = LayerTest.DockerComposeModule.Docker.live >>> DockerComposeModule.Postgres.live

  val completeLayer: Layer[Nothing, PostgresDockerModule] = Console.live >>> horizontal

  def program(num: Int): ZIO[PostgresDockerModule, Nothing, Int] = {
    for {
      _ <- DockerComposeModule.Postgres.runCommandOnPostgres(s"select $num as num")
    } yield num
  }

  def runtest(num: Int) = assertM(program(num))(equalTo(num))

  val tests = 1.to(5).map(i => testM(s"test_$i")(runtest(i)))

  override def spec: ZSpec[zio.test.environment.TestEnvironment, Any] =
    suite("test suite")(tests: _*).provideLayerShared(completeLayer)
}
