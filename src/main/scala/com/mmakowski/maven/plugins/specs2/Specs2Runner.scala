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
 * Executes pre-compiled specifications found in test classes directory. 
 * 
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
    val classpath = buildClasspath(project, classesDir, testClassesDir) //
    log.debug("test classpath: " + classpath)
    val classLoader = new URLClassLoader(classpath.toArray[URL], getClass.getClassLoader)
    val runner = new TestInterfaceRunner(classLoader, Array())
    runner.runSpecification("HelloWorldSpec", new TestHandler(log), Array("html", "junitxml"))
  }
  
  private def buildClasspath(project: MavenProject, classesDir: File, testClassesDir: File) = {
    def url(file: File) = new URL(file.getAbsoluteFile.toURI.toASCIIString)
    def urlsOf(artifacts: Set[Artifact]) = artifacts.map(_.getFile).map(url(_))
    val dependencies = Set() ++ project.getArtifacts.asInstanceOf[java.util.Set[Artifact]]
    Seq(url(testClassesDir), url(classesDir)) ++ urlsOf(dependencies)
  }
}