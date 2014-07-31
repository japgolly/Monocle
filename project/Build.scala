import sbt._
import Keys._

import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.SonatypeKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

import scala.scalajs.sbtplugin.ScalaJSPlugin._

object BuildSettings {
  import MonoclePublishing._
  val buildScalaVersion = "2.11.2"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization       := "com.github.japgolly.fork.monocle",
    version            := "0.4.0-2",
    scalaVersion       := buildScalaVersion,
    crossScalaVersions := Seq("2.10.4", "2.11.2"),
    scalacOptions     ++= Seq("-deprecation", "-unchecked", "-feature",
      "-language:higherKinds", "-language:implicitConversions", "-language:postfixOps"),
    incOptions         := incOptions.value.withNameHashing(true),
    resolvers          += Resolver.sonatypeRepo("releases"),
    resolvers          += Resolver.sonatypeRepo("snapshots")
  ) ++ publishSettings ++ scalaJSBuildSettings
}

object Dependencies {
  val scalaz            = "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.0.6-2"
  val scalaCheck        = "org.scalacheck"  %% "scalacheck"                % "1.11.3"
  val scalaCheckBinding = "org.scalaz"      %% "scalaz-scalacheck-binding" % "7.0.6"   % "test"
  val specs2            = "org.specs2"      %% "specs2"                    % "2.3.11"  % "test"
  val scalazSpec2       = "org.typelevel"   %% "scalaz-specs2"             % "0.2"     % "test"
}

object MonocleBuild extends Build {
  import BuildSettings._
  import Dependencies._

  lazy val root: Project = Project(
    "monocle",
    file("."),
    settings = buildSettings ++ Seq(
      publishArtifact := false,
      run <<= run in Compile in macros) ++ sonatypeSettings
  ) aggregate(core, law, macros, generic, test, example)

  lazy val core: Project = Project(
    "monocle-core",
    file("core"),
    settings = buildSettings ++ mimaDefaultSettings ++ Seq(
      libraryDependencies ++= Seq(scalaz),
      previousArtifact     := Some("com.github.julien-truffaut"  %  "monocle-core_2.10" % "0.3.0")
    )
  )

  lazy val law: Project = Project(
    "monocle-law",
    file("law"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(scalaz, scalaCheck)
    )
  ) dependsOn(core)

  lazy val macros: Project = Project(
    "monocle-macro",
    file("macro"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang"  %  "scala-reflect"  % scalaVersion.value,
        "org.scala-lang"  %  "scala-compiler" % scalaVersion.value % "provided"
      ),
      libraryDependencies := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
          case Some((2, scalaMajor)) if scalaMajor >= 11 => libraryDependencies.value
          // in Scala 2.10, quasiquotes are provided by macro paradise
          case Some((2, 10)) => libraryDependencies.value ++ Seq(
            compilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full),
            "org.scalamacros" %% "quasiquotes" % "2.0.0" cross CrossVersion.binary
          )
        }
      }
    )
  ) dependsOn(core)

  lazy val generic: Project = Project(
    "monocle-generic",
    file("generic"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(scalaz),
      // TODO extract to reuse shapeless dependency definition in other modules
      libraryDependencies ++= Seq(CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>  "com.chuusai" %% "shapeless"        % "2.0.0"
        case Some((2, 10))                             =>  "com.chuusai" %  "shapeless_2.10.4" % "2.0.0"
      })
    )
  ) dependsOn(core)

  lazy val test: Project = Project(
    "monocle-test",
    file("test"),
    settings = buildSettings ++ Seq(
      publishArtifact      := false,
      libraryDependencies ++= Seq(scalaz, scalaCheck, scalaCheckBinding, specs2, scalazSpec2),
      libraryDependencies ++= Seq(CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>  "com.chuusai" %% "shapeless"        % "2.0.0"
        case Some((2, 10))                             =>  "com.chuusai" %  "shapeless_2.10.4" % "2.0.0"
      })
    )
  ) dependsOn(core, generic ,law)

  lazy val example: Project = Project(
    "monocle-example",
    file("example"),
    settings = buildSettings ++ Seq(
      publishArtifact      := false,
      libraryDependencies ++= Seq(scalaz, specs2),
      libraryDependencies ++= Seq(CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>  "com.chuusai" %% "shapeless"        % "2.0.0"
        case Some((2, 10))                             =>  "com.chuusai" %  "shapeless_2.10.4" % "2.0.0"
      })
    )
  ) dependsOn(core, macros, generic, test % "test->test")
}

object MonoclePublishing  {

  lazy val publishSettings: Seq[Setting[_]] = Seq(
    pomExtra := {
      <url>https://github.com/julien-truffaut/Monocle</url>
        <licenses>
          <license>
            <name>MIT</name>
            <url>http://opensource.org/licenses/MIT</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git:github.com/julien-truffaut/Monocle</connection>
          <developerConnection>scm:git:git@github.com:julien-truffaut/Monocle.git</developerConnection>
          <url>github.com:julien-truffaut/Monocle.git</url>
        </scm>
        <developers>
          <developer>
            <id>julien-truffaut</id>
            <name>Julien Truffaut</name>
          </developer>
        </developers>
    }
  ) ++ sonatypeSettings

}
