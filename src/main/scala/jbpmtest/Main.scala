package jbpmtest

import org.kie.internal.runtime.manager.context.EmptyContext
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.process.{WorkflowProcessInstance, ProcessInstance}
import scala.collection.JavaConversions._
import org.kie.api.task.model._
import scala.Some

object Main extends App with KieSupport {


  println("run")

  Kie.startUp()
  val manager = Kie.getRuntimeManager(Some("bpmn/New Process.bpmn2"))
  //  val manager = Kie.getRuntimeManager(Some("bpmn/test.bpmn.xml"))
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

  if (false) ksession.setGlobal("globalHelper", new Helper("yyyyMMddHHmmss'Z'"))
  //  runtime.setVariable("helper", Helper)

  val params = Map[String, AnyRef]("userId" -> "testparma",
    "localVar1" -> "john",
    "localVar2" -> "other value ",
    "processVar1" -> "pv 1",
    "processVar2" -> "pv 2",
    "someDate" -> new java.util.Date(),
    "someDateFormatted" -> "SDF-input",
    "helper" -> Helper)

  //  var pi: ProcessInstance = ksession.getProcessInstance(1)
  println("starting process ")

  var pi: ProcessInstance = ksession.startProcess("defaultPackage.New_Process", params)
  //  var pi: ProcessInstance = ksession.startProcess("com.sample.HelloWorld", params)
  println(s"run $pi")
  //  }
  val map = dumpMap(listVariables(pi.getId))
  println(s"\nvariable $map")


  println(s"\ntask state ${pi.getStateName}")

  def varUserId(pi: ProcessInstance)(varName: String) = pi.asInstanceOf[WorkflowProcessInstance].getVariable(varName)

  val getvar = varUserId(pi) _

  println(s"task state ${getvar("someDateFormatted")}")
  println(s"task state ${getvar("someDate")}")
  // println(s" descr ${task)
  implicit val taskService = runtime.getTaskService()

  import Status._

  val lst = List(Created, Ready, Reserved, InProgress, Suspended, Completed, Failed, Error, Exited, Obsolete)

  //    val taskSummaries = taskService.getTasksByStatusByProcessInstanceId(pi.getId, lst, "en-UK")
  val taskSummaries = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK")
  println("\ntask summaries " + taskSummaries.size)
  println(dumpTaskSummaries(taskSummaries.toList))


  val summary = taskSummaries.head
  println("\nowners ? " + Option(summary.getPotentialOwners).map(_.map(o => Option(o)).mkString))
  val headSummaryId = summary.getId

  val tasksForPi = taskService.getTasksByProcessInstanceId(pi.getId)

  val head: Long = tasksForPi.head

  println(s"$headSummaryId==$head ?")

  val task: Task = taskService.getTaskById(head)
  val data: TaskData = task.getTaskData

  val result = loadContent(task)
  println("\ncontent: " + dumpAny(result))

  println("\nstarting task")
  taskService.start(headSummaryId, "john")

  println("\ncompleting task")
  val completeMap = Map[String, AnyRef]("output1" -> "output1 set on complete", "output2" -> "output2 set on complete")
  taskService.complete(headSummaryId, "john", completeMap)


  val map3 = dumpMap(listVariables(pi.getId))
  println(s"\nvariables post complete  $map3")

  val result2 = loadContent(task)
  println("\ncontent complete: " + dumpAny(result2))


  if (false) {
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
