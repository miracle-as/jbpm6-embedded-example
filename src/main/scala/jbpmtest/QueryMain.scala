package jbpmtest

import javax.persistence.TypedQuery
import java.lang
import scala.collection.JavaConversions._

object QueryMain extends App with KieSupport {


  println("run")

  Kie.setupDataSource

  val (taskService, emf) = Kie.startTaskServiceWithEmf

  val pid = 2l

  if (false) {
    // API query:
    val taskIds = taskService.getTasksByProcessInstanceId(pid).toList
    val taskId = taskIds.head
    val task = taskService.getTaskById(taskId)
    println("task " + task)
  }
  val em = emf.createEntityManager()
  if (false) {
    // simple reuse
    val namedQuery: TypedQuery[lang.Long] = em.createNamedQuery("TasksByProcessInstanceId", classOf[lang.Long])
    namedQuery.setParameter("processInstanceId", pid)
    namedQuery.setFirstResult(0)
    namedQuery.setMaxResults(5)
    val results = namedQuery.getResultList.toList
    results.map {r => println("task id " + r)}

  }

  // changing sort order - unclear why every named query on Taskorm.xml has "order by t.id"...

  val queryString = extractNamedQueryString(em,"TasksByProcessInstanceId")
  val withoutOrder = chopOrder(queryString)
  // Note:
  val withNewOrder = withoutOrder + " order by t.priority"
  val namedQuery: TypedQuery[lang.Long] = em.createQuery(withNewOrder, classOf[lang.Long])
  namedQuery.setParameter("processInstanceId", pid)
  namedQuery.setFirstResult(0)
  namedQuery.setMaxResults(5)
  println("results : ")
  val results = namedQuery.getResultList.toList
  results.map {r => println("task id " + r)}

}
