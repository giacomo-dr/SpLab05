package ch.usi.inf.sp.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * A Node of the Control Flow Graph representation
 * 
 * @author Eric Botter, Lorenzo Ferretti, Giacomo Del Rio
 */
public class BasicBlock {

	private int bbAdrress;
	private List<AbstractInsnNode> instructions;
	private Map<BasicBlock,String> edges;
	
	public BasicBlock( int bbAdrress ){
		this.bbAdrress = bbAdrress;
		instructions = new ArrayList<>();
		edges = new HashMap<>();
	}
	
	public BasicBlock( BasicBlock bb2 ){
		this.bbAdrress = bb2.bbAdrress;
		instructions = bb2.instructions;
		edges = new HashMap<>();
	}
	
	public void addInstruction( AbstractInsnNode instr ){
		instructions.add( instr );
	}
	
	public AbstractInsnNode getInstruction( int idx ){
		return instructions.get(idx);
	}
	
	public int getInstructionLenght(){
		return instructions.size();
	}
	
	public int getBBAddress(){
		return bbAdrress;
	}
	
	public void addEdge( BasicBlock bb, String edgeLabel ){
		edges.put(bb, edgeLabel);
	}
	
	public String getEdge( BasicBlock bb ){
		return edges.get(bb);
	}
	
	public int getEdgesLenght(){
		return edges.size();
	}
	
	public Iterator<BasicBlock> getEdges(){
		return edges.keySet().iterator();
	}
	
}
