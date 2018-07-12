package sample.cluster.factorial

import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.cluster.Cluster
import akka.routing.FromConfig
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpResponse
import spray.json.DefaultJsonProtocol
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._

case object GetInfo
case class Info(completed: Long, n: Int, factorial: BigInt)

class FactorialFrontend(upToN: Int, repeat: Boolean) extends Actor with ActorLogging {

  val backend = context.actorOf(FromConfig.props(),
    name = "factorialBackendRouter")

  var completed = 0L

  implicit val executionContext = context.dispatcher

  override def preStart(): Unit = {
    sendJobs()
    if (repeat) {
      context.setReceiveTimeout(10.seconds)
    }
  }

  def receive = {
    case (n: Int, factorial: BigInt) =>
      if (n == upToN) {
        log.info("{}! = {} sender: {}", n, factorial, sender().path)
        completed  += 1
        if (repeat)
          context.system.scheduler.scheduleOnce(500.millis) {
            sendJobs()
          }
        else context.stop(self)
      }
    case GetInfo => sender() ! completed
    case ReceiveTimeout =>
      log.info("Timeout")
      sendJobs()
  }

  def sendJobs(): Unit = {
    log.info("Starting batch of factorials up to [{}]", upToN)
    1 to upToN foreach { backend ! _ }
  }
}

object FactorialFrontend extends SprayJsonSupport with DefaultJsonProtocol {

  import akka.http.scaladsl.server.Directives._
  import akka.pattern.ask

  def routes(system: ActorSystem, frontend: ActorRef, timeout: Timeout) = {

    implicit val executionContext = system.dispatcher
    implicit val infoFormat = jsonFormat3(Info.apply)

    logRequestResult("akka-http") {
      path("info") {
        get {
          complete {
            (frontend ? GetInfo)(timeout).mapTo[Long].map {
              x => HttpResponse(entity = s"$x")
            }
          }
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {

    // Java will cache dns lookups by default, turn off cacheing
    java.security.Security.setProperty("networkaddress.cache.ttl", "0")

    val (upToN, repeat) = args.size match {
      case 0 => (10, true)
      case 1 => (args(0).toInt, true)
      case 2 => (args(0).toInt, args(1) == "true")
      case x =>
        println("Format: ...FactorialFrontend 'upToN' 'repeat'")
        System.exit(0)
    }

    val serverHost = "0.0.0.0"
    val serverPort = Option(System.getenv("PORT")).getOrElse("8080").toInt

    val internalIp = NetworkConfig.hostLocalAddress

    val appConfig = ConfigFactory.load("factorial")
    val clusterName = appConfig.getString("clustering.name")
    val internalSeedHostname = appConfig.getString("clustering.seed-host")
    val internalSeedPort = appConfig.getString("clustering.seed-port")
    val minMembers = appConfig.getNumber("akka.cluster.min-nr-of-members")

    val config = ConfigFactory.parseString("akka.cluster.roles = [frontend]").
      withFallback(NetworkConfig.seedsConfig(clusterName, internalSeedHostname, internalSeedPort)).
      withFallback(appConfig)

    implicit val system = ActorSystem(clusterName, config)
    implicit val materializer = ActorMaterializer()
    implicit val timeout = Timeout(5.seconds)
    implicit val executor = system.dispatcher
    system.log.info(s"Factorials will start when $minMembers backend members in the cluster.")

    Cluster(system) registerOnMemberUp {
      val frontend = system.actorOf(Props(classOf[FactorialFrontend], upToN, repeat), name = "factorialFrontend")
      Http().bindAndHandle(routes(system, frontend, timeout), serverHost, serverPort)
    }


  }
}
