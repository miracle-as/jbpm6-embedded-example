package jbpmtest

import java.text.SimpleDateFormat
import java.util.Date

class Helper(dateFormat: String) extends Serializable {

  def formatDate(anyDate: Any): String = new SimpleDateFormat(dateFormat).format(anyDate.asInstanceOf[Date])

  def format(date: Date): String = new SimpleDateFormat(dateFormat).format(date)
}

object Helper extends Helper("yyyy-MM-dd") with Serializable
