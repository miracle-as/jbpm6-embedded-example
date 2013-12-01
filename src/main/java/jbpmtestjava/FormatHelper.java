package jbpmtestjava;

public class FormatHelper {
public static String format(Object date){
	return new java.text.SimpleDateFormat("dd-MM").format(date);
}
}
