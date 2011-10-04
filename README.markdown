Maven Specs2 Plug-in
====================

This plug-in executes [specs2](http://etorreborre.github.com/specs2/) specifications that have been previously 
compiled to the test classes directory. It assumes that all specification classes have names ending with _Spec_ 
and that all classes with such name are specifications.

Usage
-----

Add this to the build/plugins section of your pom:

    <plugin>
      <groupId>com.mmakowski</groupId>
      <artifactId>maven-specs2-plugin</artifactId>
      <version>0.1.0</version>
      <executions>
        <execution>
          <id>verify</id>
          <phase>verify</phase>
          <goals>
            <goal>run-specs</goal>
          </goals>
        </execution>
      </executions>
    </plugin>

now the _verify_ phase of your maven build will execute all specifications in your project and will generate HTML 
reports (in `target/specs2-reports`) and JUnit XML reports (in `target/test-reports`).

Features
--------

When `run-specs` goal is executed, the plug-in:
  
  1. finds all classes whose name ends with _Spec_ in the test classes directory (typically `target/test-classes`) 
     and attempts to execute each, generating a JUnit XML and HTML reports;
  2. finds a class called `index` in the test classes directory and attempts to generate a HTML report from it.

Limitations
-----------

At them moment the plug-in has only been tested with specs2 1.6.1 and scala 2.9.1. It is likely that projects using
other versions will not work.
