package com.mmakowski.maven.plugins.specs2

//import org.specs2.runner.{HtmlRunner, TestInterfaceRunner}
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

import scala.collection.mutable.{Map}

/**
 * Executes pre-compiled specifications found in test classes directory. 
 * 
 * @author Maciek Makowski
 * @since 0.1.0
 */
class Specs2Runner {
  /**
   * A handler that counts the total for each possible result of test when running a Spec
   */
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
  
  def runSpecs(log: Log, project: MavenProject, suffix: String): java.lang.Boolean = {
    val classpath = urlsOf(List() ++ project.getTestClasspathElements().asInstanceOf[java.util.List[String]])
    log.debug("test classpath: " + classpath)
    val fullSuffix = "%s.class" format suffix
    log.debug("searching for specs ending in %s" format fullSuffix)
    val testClassesPath = Path(project.getBuild.getTestOutputDirectory)
    val specs = findSpecsIn(testClassesPath, "", _.name.endsWith(fullSuffix))
    val classLoader = new URLClassLoader(classpath.toArray[URL], getClass.getClassLoader)
    val runWithTestClassLoader = runWithClassLoader(classLoader, log)_
    
    val runner = new TestInterfaceRunner(classLoader, log)
    
    def runSpec(failingSpecs: Seq[String], spec: String): Seq[String] = {
      log.info(spec + ":")
      val handler = new AggregatingHandler
      val runCompleted = runWithTestClassLoader("spec runner", runner.runSpecification(spec, handler, Array("console", "html", "junitxml")))
      log.info(handler.report)

      val result = runCompleted && handler.noErrorsOrFailures
      if (!result) failingSpecs :+ spec else failingSpecs
    }
    
    def generateIndex() = {
      val indices = findSpecsIn(testClassesPath, "", _.name == "index.class")
      if (indices isEmpty) true else {
        val index = indices(0)
        log.info("generating " + index)
        runWithTestClassLoader("index generator", new HtmlRunner(classLoader).start(index))
      }
    }

    val failures = specs.foldLeft(List(): Seq[String])(runSpec)
    logFailingSpecs(failures, log)
    val indexGenerationSuccesful = generateIndex()
    failures.isEmpty && indexGenerationSuccesful
  }
  
  def url(file: File) = new URL(file.getAbsoluteFile.toURI.toASCIIString)

  def urlsOf(files: List[String]) = files.map(new File(_)).map(url(_))

  def findSpecsIn(dir: Path, pkg: String, pred: Path => Boolean): Seq[String] = {
    def qualified(name: String) = if (pkg.isEmpty) name else pkg + "." + name
    dir.children().toSeq.collect {
      case IsFile(f) if pred(f) => Seq(qualified(f.name.take(f.name.length - ".class".length))) 
      case IsDirectory(d) => findSpecsIn(d, qualified(d.name), pred)
    }.flatten
  }
  
  def runWithClassLoader[T](classLoader: ClassLoader, log: Log)(threadName: String, body: => T): Boolean = {
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

  def logFailingSpecs(failures: Seq[String], log: Log) =
    if (!failures.isEmpty) log.info(failures.mkString("\n\nSpecs Failing or In Error:\n", "\n", "\n\n"))

  // Wrappers for specs2 runners that invoke relevant methods via reflection. This approach allows the plug-in to not depend on 
  // specific version of specs2 and use whatever version has been specified as the project's dependency -- as long, as it provides
  // runners with signatures expected by this plug-in.
    
  class TestInterfaceRunner(classLoader: ClassLoader, log: Log) {
    val RunnerClassName = "org.specs2.runner.TestInterfaceRunner"
    val runnerClass = classLoader.loadClass(RunnerClassName)
    val runner = runnerClass.getConstructor(classOf[ClassLoader], classOf[Array[Logger]]).newInstance(classLoader, Array(new DebugLevelLogger(log)))
    val runSpecificationMethod = runnerClass.getMethod("runSpecification", classOf[String], classOf[EventHandler], classOf[Array[String]]) 
    
    def runSpecification(spec: String, handler: EventHandler, modes: Array[String]) = 
      runSpecificationMethod.invoke(runner, spec, handler, modes)
  }
  
  class HtmlRunner(classLoader: ClassLoader) {
    val RunnerClassName = "org.specs2.runner.HtmlRunner"
    val runnerClass = classLoader.loadClass(RunnerClassName)
    val runner = runnerClass.getConstructor().newInstance()
    val startMethod = runnerClass.getMethod("start", classOf[Seq[String]])
    
    def start(spec: String) = startMethod.invoke(runner, Seq(spec))
  }
}