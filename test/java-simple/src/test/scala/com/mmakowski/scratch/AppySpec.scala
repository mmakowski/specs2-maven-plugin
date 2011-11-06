package com.mmakowski.scratch

import org.specs2._
import org.junit.runner._
import runner._
import org.specs2.mock._

@RunWith(classOf[JUnitRunner])
class AppySpec extends Specification with Mockito { def is =
  "Appy specification"                 ^
                                      p^
  "Appy should"                        ^
    "be scared"               ! scared ^
    "know its name"           ! name   ^
                                     end
    
  def reference = {
    val ref = mock[Appy]
    ref.scared returns true
    ref
  }
  def scared = (new Appy).scared === reference.scared
  def name = (new Appy).name === "Appy"
  
}