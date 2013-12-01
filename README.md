jbpm6-embedded-example
======================

Trial run on JBPM 6.0.0.Final


Important: This is just playing with the API's

Default is now set up with a postgresql-database 'jbpmtest' on localhost. Should still work with H2 as well.


Builds with Apache Maven 3

    mvn clean install

Embedded sample app can be run with:

    mvn -e exec:java -Dexec.mainClass="jbpmtestjava.Embedded" -Dexec.classpathScope=compile

