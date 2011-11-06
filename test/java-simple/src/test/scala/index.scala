import org.specs2._
import runner.FilesRunner._

class index extends Specification { def is =
  examplesLinks("Test specifications")

  def examplesLinks(t: String) = specifications().foldLeft(t.title) { (res, cur) => res ^ see(cur) }
}