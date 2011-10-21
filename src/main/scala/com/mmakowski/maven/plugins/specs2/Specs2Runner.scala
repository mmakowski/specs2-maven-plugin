package com.mmakowski.maven.plugins.specs2

import org.specs2.runner.{HtmlRunner, TestInterfaceRunner}
import org.scalatools.testing.{Event, EventHandler, Logger}
import org.scalatools.testing.Result._
import java.net.{URL, URLClassLoader}
import java.io.File
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import scalax.file.{Path, PathMatcher}
import scalax.file.PathMatcher._
import scala.collection.JavaConversions._

import scala.collection.mutable.Map

/**
 * Executes pre-compiled specifications found in test classes directory. Expects all specifiations to have class names ending with "Spec". 
 * 
 * @author Maciek Makowski
 * @since 1.0.0
 */
class Specs2Runner {
  // TODO: clean up
  
  private class AggregatingHandler extends EventHandler {
    val testCounts: Map[String, Int] = Map() withDefaultValue 0
    
    def handle(event: Event) = {
      val resultType = event.result.toString
      testCounts(resultType) = testCounts(resultType) + 1  
    }
    
    def report = List("Success", "Failure", "Error").map(s => s + ": " + testCounts(s)).mkString(" "*3)

    def noErrorsOrFailures = testCounts("Error") + testCounts("Failure") == 0
    
  }
  
  private class DebugLevelLogger(log: Log) extends Logger {
    def ansiCodesSupported = false
    def error(msg: String) = log.debug(msg)
    def warn(msg: String) = log.debug(msg)
    def info(msg: String) = log.debug(msg)
    def debug(msg: String) = log.debug(msg)
    def trace(t: Throwable) = log.error(t)
  }
  
  def runSpecs(log: Log, project: MavenProject, classesDir: File, testClassesDir: File, suffix: String): java.lang.Boolean = {
    val classpath = {
      def url(file: File) = new URL(file.getAbsoluteFile.toURI.toASCIIString)
      def urlsOf(artifacts: Set[Artifact]) = artifacts.map(_.getFile).map(url(_))
      val dependencies = Set() ++ project.getArtifacts.asInstanceOf[java.util.Set[Artifact]]
      Seq(url(testClassesDir), url(classesDir)) ++ urlsOf(dependencies)
    }
    log.debug("test classpath: " + classpath)

    val fullSuffix = "%s.class" format suffix
    log.info("searching for specs ending in %s" format fullSuffix)
    val testClassesPath = Path(testClassesDir.getAbsolutePath)
    val specs = findSpecsIn(testClassesPath, "", _.name.endsWith(fullSuffix))
    log.debug("specifications found: " + specs)
    
    val classLoader = new URLClassLoader(classpath.toArray[URL], getClass.getClassLoader)
    val runner = new TestInterfaceRunner(classLoader, Array(new DebugLevelLogger(log)))
    def runSpec(succesfulSoFar: Boolean, spec: String) = {
      log.info(spec + " Starting...")

      val handler = new AggregatingHandler
      runner.runSpecification(spec, handler, Array("console", "html", "junitxml"))

      log.info(spec + " Completed!")
      log.info(handler.report)
      
      succesfulSoFar && handler.noErrorsOrFailures
    }
    
    def generateIndex() = {
      val indices = findSpecsIn(testClassesPath, "", _.name == "index.class")
      if (indices isEmpty) true else {
        val index = indices(0)
        log.info("generating " + index)
        object generator extends Runnable {
          var success = false
          def run() = {
            try {
              new HtmlRunner().start(index)
              success = true
            } catch {
              case e => log.error(e)
            }
          }
        }
        val t = new Thread(generator, "index generator")
        t.setContextClassLoader(classLoader)
        t.start()
        t.join()
        generator.success
      }
    }
    
    specs.foldLeft(true)(runSpec) && generateIndex 
  }

  private def findSpecsIn(dir: Path, pkg: String, pred: Path => Boolean): Seq[String] = {
    def qualified(name: String) = if (pkg.isEmpty) name else pkg + "." + name
    dir.children().toSeq.collect {
      case IsFile(f) if pred(f) => Seq(qualified(f.name.take(f.name.length - ".class".length))) 
      case IsDirectory(d) => findSpecsIn(d, qualified(d.name), pred)
    }.flatten
  }
}