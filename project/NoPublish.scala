import sbt._
import sbt.Keys._
import java.io.File

/**
 * For projects that are not to be published. Copied from: https://github.com/akka/akka/blob/12ca84c247109cefedeb82303cc7ddcc972e1db8/project/Publish.scala
 */
object NoPublish extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  override def projectSettings =
    Seq(skip in publish := true, sources in (Compile, doc) := Seq.empty)
}
