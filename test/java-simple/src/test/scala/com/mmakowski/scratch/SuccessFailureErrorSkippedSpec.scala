package com.mmakowski.scratch

import org.specs2._
import org.junit.runner._
import runner._

@RunWith(classOf[JUnitRunner])
class SuccessFailureErrorSkippedSpec extends Specification { def is =
  "Possible statuses"                       ^
    "Success"  ! success ^
    "Failure"  ! failure ^
    "Error"    ! error   ^
    "Skipped"  ! todo    ^
                       end
    
  def error: org.specs2.execute.Result = throw new RuntimeException
}