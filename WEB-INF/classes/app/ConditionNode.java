package app;
import java.util.*;

public class ConditionNode{
	int id;
	String field;
	String pattern;
	String operator;
	String logicOp;
	ArrayList<ConditionNode> children=new ArrayList<>();
	
	public boolean isGroup(){
		return field==null || field.isEmpty();
	}
}