package com.mmakowski.scratch

import org.specs2._
import org.junit.runner._
import runner._

@RunWith(classOf[JUnitRunner])
class ClassWithCustomSuff extends Specification { def is = s2"""
  Runs specifications with custom suffix: $success
  """
}
