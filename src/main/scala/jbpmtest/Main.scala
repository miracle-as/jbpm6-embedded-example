package jbpmtest

import org.kie.internal.runtime.manager.context.EmptyContext
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.process.ProcessInstance
import scala.collection.JavaConversions._
import org.kie.api.task.model.TaskSummary
import org.kie.api.event.process.{ProcessCompletedEvent, DefaultProcessEventListener}

object Main extends App with KieSupport {

  println("run")

  Kie.startUp()
  val manager = Kie.getRuntimeManager("bpmn/test.bpmn.xml")
  val runtime = manager.getRuntimeEngine(EmptyContext.get())
  val ksession: KieSession = runtime.getKieSession()
  ksession.addEventListener(new DefaultProcessEventListener{
    override def afterProcessCompleted(event: ProcessCompletedEvent){
      println("completed "+event.getProcessInstance)
      val rt = event.getKieRuntime

    }
  })
  val params = Map("userId" -> "testparma")


  val pi: ProcessInstance = ksession.startProcess("com.sample.HelloWorld", params)
  println(s"run $pi")
  ksession.insert()

  println(s"task state ${pi.getStateName}")
  def varUserId(pi:ProcessInstance) = pi.getVariable("userId")
  println(s"task state ${varUserId(pi)}")

  val pi2 = Option(ksession.getProcessInstance(pi.getId))
  println("new state " + pi2.map(_.getStateName).getOrElse("#null#"))

  if (pi2.isDefined){
    println(s"task state ended ${varUserId(pi2.get)}")
  }

  val taskService = runtime.getTaskService()

  /*  val tasks: List[Task] = taskService.getTasksByProcessInstanceId(pi.getId).map {pid => taskService.getTaskById(pid)}.toList
    tasks.map {
      task =>
        println("task " + task.getNames().mkString + "(" + task.getId() + ": " + task.getDescriptions().mkString + ")")
      println(" task initiator "+task.getPeopleAssignments.getTaskInitiator)
      println(" task assignables "+task.getPeopleAssignments.getPotentialOwners.mkString)
        taskService.start(task.getId(), "sales-rep")
        taskService.complete(task.getId(), "sales-rep", null)
    }*/
  val tasks: List[TaskSummary] = taskService.getTasksAssignedAsPotentialOwner("sales-rep", "en-UK").toList
  tasks.map {
    task =>
      println("task " + task.getName() + "(" + task.getId() + ": " + task.getDescription + ")")
      //      println(" task initiator " + task.getPotentialOwners.mkString)
      taskService.start(task.getId(), "sales-rep")
      taskService.complete(task.getId(), "sales-rep", null)
    val fields = Map("x"->List[String]("y"))
//    val tsks : List[TaskSummary] = taskService.getTasksByVariousFields(fields, true)
  }

  val pi3 = Option(ksession.getProcessInstance(pi.getId))
  println("new state " + pi3.map(_.getStateName).getOrElse("#null#"))

  if (pi3.isDefined){
    println(s"task state ended ${varUserId(pi3.get)}")
  }

  manager.disposeRuntimeEngine(runtime)
  
  System.exit(0)

}
