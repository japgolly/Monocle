# Monocle on Scala.js

The `core` module has been compiled for Scala.JS and published to Maven central under `com.github.japgolly.fork.monocle`.

#### Usage

build.sbt
```scala
libraryDependencies += "com.github.japgolly.fork.monocle" %%% "monocle-core" % "1.1.1"

// For macros:

libraryDependencies += "com.github.japgolly.fork.monocle" %%% "monocle-macro" % "1.1.1"

addCompilerPlugin(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
```

