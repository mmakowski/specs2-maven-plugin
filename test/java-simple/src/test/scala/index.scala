import org.specs2._
import org.specs2.runner.SpecificationsFinder._
import runner.SpecificationsFinder._

class index extends Specification { def is =
  examplesLinks("Test specifications")

  def examplesLinks(t: String) =
    t.title ^ specifications().map(s => link(s))
}
