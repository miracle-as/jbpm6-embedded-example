package jbpmtest

import java.text.SimpleDateFormat
import java.util.Date
import org.drools.core.spi.ProcessContext

class Helper(dateFormat: String) extends Serializable {

  def formatDate(anyDate: Any): String = new SimpleDateFormat(dateFormat).format(anyDate.asInstanceOf[Date])

  def format(date: Date): String = new SimpleDateFormat(dateFormat).format(date)

  def inspect(any:AnyRef): String = any match {
    case pc: ProcessContext =>
      s"""{
        | PI: ${pc.getProcessInstance}
        | PI: ${pc.getNodeInstance}
        | PI: ${pc.getKieRuntime}
        |}""".stripMargin
    case _ =>
      "inspected any "+any.getClass
  }
}

object Helper extends Helper("yyyy-MM-dd") with Serializable
