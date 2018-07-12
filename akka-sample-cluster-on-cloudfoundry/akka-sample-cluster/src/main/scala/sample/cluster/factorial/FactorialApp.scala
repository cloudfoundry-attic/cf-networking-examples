package sample.cluster.factorial

object FactorialApp {
  def main(args: Array[String]): Unit = {
    // starting 3 backend nodes and 1 frontend node
    FactorialBackend.main(Seq("2551").toArray)
    FactorialBackend.main(Seq("2552").toArray)
    FactorialBackend.main(Seq("2553").toArray)
    FactorialFrontend.main(Array.empty)
  }
}
