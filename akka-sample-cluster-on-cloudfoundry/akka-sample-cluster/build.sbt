val akkaV = "2.4.3"

resolvers ++= Seq("Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/")

val Frontend = config("frontend") extend(Compile)
val Backend = config("backend") extend(Compile)
def customAssemblySettings =
  inConfig(Frontend)(baseAssemblySettings ++
    inTask(assembly)(mainClass := Some("sample.cluster.factorial.FactorialFrontend")) ++
    inTask(assembly)(assemblyJarName := "akka-sample-frontend.jar")) ++
    inConfig(Backend)(baseAssemblySettings ++
      inTask(assembly)(mainClass := Some("sample.cluster.factorial.FactorialBackend")) ++
      inTask(assembly)(assemblyJarName := "akka-sample-backend.jar"))

val project = Project(
  id = "akka-sample-cluster-scala",
  base = file("."),
  settings = customAssemblySettings ++ Defaults.coreDefaultSettings ++ Seq(
    name := """akka-sample-cluster""",
    scalaVersion := "2.11.8",
    // scalaVersion := provided by Typesafe Reactive Platform
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % akkaV,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaV,
      "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
      "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
      "com.typesafe.akka" %% "akka-contrib" % akkaV,
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "com.typesafe.play" %% "play-json" % "2.3.4",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"//,
      /*"org.fusesource" % "sigar" % "1.6.4"*/),
    javaOptions in run ++= Seq(
      //"-Djava.library.path=./sigar",
      "-Xms128m", "-Xmx1024m"),
    Keys.fork in run := true,  
    mainClass in (Compile, run) := Some("sample.cluster.simple.SimpleClusterApp"),
    parallelExecution in Test := false,
    mainClass in assembly := Some("sample.cluster.factorial.FactorialApp")
  )
)
