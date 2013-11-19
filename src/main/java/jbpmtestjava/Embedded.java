package jbpmtestjava;

import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Comment;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.task.api.UserGroupCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Embedded {
  public static void main(String[] args) {
    System.out.println("Starting jBPM 6");

    try {
      RuntimeManager manager = getRuntimeManager("humantask/HumanTask.bpmn");
      RuntimeEngine runtime = manager.getRuntimeEngine(EmptyContext.get());
      KieSession ksession = runtime.getKieSession();

      // start a new process instance
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("userId", "krisv");
      params.put("description", "Need a new laptop computer");
      ProcessInstance processInstance = ksession.startProcess("com.sample.humantask", params);

      // "sales-rep" reviews request
      TaskService taskService = runtime.getTaskService();
      TaskSummary task1 = taskService.getTasksAssignedAsPotentialOwner("sales-rep", "en-UK").get(0);
      System.out.println("Sales-rep executing task " + task1.getName() + "(" + task1.getId() + ": " + task1.getDescription() + ")");
      taskService.claim(task1.getId(), "sales-rep");
      taskService.start(task1.getId(), "sales-rep");
      Map<String, Object> results = new HashMap<String, Object>();
      results.put("comment", "Agreed, existing laptop needs replacing xti");
      results.put("outcome", "Accept");
      taskService.complete(task1.getId(), "sales-rep", results);

      // "krisv" approves result
      TaskSummary task2 = taskService.getTasksAssignedAsPotentialOwner("krisv", "en-UK").get(0);
      System.out.println("krisv executing task " + task2.getName() + "(" + task2.getId() + ": " + task2.getDescription() + ")");
      taskService.start(task2.getId(), "krisv");
      results = new HashMap<String, Object>();
      results.put("outcome", "Agree");
      taskService.complete(task2.getId(), "krisv", results);

      // "john" as manager reviews request
      TaskSummary task3 = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK").get(0);
      System.out.println("john executing task " + task3.getName() + "(" + task3.getId() + ": " + task3.getDescription() + ")");
      taskService.claim(task3.getId(), "john");
      taskService.start(task3.getId(), "john");
      results = new HashMap<String, Object>();
      results.put("outcome", "Agree");
      taskService.complete(task3.getId(), "john", results);

      Object obj = getVariable(ksession.getProcessInstance(processInstance.getId()), "userId");
      System.out.println(" - process variable 'userId' " + obj);

      Task taskById = taskService.getTaskById(task3.getId());
      List<Comment> list = taskById.getTaskData().getComments();
      for (Comment comment : list) {
        System.out.println(" - task user comment " + comment);
      }

      if (false) { // NOTE: This last step is currently not working...
        // "sales-rep" gets notification

        TaskSummary task4 = taskService.getTasksAssignedAsPotentialOwner("sales-rep", "en-UK").get(0);
        System.out.println("sales-rep executing task " + task4.getName() + "(" + task4.getId() + ": " + task4.getDescription() + ")");
        taskService.start(task4.getId(), "sales-rep");
        Task task = taskService.getTaskById(task4.getId());
        Content content = taskService.getContentById(task.getTaskData().getDocumentContentId());
        Object result = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        Map<?, ?> map = (Map<?, ?>) result;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        taskService.complete(task4.getId(), "sales-rep", null);
      }


      manager.disposeRuntimeEngine(runtime);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    System.exit(0);
  }

  private static Object getVariable(ProcessInstance processInstance, String name) {
    if (processInstance == null || !(processInstance instanceof WorkflowProcessInstance)) {
      return null;
    }
    return ((WorkflowProcessInstance) processInstance).getVariable(name);
  }

  private static RuntimeManager getRuntimeManager(String process) {
    // load up the knowledge base
    JbpmHelper.startH2Server();
    JbpmHelper.setupDataSource();
    Properties properties = new Properties();
    properties.setProperty("krisv", "");
    properties.setProperty("sales-rep", "sales");
    properties.setProperty("john", "PM");
    UserGroupCallback userGroupCallback = new JBossUserGroupCallbackImpl(properties);
    RuntimeEnvironment environment = RuntimeEnvironmentBuilder.getDefault()
            .userGroupCallback(userGroupCallback)
            .addAsset(ResourceFactory.newClassPathResource(process), ResourceType.BPMN2)
            .get();
    return RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(environment);
  }
}