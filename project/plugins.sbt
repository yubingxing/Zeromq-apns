resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/" 

resolvers += "sbt-release-repo" at "http://gseitz.github.com/maven/"

resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
// See: https://github.com/mpeltonen/sbt-idea/tree/sbt-0.10
// Provides the `gen-idea` command to sync the IDEA project structure.

//addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "0.11.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0")

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")
 
