package jbpmtest

import org.kie.internal.utils.KieHelper
import org.kie.internal.io.ResourceFactory

object Main2 extends App {

  val kieHelper = new KieHelper
  val kieBase = kieHelper.addResource(ResourceFactory.newClassPathResource("bpmn/test.bpmn.xml")).build()

  val ksession = kieBase.newKieSession()
  val processInstance = ksession.startProcess("com.sample.HelloWorld")


}
