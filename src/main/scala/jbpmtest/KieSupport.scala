package jbpmtest

import org.kie.api.runtime.process.{WorkflowProcessInstance, ProcessInstance}
import org.kie.api.runtime.KieSession
import org.kie.internal.command.Context
import org.drools.core.command.impl.{KnowledgeCommandContext, GenericCommand}
import scala.Predef.String
import scala.collection.JavaConversions._
import org.jbpm.process.core.context.variable.VariableScope
import org.jbpm.process.instance.context.variable.VariableScopeInstance
import javax.transaction.UserTransaction
import java.util.Hashtable
import javax.naming.InitialContext
import java.util
import org.kie.api.task.model.{Task, Content, TaskSummary, I18NText}
import bitronix.tm.{BitronixTransactionManager, TransactionManagerServices}
import org.jbpm.services.task.utils.ContentMarshallerHelper
import org.kie.api.task.TaskService


trait KieSupport {

  val stopped = List(ProcessInstance.STATE_ABORTED, ProcessInstance.STATE_SUSPENDED, ProcessInstance.STATE_COMPLETED)


  implicit def processInstanceCompanions(processInstance: ProcessInstance) = new ProcessInstanceSupportWrapper(processInstance)

  class ProcessInstanceSupportWrapper(val processInstance: ProcessInstance) {
    def isStopped: Boolean = stopped.contains(processInstance.getState)

    def getStateName: String = processInstance.getState match {
      case ProcessInstance.STATE_ABORTED => "Aborted"
      case ProcessInstance.STATE_ACTIVE => "Active"
      case ProcessInstance.STATE_COMPLETED => "Completed"
      case ProcessInstance.STATE_PENDING => "Pending"
      case ProcessInstance.STATE_SUSPENDED => "Suspended"
      case _ => "Unknown"
    }

    def getVariable(name: String): Option[Any] = processInstance match {
      case wpi: WorkflowProcessInstance => Option(wpi.getVariable(name))
      case _ => None
    }
  }

  def listVariables(piId: Long)(implicit ksession: KieSession): scala.collection.mutable.Map[String, Any] = {
    val res = ksession.execute(new GenericCommand[java.util.Map[String, Object]]() {

      override def execute(context: Context): java.util.Map[String, Object] = {
        val kcontext = context.asInstanceOf[KnowledgeCommandContext]
        val ksession = kcontext.getKieSession

        val processInstance = ksession.getProcessInstance(piId).asInstanceOf[org.jbpm.process.instance.ProcessInstance]

        val variableScope = processInstance.getContextInstance(VariableScope.VARIABLE_SCOPE).asInstanceOf[VariableScopeInstance]

        val variables = variableScope.getVariables

        variables

      }

    })
    collection.mutable.Map() ++ res.toMap
  }

  def withTx(block: => Unit) {
    //    val txm = transactionManager
    val txm = ctx.lookup("java:comp/UserTransaction").asInstanceOf[UserTransaction];
    try {
      txm.begin()
      block
      txm.commit()
    } catch {
      case ex: Exception =>
        txm.rollback()
        throw ex

    }
  }


  def transactionManager: BitronixTransactionManager = {
    TransactionManagerServices.getTransactionManager
  }

  def ctx = {
    val env = new Hashtable[String, String]
    env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "bitronix.tm.jndi.BitronixInitialContextFactory")
    val ctx = new InitialContext(env)
    ctx
  }

  def loadContent(task: Task)(implicit taskService: TaskService): AnyRef = {
    val content: Content = taskService.getContentById(task.getTaskData.getDocumentContentId)
    ContentMarshallerHelper.unmarshall(content.getContent, null)
  }

  def dump(list: util.List[I18NText]): String = list.map {i18ntext => i18ntext.getText}.mkString

  def dumpAny(any: AnyRef): String = any match {
    case map: java.util.Map[_, _] => "map: " + dumpMap(map)
    case _ => s"any (#{any.getClass}): #{any}"
  }


  def dumpMap(map:java.util.Map[_,_]): String = {
    map.mkString("[", ",\n", "]")
  }

  def dumpTaskSummary(taskSummary: TaskSummary): String = s"""{TaskSummary name=${taskSummary.getName} - description=${taskSummary.getDescription} - subject=${taskSummary.getSubject} }"""

  def dumpTaskSummaries(list: List[TaskSummary]): String = list.map(dumpTaskSummary).mkString
}
