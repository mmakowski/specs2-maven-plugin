package com.mmakowski.scratch

import org.specs2._
import org.junit.runner._
import runner._
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext

@RunWith(classOf[JUnitRunner])
class LoadingSpringContextFromExternalJarSpec extends Specification { def is = s2"""
  Spring context from external jar
    is loaded succesfully"  $loadContext
"""
    
  def loadContext = {
    def ctx = new ClassPathXmlApplicationContext("spring-beans.xml")
    ctx.close()
    success
  }
}