import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtunidoc.Plugin.UnidocKeys._

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.toScalaJSGroupID

lazy val buildSettings = Seq(
  organization       := "com.github.japgolly.fork.monocle",
  scalaVersion       := "2.11.7",
  crossScalaVersions := Seq("2.10.6", "2.11.7"),
  scalacOptions     ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:implicitConversions", "-language:higherKinds", "-language:postfixOps",
    "-unchecked",
    "-Xfatal-warnings",
    "-Yinline-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard",
    "-Xfuture"
  ),
  resolvers ++= Seq(
    "bintray/non" at "http://dl.bintray.com/non/maven",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scmInfo := Some(ScmInfo(url("https://github.com/julien-truffaut/Monocle"), "scm:git:git@github.com:julien-truffaut/Monocle.git"))
)

lazy val scalaz     = "com.github.japgolly.fork.scalaz"      %%%! "scalaz-core" % "7.2.0"
lazy val shapeless  = "com.chuusai"     %%%! "shapeless"   % "2.2.5"
lazy val refinedDep = "eu.timepit"      %% "refined"     % "0.3.2"

lazy val discpline  = "org.typelevel"   %% "discipline"  % "0.4"
lazy val scalatest  = "org.scalatest"   %% "scalatest"   % "3.0.0-M7"  % "test"

lazy val macroCompat = "org.typelevel" %%%! "macro-compat" % "1.1.0"

lazy val macroVersion = "2.1.0"
lazy val paradisePlugin = compilerPlugin("org.scalamacros" %  "paradise"       % macroVersion cross CrossVersion.full)

def mimaSettings(module: String): Seq[Setting[_]] = mimaDefaultSettings ++ Seq(
  previousArtifact := Some("com.github.julien-truffaut" %  (s"monocle-${module}_2.11") % "1.1.0")
)

def scalajs: Project => Project =
	_.enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin)
		.settings(ScalaJSPlugin.projectSettings: _*)
		.settings(scalacOptions in Compile += sourceMapOpt)

val sourceMapOpt = {
	val a = new java.io.File("").toURI.toString.replaceFirst("/$", "")
	val g = "https://raw.githubusercontent.com/japgolly/Monocle/v1.2.0-js"
	s"-P:scalajs:mapSourceURI:$a->$g/"
}

lazy val monocleSettings = buildSettings ++ publishSettings // ++ ScalaJSPlugin.projectSettings

lazy val monocle = project.in(file("."))
  .settings(moduleName := "monocle")
  .settings(monocleSettings)
  .aggregate(core, generic, law, macros, state, refined, test, example, docs, bench)
  .dependsOn(core, generic, law, macros, state, refined, test % "test-internal -> test", bench % "compile-internal;test-internal -> test")
  .configure(scalajs)

lazy val core = project
  .settings(moduleName := "monocle-core")
  .settings(monocleSettings)
  .settings(mimaSettings("core"))
  .settings(libraryDependencies := Seq(scalaz))
  .configure(scalajs)

lazy val generic = project.dependsOn(core)
  .settings(moduleName := "monocle-generic")
  .settings(monocleSettings)
  .settings(mimaSettings("generic"))
  .settings(libraryDependencies := Seq(scalaz, shapeless))
  .configure(scalajs)

lazy val refined = project.dependsOn(core)
  .settings(moduleName := "monocle-refined")
  .settings(monocleSettings)
  .settings(libraryDependencies := Seq(scalaz, refinedDep))

lazy val law = project.dependsOn(core)
  .settings(moduleName := "monocle-law")
  .settings(monocleSettings)
  .settings(libraryDependencies := Seq(discpline))

lazy val macros = project.dependsOn(core)
  .in(file("macro"))
  .settings(moduleName := "monocle-macro")
  .settings(monocleSettings)
  .settings(Seq(
  scalacOptions  += "-language:experimental.macros",
  libraryDependencies ++= Seq(
    "org.scala-lang"  %  "scala-reflect"  % scalaVersion.value,
    "org.scala-lang"  %  "scala-compiler" % scalaVersion.value % "provided",
    macroCompat
  ),
  addCompilerPlugin(paradisePlugin),
  libraryDependencies ++= CrossVersion partialVersion scalaVersion.value collect {
    case (2, scalaMajor) if scalaMajor < 11 => Seq("org.scalamacros" %% "quasiquotes" % macroVersion)
  } getOrElse Nil,
  unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / s"scala-${scalaBinaryVersion.value}"
  ))
  .configure(scalajs)

lazy val state = project.dependsOn(core)
  .settings(moduleName := "monocle-state")
  .settings(monocleSettings)
  .settings(libraryDependencies := Seq(scalaz))
  .configure(scalajs)

lazy val test = project.dependsOn(core, generic, macros, law, state, refined)
  .settings(moduleName := "monocle-test")
  .settings(monocleSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(scalaz, shapeless, scalatest, compilerPlugin(paradisePlugin))
  )

lazy val bench = project.dependsOn(core, generic, macros)
  .settings(moduleName := "monocle-bench")
  .settings(monocleSettings)
  .settings(noPublishSettings)
  .settings(libraryDependencies ++= Seq(
    shapeless,
    compilerPlugin(paradisePlugin)
  )).enablePlugins(JmhPlugin)

lazy val example = project.dependsOn(core, generic, refined, macros, state, test % "test->test")
  .settings(moduleName := "monocle-example")
  .settings(monocleSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(scalaz, shapeless, scalatest, compilerPlugin(paradisePlugin))
  )

lazy val docs = project.dependsOn(core, example)
  .settings(moduleName := "monocle-docs")
  .settings(monocleSettings)
  .settings(noPublishSettings)
  .settings(unidocSettings)
  .settings(site.settings)
  .settings(ghpages.settings)
  .settings(docSettings)
  .settings(tutSettings)
  .settings(
    libraryDependencies ++= Seq(scalaz, shapeless, compilerPlugin(paradisePlugin))
  )


lazy val docSettings = Seq(
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core),
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
  site.addMappingsToSiteDir(tut, "_tut"),
  ghpagesNoJekyll := false,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  ),
  git.remoteRepo := "git@github.com:julien-truffaut/Monocle.git",
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
)


lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/julien-truffaut/Monocle")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  autoAPIMappings := true,
  apiURL := Some(url("https://julien-truffaut.github.io/Monocle/api/")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <developers>
      <developer>
        <id>julien-truffaut</id>
        <name>Julien Truffaut</name>
      </developer>
      <developer>
        <id>NightRa</id>
        <name>Ilan Godik</name>
      </developer>
      <developer>
        <id>aoiroaoino</id>
        <name>Naoki Aoyama</name>
      </developer>
    </developers>
    ),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

addCommandAlias("validate", ";compile;test;unidoc;tut")

// For Travis CI - see http://www.cakesolutions.net/teamblogs/publishing-artefacts-to-oss-sonatype-nexus-using-sbt-and-travis-ci
credentials ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
