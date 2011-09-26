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
      <version>1.0.0</version>
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

now the verify phase will execute all specifications in your project and will generate HTML reports (in 
`target/specs2-reports`) and JUnit XML reports (in `target/test-reports`).

