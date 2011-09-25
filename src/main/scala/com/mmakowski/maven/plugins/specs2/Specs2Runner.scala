package com.mmakowski.maven.plugins.specs2

import org.specs2.runner.TestInterfaceRunner
import org.apache.maven.plugin.logging.Log
import org.scalatools.testing.{Event, EventHandler}
import org.scalatools.testing.Result._
import java.net.{URL, URLClassLoader}
import java.io.File
import org.apache.maven.project.MavenProject
import org.apache.maven.artifact.Artifact
import scala.collection.JavaConversions._

/**
 * @author Maciek Makowski
 */
class Specs2Runner {
  private class TestHandler(val log: Log) extends EventHandler {
    def handle(event: Event) = {
      log.info(event.description)
      log.info(event.error)
    }
  }
  
  def runSpecs(log: Log, project: MavenProject, classesDir: File, testClassesDir: File): Unit = {
    //log.info(getClass.getClassLoader().asInstanceOf[URLClassLoader].getURLs().toSeq.toString)
    log.info(project.getArtifacts.asInstanceOf[java.util.Set[Artifact]].toString)
    val classpath = buildClasspath(project, classesDir, testClassesDir) //
    log.debug("test classpath: " + classpath)
    val runner = new TestInterfaceRunner(new URLClassLoader(classpath.toArray[URL]), Array())
    runner.runSpecification("com.mmakowski.scratch.AppySpec", new TestHandler(log), Array("html", "junitxml"))
  }
  
  private def buildClasspath(project: MavenProject, classesDir: File, testClassesDir: File) = {
    def url(file: File) = new URL(file.getAbsoluteFile.toURI.toASCIIString)
    def urlsFrom(artifacts: Set[Artifact]) = artifacts.map(_.getFile).map(url(_))
    val artifacts = Set() ++ project.getArtifacts.asInstanceOf[java.util.Set[Artifact]]
    // new URL( file.toURI().toASCIIString() )
    //Array()) // c:/Data/projects/attic/java-with-specs2/
    Seq(url(testClassesDir), url(classesDir)) ++ urlsFrom(artifacts)
  }
}