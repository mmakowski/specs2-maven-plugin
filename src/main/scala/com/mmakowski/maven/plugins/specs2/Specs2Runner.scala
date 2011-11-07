package com.mmakowski.maven.plugins.specs2

import org.specs2.runner.{HtmlRunner, TestInterfaceRunner}
import org.scalatools.testing.{Event, EventHandler, Logger}
import org.scalatools.testing.Result
import org.scalatools.testing.Result._
import java.net.{URL, URLClassLoader}
import java.io.File
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import scalax.file.{Path, PathMatcher}
import scalax.file.PathMatcher._
import scala.collection.JavaConversions._

import scala.collection.mutable.{Map, ListBuffer}

/**
 * Executes pre-compiled specifications found in test classes directory. 
 * 
 * @author Maciek Makowski
 * @since 0.1.0
 */
class Specs2Runner {
  // TODO: clean up

  private class AggregatingHandler extends EventHandler {
    val testCounts: Map[String, Int] = Map() withDefaultValue 0
    
    def handle(event: Event) {
      val resultType = event.result.toString
      testCounts(resultType) = testCounts(resultType) + 1  
    }
    
    def report = Result.values.map(_.toString).map(s => s + ": " + testCounts(s)).mkString(" "*3)

    def noErrorsOrFailures = testCounts("Error") + testCounts("Failure") == 0
    
  }
  
  /**
   * Logger for test interface -- funnels all messages except for errors into maven debug logger 
   */
  private class DebugLevelLogger(log: Log) extends Logger {
    def ansiCodesSupported = false
    def error(msg: String) = log.debug(msg)
    def warn(msg: String) = log.debug(msg)
    def info(msg: String) = log.debug(msg)
    def debug(msg: String) = log.debug(msg)
    def trace(t: Throwable) = log.error(t)
  }
  
  def runSpecs(log: Log, project: MavenProject, classesDir: File, testClassesDir: File, suffix: String): java.lang.Boolean = {
    val failures = ListBuffer[String]()

    val classpath = {
      def url(file: File) = new URL(file.getAbsoluteFile.toURI.toASCIIString)
      def urlsOf(artifacts: Set[Artifact]) = artifacts.map(_.getFile).map(url(_))
      val dependencies = Set() ++ project.getArtifacts.asInstanceOf[java.util.Set[Artifact]]
      Seq(url(testClassesDir), url(classesDir)) ++ urlsOf(dependencies)
    }
    log.debug("test classpath: " + classpath)

    val fullSuffix = "%s.class" format suffix
    log.debug("searching for specs ending in %s" format fullSuffix)
    val testClassesPath = Path(testClassesDir.getAbsolutePath)
    val specs = findSpecsIn(testClassesPath, "", _.name.endsWith(fullSuffix))
    
    val classLoader = new URLClassLoader(classpath.toArray[URL], getClass.getClassLoader)
    def runWithTestClassLoader[T](threadName: String, body: => T) = {
      object runner extends Runnable {
        var success = false
        def run() = {
          try {
            body
            success = true
          } catch {
            case e => log.error(e)
          }
        }
      }
      val t = new Thread(runner, threadName)
      t.setContextClassLoader(classLoader)
      t.start()
      t.join()
      runner.success
    }
    
    // test class loader is set on both parent thread and TestInterfaceRunner -- this is probably redundant 
    val runner = new TestInterfaceRunner(classLoader, Array(new DebugLevelLogger(log)))
    def runSpec(succesfulSoFar: Boolean, spec: String) = {
      log.info(spec + ":")
      val handler = new AggregatingHandler
      val runCompleted = runWithTestClassLoader("spec runner", runner.runSpecification(spec, handler, Array("console", "html", "junitxml")))
      log.info(handler.report)

      val result = runCompleted && handler.noErrorsOrFailures
      if (!result) failures += spec
      succesfulSoFar && result
    }
    
    def generateIndex() = {
      val indices = findSpecsIn(testClassesPath, "", _.name == "index.class")
      if (indices isEmpty) true else {
        val index = indices(0)
        log.info("generating " + index)
        runWithTestClassLoader("index generator", new HtmlRunner().start(index))
      }
    }

    def printFailingSpecs() {
      if (!failures.isEmpty) log.info(failures.mkString("\n\nSpecs Failing or In Error:\n", "\n", "\n\n"))
    }

    val result = specs.foldLeft(true)(runSpec)
    printFailingSpecs()
    result && generateIndex
  }

  private def findSpecsIn(dir: Path, pkg: String, pred: Path => Boolean): Seq[String] = {
    def qualified(name: String) = if (pkg.isEmpty) name else pkg + "." + name
    dir.children().toSeq.collect {
      case IsFile(f) if pred(f) => Seq(qualified(f.name.take(f.name.length - ".class".length))) 
      case IsDirectory(d) => findSpecsIn(d, qualified(d.name), pred)
    }.flatten
  }
}