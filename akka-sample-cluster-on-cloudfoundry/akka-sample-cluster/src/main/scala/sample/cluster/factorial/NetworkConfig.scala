package sample.cluster.factorial

import java.net.{InetAddress, NetworkInterface, URLEncoder}

import scala.collection.JavaConversions._
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http.{Http, HttpOptions, HttpResponse}

/**
  * Created by Admin on 2016-08-31.
  */
object NetworkConfig {

  def hostLocalAddress: String = System.getenv("CF_INSTANCE_INTERNAL_IP")

  def seedsConfig(
                   clusterName: String,
                   internalSeedHostname: String,
                   internalSeedPort: String): Config = {
    ConfigFactory.empty().withValue("akka.cluster.seed-nodes",
      ConfigValueFactory.fromIterable(
        InetAddress.getAllByName(internalSeedHostname).map(_.getHostAddress).toSeq.
          map{ case(ip) => s"akka.tcp://$clusterName@$ip:$internalSeedPort"}))
  }
}
