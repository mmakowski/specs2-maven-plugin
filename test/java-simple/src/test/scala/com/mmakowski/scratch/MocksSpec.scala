package com.mmakowski.scratch

import org.specs2._
import org.junit.runner._
import runner._
import org.specs2.mock._

@RunWith(classOf[JUnitRunner])
class MockSpec extends Specification with Mockito { def is =
  "Mockito spec"                       ^
                                      p^
  "Mocks:"                             ^
    "should work"          ! mockWorks ^
                                     end
    
  def reference = {
    val ref = mock[Appy]
    ref.scared returns true
    ref
  }
  def mockWorks = (new Appy).scared === reference.scared
  
}