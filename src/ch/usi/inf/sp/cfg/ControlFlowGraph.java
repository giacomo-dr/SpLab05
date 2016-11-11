package ch.usi.inf.sp.cfg;

import java.util.Iterator;
import java.util.List;

/**
 * This class can represent a Control Flow Graph of a java method
 * as well as the Dominance Tree of it.
 * 
 * @author Eric Botter, Lorenzo Ferretti, Giacomo Del Rio
 */
public class ControlFlowGraph {
	private String className;
	private String methodName;
	private List<BasicBlock> bbList;
	
	public ControlFlowGraph( String className, String methodName, List<BasicBlock> bbList ){
		this.methodName = methodName;
		this.className = className;
		this.bbList = bbList;
	}
	
	public String getClassName(){
		return className;
	}
	
	public String getMethodName(){
		return methodName;
	}
	
	public Iterator<BasicBlock> getBasicBlocks(){
		return bbList.iterator();
	}
	
	public int getBasicBlockCount(){
		return bbList.size();
	} 
}
