package jbpmtest

import org.kie.api.runtime.manager.RuntimeManager
import org.kie.internal.runtime.manager.context.EmptyContext
import org.kie.internal.io.ResourceFactory
import org.kie.api.io.ResourceType
import org.kie.internal.runtime.manager.{RuntimeEnvironment, RuntimeManagerFactory}
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder
import org.kie.api.task.TaskService
import javax.persistence.{Persistence, EntityManagerFactory}
import org.jbpm.services.task.HumanTaskConfigurator
import org.jbpm.shared.services.impl.JbpmJTATransactionManager
import org.kie.internal.runtime.StatefulKnowledgeSession
import java.util.Properties
import java.io.{FilenameFilter, File}
import org.kie.internal.KnowledgeBase
import org.h2.tools.Server

import org.kie.api.runtime.EnvironmentName
import bitronix.tm.TransactionManagerServices
import bitronix.tm.resource.jdbc.PoolingDataSource
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl

object Kie {

  def getRuntimeManager(process: String): RuntimeManager = {


    val properties = new Properties()
    properties.setProperty("krisv", "")
    properties.setProperty("sales-rep", "sales")
    properties.setProperty("john", "PM")
    val userGroupCallback = new JBossUserGroupCallbackImpl(properties)
    val builder = RuntimeEnvironmentBuilder.getDefault
    builder.userGroupCallback(userGroupCallback)
    builder.addAsset(ResourceFactory.newClassPathResource(process), ResourceType.BPMN2)
    val environment: RuntimeEnvironment = builder.get()

    RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(environment)
  }

  val processStateName = List("PENDING", "ACTIVE", "COMPLETED", "ABORTED", "SUSPENDED")
  val txStateName = List("ACTIVE",
    "MARKED_ROLLBACK",
    "PREPARED",
    "COMMITTED",
    "ROLLEDBACK",
    "UNKNOWN",
    "NO_TRANSACTION",
    "PREPARING",
    "COMMITTING",
    "ROLLING_BACK")

  def startUp() {
    cleanupSingletonSessionId()
    val properties = getProperties
    val driverClassName = properties.getProperty("persistence.datasource.driverClassName", "org.h2.Driver")
    if (driverClassName.startsWith("org.h2")) {
      startH2Server()
    }
    val persistenceEnabled = properties.getProperty("persistence.enabled", "false")
    val humanTaskEnabled = properties.getProperty("taskservice.enabled", "false")
    if ("true".equals(persistenceEnabled) || "true".equals(humanTaskEnabled)) {
      setupDataSource
    }
    if ("true".equals(humanTaskEnabled)) {
      startTaskService
    }
  }

  def startH2Server() = {
    val server = Server.createTcpServer()
    server.start()
    server
  }

  def setupDataSource: PoolingDataSource = {
    val properties = getProperties
    // create data source
    val pds = new PoolingDataSource()
    pds.setUniqueName(properties.getProperty("persistence.datasource.name", "jdbc/jbpm-ds"))
    pds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource")
    pds.setMaxPoolSize(5)
    pds.setAllowLocalTransactions(true)
    pds.getDriverProperties.put("user", properties.getProperty("persistence.datasource.user", "sa"))
    pds.getDriverProperties.put("password", properties.getProperty("persistence.datasource.password", ""))
    pds.getDriverProperties.put("url", properties.getProperty("persistence.datasource.url", "jdbc:h2:tcp://localhost/~/jbpm-dbMVCC=TRUE"))
    pds.getDriverProperties.put("driverClassName", properties.getProperty("persistence.datasource.driverClassName", "org.h2.Driver"))
    pds.init()
    pds
  }

  def startTaskService: TaskService = {
    val properties = getProperties
    val dialect = properties.getProperty("persistence.persistenceunit.dialect", "org.hibernate.dialect.H2Dialect")
    val map = new java.util.HashMap[String, String]()
    map.put("hibernate.dialect", dialect)
    val property = properties.getProperty("persistence.persistenceunit.name", "org.jbpm.persistence.jpa")
    val emf: EntityManagerFactory = Persistence.createEntityManagerFactory(property, map)
    System.setProperty("jbpm.user.group.mapping", properties.getProperty("taskservice.usergroupmapping", "classpath:/usergroups.properties"))

    val taskService = new HumanTaskConfigurator()
      .entityManagerFactory(emf)
      //      .userGroupCallback(getUserGroupCallback())
      .transactionManager(new JbpmJTATransactionManager())
      .getTaskService()
    taskService
  }

  def registerTaskService(ksession: StatefulKnowledgeSession) {
    // no-op HT work item handler is already registered when using RuntimeManager

  }

  def newStatefulKnowledgeSession(kbase: KnowledgeBase): StatefulKnowledgeSession = {
    loadStatefulKnowledgeSession(kbase, -1)
  }

  def loadStatefulKnowledgeSession(kbase: KnowledgeBase, sessionId: Int): StatefulKnowledgeSession = {
    val properties = getProperties
    val persistenceEnabled = properties.getProperty("persistence.enabled", "false")
    var builder: RuntimeEnvironmentBuilder = null
    if ("true".equals(persistenceEnabled)) {
      val dialect = properties.getProperty("persistence.persistenceunit.dialect", "org.hibernate.dialect.H2Dialect")
      val map = new java.util.HashMap[String, String]()
      map.put("hibernate.dialect", dialect)
      val emf: EntityManagerFactory = Persistence.createEntityManagerFactory(properties.getProperty("persistence.persistenceunit.name", "org.jbpm.persistence.jpa"), map)


      builder = RuntimeEnvironmentBuilder.getDefault()
        .entityManagerFactory(emf)
        .addEnvironmentEntry(EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager())


    } else {
      builder = RuntimeEnvironmentBuilder.getDefaultInMemory()
    }
    builder.knowledgeBase(kbase)
    val manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get())
    return manager.getRuntimeEngine(EmptyContext.get()).getKieSession().asInstanceOf[StatefulKnowledgeSession]
  }

  /*
    @SuppressWarnings("unchecked")
    def getUserGroupCallback: UserGroupCallback = {
      val properties = getProperties
      val className = properties.getProperty("taskservice.usergroupcallback")
      if (className != null) {
        try {
          val clazz: Class[UserGroupCallback] = classOf[className].asInstanceOf[Class[UserGroupCallback]]

          return clazz.newInstance()
        } catch (Exception e) {
          throw new IllegalArgumentException("Cannot create instance of UserGroupCallback " + className, e)
        }
      } else {
        return new JBossUserGroupCallbackImpl("classpath:/usergroups.properties")
      }
    }
  */

  def getProperties: Properties = {
    val properties = new Properties()
    properties.load(this.getClass.getResourceAsStream("/jBPM.properties"))
    properties
  }

  def cleanupSingletonSessionId() {
    val tempDir = new File(System.getProperty("java.io.tmpdir"))
    if (tempDir.exists()) {
      val jbpmSerFiles = tempDir.list(new FilenameFilter() {
        def accept(dir: File, name: String): Boolean = name.endsWith("-jbpmSessionId.ser")
      })
      jbpmSerFiles.foreach(file => new File(tempDir, file).delete())

    }
  }

}
