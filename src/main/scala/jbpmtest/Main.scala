package jbpmtest

import org.kie.internal.runtime.manager.context.EmptyContext
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.process.{WorkflowProcessInstance, ProcessInstance}
import scala.collection.JavaConversions._
import org.kie.api.task.model.Status

object Main extends App with KieSupport {


  println("run")

  Kie.startUp()
  val manager = Kie.getRuntimeManager(Some("bpmn/test.bpmn.xml"))
  val runtime = manager.getRuntimeEngine(EmptyContext.get())
  //  runtime.getTaskService.getTasksByProcessInstanceId()
  implicit val ksession: KieSession = runtime.getKieSession
  /*
      ksession.addEventListener(new DefaultProcessEventListener {
        override def afterProcessCompleted(event: ProcessCompletedEvent) {
          println("completed " + event.getProcessInstance)
          val rt = event.getKieRuntime

        }
      })
  */

    ksession.setGlobal("globalHelper", new Helper("yyyyMMddHHmmss'Z'"))
  //  runtime.setVariable("helper", Helper)

  val params = Map[String,AnyRef]("userId" -> "testparma",
    "someDate" -> new java.util.Date(),
    "someDateFormatted" -> "SDF-input",
    "helper" -> Helper)

  //  var pi: ProcessInstance = ksession.getProcessInstance(1)
  println("starting process ")

  var pi: ProcessInstance = ksession.startProcess("com.sample.HelloWorld", params)
  println(s"run $pi")
  //  }
  val map = listVariables(pi.getId)
  println(s"variable $map")


  println(s"task state ${pi.getStateName}")

  def varUserId(pi: ProcessInstance)(varName: String) = pi.asInstanceOf[WorkflowProcessInstance].getVariable(varName)

  val getvar = varUserId(pi) _

  println(s"task state ${getvar("someDateFormatted")}")
  println(s"task state ${getvar("someDate")}")
  // println(s" descr ${task)
  val taskService = runtime.getTaskService()

  import Status._

  val lst = List(Created, Ready, Reserved, InProgress, Suspended, Completed, Failed, Error, Exited, Obsolete)

//  val taskSummaries = taskService.getTasksByStatusByProcessInstanceId(pi.getId, lst, "US-en")
  val taskSummaries  = taskService.getTasksAssignedAsPotentialOwner("sales-rep", "en-UK")
  println("task summaries "+taskSummaries.size)
  println(dumpTaskSummaries(taskSummaries.toList))

  if (false) {

    val tasksForPi = taskService.getTasksByProcessInstanceId(pi.getId)

    /*
        private val head: Long = tasksForPi.head

        private val task: Task = taskService.getTaskById(head)

        println("task " + task.getNames().mkString + "(" + task.getId() + ": " + dump(task.getDescriptions()) + ")")
        println(" task initiator " + task.getPeopleAssignments.getTaskInitiator)
        println(" task assignables " + task.getPeopleAssignments.getPotentialOwners.mkString)
    */


    val pi2 = Option(ksession.getProcessInstance(pi.getId))
    println("new state " + pi2.map(_.getStateName).getOrElse("#null#"))

    if (pi2.isDefined) {
      println(s"task state ended ${varUserId(pi2.get)("userId")}")
    }


    val pi3 = Option(ksession.getProcessInstance(pi.getId))
    println("new state " + pi3.map(_.getStateName).getOrElse("#null#"))

    if (pi3.isDefined) {
      println(s"task state ended ${varUserId(pi3.get)("userId")}")
    }
  }


  manager.disposeRuntimeEngine(runtime)

  manager.close()


  System.exit(0)

}
