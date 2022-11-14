name := """csvParser"""

organization := "com.ruimo"

scalaVersion := "2.13.10"

crossScalaVersions := List("2.12.3", "2.13.10", "3.0.10") 

resolvers += "ruimo.com" at "https://static.ruimo.com/release"

publishTo := Some(
  Resolver.file(
    "recoengcommon",
    new File(Option(System.getenv("RELEASE_DIR")).getOrElse("/tmp"))
  )
)

libraryDependencies ++= Seq(
  "org.specs2" % "specs2-core_2.13" % "4.12.0" % "test",
  "com.ruimo" %% "scoins" % "1.26"
)

Test / scalacOptions ++= Seq("-Yrangepos")

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

resolvers += Resolver.jcenterRepo
