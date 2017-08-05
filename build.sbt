name := """csvParser"""

organization := "com.ruimo"

scalaVersion := "2.12.3"

crossScalaVersions := List("2.11.8", "2.12.3") 

resolvers += "ruimo.com" at "http://static.ruimo.com/release"

publishTo := Some(
  Resolver.file(
    "recoengcommon",
    new File(Option(System.getenv("RELEASE_DIR")).getOrElse("/tmp"))
  )
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.9.1" % "test",
  "com.ruimo" %% "scoins" % "1.12"
)

scalacOptions in Test ++= Seq("-Yrangepos")

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
