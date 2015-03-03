import sbt._
import Keys._

import xerial.sbt.Sonatype._
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

import org.typelevel.sbt.TypelevelPlugin._

import pl.project13.scala.sbt.SbtJmh._
import JmhKeys._

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.toScalaJSGroupID

object BuildSettings {
  import MonoclePublishing._
  val buildScalaVersion = "2.11.5"
  val previousVersion   = "1.0.0"

  def scalajs: Project => Project =
    _.enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin)
      .settings(scalacOptions += sourceMapOpt)

  val sourceMapOpt = {
    val a = new java.io.File("").toURI.toString.replaceFirst("/$", "")
    val g = "https://raw.githubusercontent.com/japgolly/Monocle/v1.0.1-js"
    s"-P:scalajs:mapSourceURI:$a->$g/"
  }

  val buildSettings = typelevelDefaultSettings ++ Seq(
    organization       := "com.github.japgolly.fork.monocle",
    scalaVersion       := buildScalaVersion,
    crossScalaVersions := Seq("2.10.4", "2.11.5"),
    version := "1.0.1-2",
    scalacOptions     ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:implicitConversions", "-language:higherKinds", "-language:postfixOps",
      "-unchecked",
      "-Yno-generic-signatures",
      "-Yno-adapted-args",
      "-Ywarn-value-discard"
    ),
    incOptions         := incOptions.value.withNameHashing(true),
    resolvers          += Resolver.sonatypeRepo("releases"),
    resolvers          += Resolver.sonatypeRepo("snapshots"),
    resolvers          += "bintray/non" at "http://dl.bintray.com/non/maven"
  ) ++ publishSettings ++ ScalaJSPlugin.projectSettings
}

object Dependencies {
  def scalaz            = Def setting ("com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.1-2")
  val scalaCheckBinding = "org.scalaz"      %% "scalaz-scalacheck-binding" % "7.1.0"  % "test"
  val specs2Scalacheck  = "org.specs2"      %% "specs2-scalacheck"         % "2.4.14"
  val scalazSpec2       = "org.typelevel"   %% "scalaz-specs2"             % "0.3.0"  % "test"
  val shapeless = Def setting (
    CrossVersion partialVersion scalaVersion.value match {
      case Some((2, scalaMajor)) if scalaMajor >= 11 => "com.chuusai" %% "shapeless" % "2.0.0"
      case Some((2, 10))                             => "com.chuusai" %  "shapeless" % "2.0.0" cross CrossVersion.full
    }
  )

  val macroVersion = "2.0.1"
  val paradisePlugin = compilerPlugin("org.scalamacros" % "paradise"        % macroVersion cross CrossVersion.full)
  val kindProjector  = compilerPlugin("org.spire-math"  %% "kind-projector" % "0.5.2")
}

object MonocleBuild extends Build {
  import BuildSettings._
  import Dependencies._

  lazy val root: Project = Project(
    "monocle",
    file("."),
    settings = buildSettings ++ Seq(
      publishArtifact := false,
      run <<= run in Compile in macros)
  ).aggregate(core, law, macros, generic, test, example, bench)
  .configure(scalajs)

  lazy val core: Project = Project(
    "monocle-core",
    file("core"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(scalaz.value),
      addCompilerPlugin(kindProjector),
      previousArtifact     := Some("com.github.julien-truffaut"  %  "monocle-core_2.11" % previousVersion)
    )
  )
  .configure(scalajs)

  lazy val law: Project = Project(
    "monocle-law",
    file("law"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(scalaz.value, specs2Scalacheck),
      previousArtifact := Some("com.github.julien-truffaut"  %  "monocle-law_2.11" % previousVersion)
    )
  ) dependsOn(core)
  .configure(scalajs)

  lazy val macros: Project = Project(
    "monocle-macro",
    file("macro"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang"  %  "scala-reflect"  % scalaVersion.value,
        "org.scala-lang"  %  "scala-compiler" % scalaVersion.value % "provided"
      ),
      addCompilerPlugin(paradisePlugin),
      libraryDependencies ++= CrossVersion partialVersion scalaVersion.value collect {
        case (2, scalaMajor) if scalaMajor < 11 =>
          // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
          Seq("org.scalamacros" %% "quasiquotes" % macroVersion)
      } getOrElse Nil
    )
  ) dependsOn(core)
  .configure(scalajs)

  lazy val generic: Project = Project(
    "monocle-generic",
    file("generic"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(scalaz.value, shapeless.value),
      previousArtifact := Some("com.github.julien-truffaut"  %  "monocle-generic_2.11" % previousVersion)
    )
  ) dependsOn(core)
  .configure(scalajs)

  lazy val test: Project = Project(
    "monocle-test",
    file("test"),
    settings = buildSettings ++ Seq(
      publishArtifact      := false,
      libraryDependencies ++= Seq(scalaz.value, scalaCheckBinding, scalazSpec2, specs2Scalacheck, shapeless.value)
    )
  ) dependsOn(core, generic ,law)

  lazy val example: Project = Project(
    "monocle-example",
    file("example"),
    settings = buildSettings ++ Seq(
      publishArtifact      := false,
      libraryDependencies ++= Seq(scalaz.value, specs2Scalacheck, shapeless.value),
      addCompilerPlugin(paradisePlugin) // Unfortunately necessary :( see: http://stackoverflow.com/q/23485426/463761
    )
  ) dependsOn(core, macros, generic, test % "test->test")

  lazy val bench: Project = Project(
    "monocle-bench",
    file("bench"),
    settings = buildSettings ++ jmhSettings ++ Seq(
      addCompilerPlugin(kindProjector)
    )
  ) dependsOn(core)
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
