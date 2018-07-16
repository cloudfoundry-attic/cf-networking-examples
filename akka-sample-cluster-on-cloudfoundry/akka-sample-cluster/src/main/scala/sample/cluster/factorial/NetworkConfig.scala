package sample.cluster.factorial

import java.net.InetAddress

import scala.collection.JavaConverters._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

object NetworkConfig {

  def hostLocalAddress: String = {
    System.getenv("CF_INSTANCE_INTERNAL_IP") match {
      case null => "127.0.0.1" // for local testing
      case ip => ip
    }
  }

  def seedsConfig(
                   clusterName: String,
                   internalSeedHostname: String,
                   internalSeedPort: String): Config = {
    ConfigFactory.empty().withValue("akka.cluster.seed-nodes",
      ConfigValueFactory.fromIterable(
        InetAddress.getAllByName(internalSeedHostname).toVector.map { address =>
            s"akka.tcp://$clusterName@${address.getHostAddress}:$internalSeedPort"
          }.asJava))
    // change 'akka.tcp' to 'akka' if artery is enabled
  }
}
