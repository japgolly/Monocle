# Monocle on Scala.js

The `core` module has been compiled for Scala.JS and published to Maven central under `com.github.japgolly.fork.monocle`.

#### Usage

build.sbt
```
libraryDependencies += "com.github.japgolly.fork.monocle" %%% "monocle-core" % "0.5.1"

// For macros
libraryDependencies ++= Seq(
  "com.github.japgolly.fork.monocle" %%% "monocle-macro" % "0.5.1",
  compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
```
