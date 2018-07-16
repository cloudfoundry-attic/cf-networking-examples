val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.1.3"

val Frontend = config("frontend") extend(Compile)
val Backend = config("backend") extend(Compile)
def customAssemblySettings =
  inConfig(Frontend)(baseAssemblySettings ++
    inTask(assembly)(mainClass := Some("sample.cluster.factorial.FactorialFrontend")) ++
    inTask(assembly)(assemblyJarName := "akka-sample-frontend.jar")) ++
    inConfig(Backend)(baseAssemblySettings ++
      inTask(assembly)(mainClass := Some("sample.cluster.factorial.FactorialBackend")) ++
      inTask(assembly)(assemblyJarName := "akka-sample-backend.jar"))


lazy val `akka-sample-cluster-scala` = project
  .in(file("."))
  .settings(
    customAssemblySettings,
    name := "akka-sample-cluster",
    scalaVersion := "2.12.6",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test),
    fork in run := true,
    mainClass in (Compile, run) := Some("sample.cluster.factorial.FactorialApp"),
    parallelExecution in Test := false,
    mainClass in assembly := Some("sample.cluster.factorial.FactorialApp"),
    licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))
  )
