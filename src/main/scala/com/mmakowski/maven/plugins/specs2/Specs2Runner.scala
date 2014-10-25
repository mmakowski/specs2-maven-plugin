package com.mmakowski.maven.plugins.specs2

import java.io.File
import java.net.{URL, URLClassLoader}

import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.scalatools.testing.{Event, EventHandler, Logger, Result}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scalax.file.Path
import scalax.file.PathMatcher._

/**
 * Executes pre-compiled specifications found in test classes directory. 
 * 
 * @author Maciek Makowski
 * @since 0.1.0
 */
class Specs2Runner(args: String) {
  /**
   * A handler that counts the total for each possible result of test when running a Spec
   */
  private class AggregatingEventHandler extends EventHandler {
    val testCounts: mutable.Map[String, Int] = mutable.Map() withDefaultValue 0
    
    def handle(event: Event) {
      val resultType = event.result.toString
      testCounts(resultType) = testCounts(resultType) + 1  
    }
    
    def report = Result.values.map(s => s"$s: ${testCounts(s.toString)}").mkString(", ")

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
    val classpath = urlsOf(project.getTestClasspathElements.asInstanceOf[java.util.List[String]].asScala)
    log.debug("test classpath: " + classpath)
    val fullSuffix = s"$suffix.class"
    log.debug(s"searching for specs ending in $fullSuffix")
    val testClassesPath = Path.fromString(project.getBuild.getTestOutputDirectory)
    val specs = findSpecsIn(testClassesPath, "", _.name.endsWith(fullSuffix))
    val classLoader = new URLClassLoader(classpath.toArray[URL], getClass.getClassLoader)
    val runWithTestClassLoader = runWithClassLoader(classLoader, log)_
    
    val runner = new TestInterfaceRunner(classLoader, log)

    def runSpec(failingSpecs: Seq[String], spec: String): Seq[String] = {
      log.info(spec + ":")
      val eventHandler = new AggregatingEventHandler
      val runCompleted = runWithTestClassLoader("spec runner", runner.runSpecification(spec, eventHandler, args.split(" ")))
      log.info(eventHandler.report)

      val result = runCompleted && eventHandler.noErrorsOrFailures
      if (!result) failingSpecs :+ spec else failingSpecs
    }
    
    def generateIndex(): Boolean = {
      val indices = findSpecsIn(testClassesPath, "", _.name == "index.class")
      if (indices.isEmpty) true else {
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
  
  private def url(file: File) = new URL(file.getAbsoluteFile.toURI.toASCIIString)

  private def urlsOf(files: Seq[String]) = files.map(new File(_)).map(url)

  private def findSpecsIn(dir: Path, pkg: String, pred: Path => Boolean): Seq[String] = {
    def qualified(name: String) = if (pkg.isEmpty) name else pkg + "." + name
    dir.children().toSeq.collect {
      case IsFile(f) if pred(f) => Seq(qualified(f.name.take(f.name.length - ".class".length))) 
      case IsDirectory(d) => findSpecsIn(d, qualified(d.name), pred)
    }.flatten
  }
  
  private def runWithClassLoader[T](classLoader: ClassLoader, log: Log)(threadName: String, body: => T): Boolean = {
    object runner extends Runnable {
      var success = false
      def run() = {
        try {
          body
          success = true
        } catch {
          case e: Exception => log.error(e)
        }
      }
    }
    val t = new Thread(runner, threadName)
    t.setContextClassLoader(classLoader)
    t.start()
    t.join()
    runner.success
  }

  private def logFailingSpecs(failures: Seq[String], log: Log) =
    if (failures.nonEmpty) log.info(failures.mkString("\n\nSpecs Failing or In Error:\n", "\n", "\n\n"))

  // Wrappers for specs2 runners that invoke relevant methods via reflection. This approach allows the plug-in to not depend on 
  // specific version of specs2 and use whatever version has been specified as the project's dependency -- as long, as it provides
  // runners with signatures expected by this plug-in.
    
  private class TestInterfaceRunner(classLoader: ClassLoader, log: Log) {
    val RunnerClassName = "org.specs2.runner.TestInterfaceRunner"
    val runnerClass = classLoader.loadClass(RunnerClassName)
    val runner = runnerClass.getConstructor(classOf[ClassLoader], classOf[Array[Logger]]).newInstance(classLoader, Array(new DebugLevelLogger(log)))
    val runSpecificationMethod = runnerClass.getMethod("runSpecification", classOf[String], classOf[EventHandler], classOf[Array[String]]) 
    
    def runSpecification(spec: String, handler: EventHandler, modes: Array[String]) = 
      runSpecificationMethod.invoke(runner, spec, handler, modes)
  }
  
  private class HtmlRunner(classLoader: ClassLoader) {
    val RunnerClassName = "org.specs2.runner.HtmlRunner"
    val runnerClass = classLoader.loadClass(RunnerClassName)
    val runner = runnerClass.getConstructor().newInstance()
    val startMethod = runnerClass.getMethod("start", classOf[Seq[String]])
    
    def start(spec: String) = startMethod.invoke(runner, Seq(spec))
  }
}