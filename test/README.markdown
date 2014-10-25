This directory contains projects that use specs2-maven-plugin in their build. The 
process I follow before releasing the plug-in is:

1. `install` the plug-in to local repo
2. run all tests, checking that the output is as expected
3. if everything looks okay, `deploy` to scala-tools.org.

Note that currently step 2. is not automated, i.e. the output has to be inspected by hand.
