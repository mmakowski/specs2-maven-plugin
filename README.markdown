Specs2 Maven Plug-in
====================

[![Build Status](https://travis-ci.org/mmakowski/specs2-maven-plugin.svg?branch=master)](https://travis-ci.org/mmakowski/specs2-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/com.mmakowski/specs2-maven-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/com.mmakowski/specs2-maven-plugin)

This plug-in executes [specs2](http://etorreborre.github.com/specs2/) specifications that have been previously 
compiled to the test classes directory. It assumes that all specification classes have names ending with specified
suffix (_Spec_ by default) and that all classes with such name are specifications.

Usage
-----

Add this to the build/plugins section of your pom:

    <plugin>
      <groupId>com.mmakowski</groupId>
      <artifactId>specs2-maven-plugin</artifactId>
      <version>0.4.4</version>
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
  
  1. finds all classes whose name ends with specified suffix in the test classes directory (typically `target/test-classes`) 
     and attempts to execute each, generating a JUnit XML and HTML reports;
  2. finds a class called `index` in the test classes directory and attempts to generate a HTML report from it.

Limitations
-----------

At the moment each version of the plug-in works with a specific version of Scala only:

<table>
<thead>
  <tr><th>scala version</th><th>recommended plugin version</th></tr>
</thead>
<tbody>
  <tr><td>2.9.1         </td><td>0.3.0                     </td></tr>
  <tr><td>2.10          </td><td>0.4.2                     </td></tr>
  <tr><td>2.11	        </td><td>0.4.4                     </td></tr>
</tbody>
</table>

Credits
-------

The plug-in is maintained by [Maciek Makowski](https://github.com/mmakowski) with contributions from:

* [Emrys Ingersoll](https://github.com/wemrysi)
* [Rafał Krzewski](https://github.com/rkrzewski)
* [Taylor Leese](https://github.com/taylorleese)
* [Adam Retter](https://github.com/adamretter)
* [Eric Torreborre](https://github.com/etorreborre)
* [Jordan West](https://github.com/jrwest)
