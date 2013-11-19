package jbpmtest

import org.kie.api.runtime.process.{WorkflowProcessInstance, ProcessInstance}


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

}
