package jbpmtest

import org.junit.Test
import org.junit.Assert._

//import org.scalatest.Assertions._


class KieSupportTest {

  val sampleSqlWithOrder = """select distinct
                             |            new org.jbpm.services.task.query.TaskSummaryImpl(
                             |            t.id,
                             |            t.taskData.processInstanceId,
                             |            name.shortText,
                             |            subject.shortText,
                             |            description.shortText,
                             |            t.taskData.status,
                             |            t.priority,
                             |            t.taskData.skipable,
                             |            actualOwner,
                             |            createdBy,
                             |            t.taskData.createdOn,
                             |            t.taskData.activationTime,
                             |            t.taskData.expirationTime,
                             |            t.taskData.processId,
                             |            t.taskData.processSessionId,
                             |            t.subTaskStrategy,
                             |            t.taskData.parentId )
                             |            from
                             |            TaskImpl t
                             |            left join t.taskData.actualOwner as actualOwner
                             |            left join t.taskData.createdBy as createdBy
                             |            left join t.subjects as subject
                             |            left join t.descriptions as description
                             |            left join t.names as name
                             |            where
                             |            t.archived = 0 and
                             |            t.taskData.processInstanceId = :processInstanceId and
                             |            name.shortText = :taskName and
                             |            (
                             |            name.language = :language
                             |            or t.names.size = 0
                             |            ) and
                             |
                             |            (
                             |            subject.language = :language
                             |            or t.subjects.size = 0
                             |            ) and
                             |
                             |            (
                             |            description.language = :language
                             |            or t.descriptions.size = 0
                             |            ) and
                             |
                             |            t.taskData.status in (:status)
                             |            order by t.id DESC
                             |            """.stripMargin
  val sampleSqlWithoutOrder = """select distinct
                                |            new org.jbpm.services.task.query.TaskSummaryImpl(
                                |            t.id,
                                |            t.taskData.processInstanceId,
                                |            name.shortText,
                                |            subject.shortText,
                                |            description.shortText,
                                |            t.taskData.status,
                                |            t.priority,
                                |            t.taskData.skipable,
                                |            actualOwner,
                                |            createdBy,
                                |            t.taskData.createdOn,
                                |            t.taskData.activationTime,
                                |            t.taskData.expirationTime,
                                |            t.taskData.processId,
                                |            t.taskData.processSessionId,
                                |            t.subTaskStrategy,
                                |            t.taskData.parentId )
                                |            from
                                |            TaskImpl t
                                |            left join t.taskData.actualOwner as actualOwner
                                |            left join t.taskData.createdBy as createdBy
                                |            left join t.subjects as subject
                                |            left join t.descriptions as description
                                |            left join t.names as name
                                |            where
                                |            t.archived = 0 and
                                |            t.taskData.processInstanceId = :processInstanceId and
                                |            name.shortText = :taskName and
                                |            (
                                |            name.language = :language
                                |            or t.names.size = 0
                                |            ) and
                                |
                                |            (
                                |            subject.language = :language
                                |            or t.subjects.size = 0
                                |            ) and
                                |
                                |            (
                                |            description.language = :language
                                |            or t.descriptions.size = 0
                                |            ) and
                                |
                                |            t.taskData.status in (:status)""".stripMargin

  @Test
  def testChopOrder {
    val kieSupport = new KieSupport {}

    assertEquals(kieSupport.chopOrder(sampleSqlWithOrder).trim, sampleSqlWithoutOrder.trim)
    assertEquals(kieSupport.chopOrder(sampleSqlWithoutOrder).trim , sampleSqlWithoutOrder.trim)
  }
}
