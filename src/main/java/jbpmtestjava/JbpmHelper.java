package jbpmtestjava;


import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.h2.tools.Server;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.HumanTaskConfigurator;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.jbpm.shared.services.impl.JbpmJTATransactionManager;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.TaskService;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.task.api.UserGroupCallback;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Deprecated // Deprecated - se jbpm examples and docs for explanations...
public class JbpmHelper {

  BitronixTransactionManager transactionManager;

  public static void startUp() {
    cleanupSingletonSessionId();
    Properties properties = getProperties();
    String driverClassName = properties.getProperty("persistence.datasource.driverClassName", "org.h2.Driver");
    if (driverClassName.startsWith("org.h2")) {
      startH2Server();
    }
    String persistenceEnabled = properties.getProperty("persistence.enabled", "false");
    String humanTaskEnabled = properties.getProperty("taskservice.enabled", "false");
    if ("true".equals(persistenceEnabled) || "true".equals(humanTaskEnabled)) {
      setupDataSource();
    }
    if ("true".equals(humanTaskEnabled)) {
      startTaskService();
    }
  }

  public static Server startH2Server() {
    try {
      // start h2 in memory database
      Server server = Server.createTcpServer(new String[0]);
      server.start();
      return server;
    } catch (Throwable t) {
      throw new RuntimeException("Could not start H2 server", t);
    }
  }

  public static PoolingDataSource setupDataSource() {
    Properties properties = getProperties();
    // create data source
    PoolingDataSource pds = new PoolingDataSource();
    pds.setUniqueName(properties.getProperty("persistence.datasource.name", "jdbc/jbpm-ds"));
    pds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
    pds.setMaxPoolSize(5);
    pds.setAllowLocalTransactions(true);
    pds.getDriverProperties().put("user", properties.getProperty("persistence.datasource.user", "sa"));
    pds.getDriverProperties().put("password", properties.getProperty("persistence.datasource.password", ""));
    pds.getDriverProperties().put("url", properties.getProperty("persistence.datasource.url", "jdbc:h2:tcp://localhost/~/jbpm-db;MVCC=TRUE"));
    pds.getDriverProperties().put("driverClassName", properties.getProperty("persistence.datasource.driverClassName", "org.h2.Driver"));
    pds.init();
    return pds;
  }

  public static TaskService startTaskService() {
    Properties properties = getProperties();
    EntityManagerFactory emf = getEntityManagerFactory(properties);
    System.setProperty("jbpm.user.group.mapping", properties.getProperty("taskservice.usergroupmapping", "classpath:/usergroups.properties"));

    TaskService taskService = new HumanTaskConfigurator()
            .entityManagerFactory(emf)
            .userGroupCallback(getUserGroupCallback())
            .transactionManager(new JbpmJTATransactionManager())
            .getTaskService();
    return taskService;
  }

  public static EntityManagerFactory getEntityManagerFactory() {
    return getEntityManagerFactory(getProperties());
  }

  private static EntityManagerFactory getEntityManagerFactory(Properties properties) {
    String dialect = properties.getProperty("persistence.persistenceunit.dialect", "org.hibernate.dialect.H2Dialect");
    Map<String, String> map = new HashMap<String, String>();
    map.put("hibernate.dialect", dialect);
    return Persistence.createEntityManagerFactory(properties.getProperty("persistence.persistenceunit.name", "org.jbpm.persistence.jpa"), map);
//    return Persistence.createEntityManagerFactory(properties.getProperty("taskservice.datasource.name", "org.jbpm.services.task"), map);
  }

  public static void registerTaskService(StatefulKnowledgeSession ksession) {
    // no-op HT work item handler is already registered when using RuntimeManager

  }

  public StatefulKnowledgeSession newStatefulKnowledgeSession(KnowledgeBase kbase) {
    return loadStatefulKnowledgeSession(kbase, -1);
  }

  public StatefulKnowledgeSession loadStatefulKnowledgeSession(KnowledgeBase kbase, int sessionId) {
    Properties properties = getProperties();
    String persistenceEnabled = properties.getProperty("persistence.enabled", "false");
    RuntimeEnvironmentBuilder builder = null;
    if ("true".equals(persistenceEnabled)) {
      String dialect = properties.getProperty("persistence.persistenceunit.dialect", "org.hibernate.dialect.H2Dialect");
      Map<String, String> map = new HashMap<String, String>();
      map.put("hibernate.dialect", dialect);
      EntityManagerFactory emf = Persistence.createEntityManagerFactory(properties.getProperty("persistence.persistenceunit.name", "org.jbpm.persistence.jpa"), map);


      transactionManager = TransactionManagerServices.getTransactionManager();
      builder = RuntimeEnvironmentBuilder.getDefault()
              .entityManagerFactory(emf)
              .addEnvironmentEntry(EnvironmentName.TRANSACTION_MANAGER, transactionManager);


    } else {
      builder = RuntimeEnvironmentBuilder.getDefaultInMemory();
    }
    builder.knowledgeBase(kbase);
    RuntimeManager manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get());
    return (StatefulKnowledgeSession) manager.getRuntimeEngine(EmptyContext.get()).getKieSession();
  }

  @SuppressWarnings("unchecked")
  public static UserGroupCallback getUserGroupCallback() {
    Properties properties = getProperties();
    String className = properties.getProperty("taskservice.usergroupcallback");
    if (className != null) {
      try {
        Class<UserGroupCallback> clazz = (Class<UserGroupCallback>) Class.forName(className);

        return clazz.newInstance();
      } catch (Exception e) {
        throw new IllegalArgumentException("Cannot create instance of UserGroupCallback " + className, e);
      }
    } else {
      return new JBossUserGroupCallbackImpl("classpath:/usergroups.properties");
    }
  }

  public static Properties getProperties() {
    Properties properties = new Properties();
    try {
      properties.load(JbpmHelper.class.getResourceAsStream("/jBPM.properties"));
    } catch (Throwable t) {
      // do nothing, use defaults
    }
    return properties;
  }

  protected static void cleanupSingletonSessionId() {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    if (tempDir.exists()) {

      String[] jbpmSerFiles = tempDir.list(new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {

          return name.endsWith("-jbpmSessionId.ser");
        }
      });
      for (String file : jbpmSerFiles) {

        new File(tempDir, file).delete();
      }
    }
  }
}
