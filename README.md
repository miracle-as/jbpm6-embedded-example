jbpm6-embedded-example
======================

Trial run on JBPM6 CR5 


Builds with Apache Maven 3

    mvn clean install

Embedded sample app can be run with (starts java main, running data on h2-db-instance):

    mvn -e exec:java -Dexec.mainClass="jbpmtestjava.Embedded" -Dexec.classpathScope=compile

