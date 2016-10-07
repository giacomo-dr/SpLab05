package ch.usi.inf.sp.da;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

/**
 * This class extracts a control flow graph in .dot format
 * from the byte code of a Java method.
 * 
 * @author Eric Botter, Lorenzo Ferretti, Giacomo Del Rio
 */
public final class ControlFlowGraphExtractor {
	
	private SortedSet<Integer> bbBoundAdrresses;
	private Map<Integer, BasicBlock> bbMap;
	
	public ControlFlowGraphExtractor(){
		bbBoundAdrresses = new TreeSet<>();
		bbMap = new HashMap<Integer, BasicBlock>();
	}
	
	/**
	 * Creates a Control Flow Graph out of a method
	 * @param method
	 * @return
	 */
	public ControlFlowGraph createCFG( String className, MethodNode method ){
		// Get basic blocks bounds and create BasicBlock objects
		bbMap.put( -1, new BasicBlock(-1) ); // Dummy "start" basic block 
		bbBoundAdrresses.add( 0 );           // Start of the first basic block
		bbMap.put( 0, new BasicBlock(0) );   // First basic block
		final InsnList instructions = method.instructions;
		for( int i=0; i<instructions.size(); i++ ){
			final AbstractInsnNode instruction = instructions.get(i);
			extractAdrresses( instruction, i, instructions );
		}
		bbBoundAdrresses.add( instructions.size() );          // End of the last basic block + 1
		bbMap.put( instructions.size(), new BasicBlock(-2) ); // Dummy "end" basic block 
		
		// Fill BasicBlocks with instructions and add edges
		Iterator<Integer> it = bbBoundAdrresses.iterator();
		int startOfBlock = it.next(); 
		while( it.hasNext() ){
			BasicBlock bb = bbMap.get(startOfBlock);
			int endOfBlock = it.next() - 1;
			populateBasicBlock( bb, startOfBlock, endOfBlock, instructions );
			startOfBlock = endOfBlock + 1;
		}
		
		// Connect the dummy "start" basic block to the first basic block
		bbMap.get( -1 ).addEdge( bbMap.get( 0 ), "" );
		
		ControlFlowGraph cfg = new ControlFlowGraph( className, method.name,
				new ArrayList<BasicBlock>( bbMap.values()) );
		return cfg;
	}
	
	/**
	 * This method extracts addresses from instructions that are used as
	 * starting and ending points of the basics blocks.
	 * 
	 * @param instruction The instruction to be analyzed
	 * @param i Index of instruction into instructions array
	 * @param instructions
	 */
	public void extractAdrresses( AbstractInsnNode instruction, int i, InsnList instructions){
		switch (instruction.getType()) {

		case AbstractInsnNode.JUMP_INSN:
			// Opcodes: IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
		    // IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
		    // IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
		{
			final LabelNode targetInstruction = ((JumpInsnNode)instruction).label;
			final int targetId = instructions.indexOf(targetInstruction);
			bbBoundAdrresses.add( targetId );
			bbMap.put( targetId, new BasicBlock(targetId) );
			if( instruction.getOpcode() != Opcodes.GOTO ){
				bbBoundAdrresses.add( i+1 );
				bbMap.put( i+1, new BasicBlock(i+1) );
			}
			break;
		}
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			// Opcodes: LOOKUPSWITCH.
		{
			final List<?> labels = ((LookupSwitchInsnNode)instruction).labels;
			for (int t=0; t<labels.size(); t++) {
				final LabelNode targetInstruction = (LabelNode)labels.get(t);
				final int targetId = instructions.indexOf(targetInstruction);
				bbBoundAdrresses.add( targetId );
				bbMap.put( targetId, new BasicBlock(targetId) );
			}
			final LabelNode defaultTargetInstruction = ((LookupSwitchInsnNode)instruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			bbBoundAdrresses.add( defaultTargetId );
			bbMap.put( defaultTargetId, new BasicBlock(defaultTargetId) );
			// bbBoundAdrresses.add( i+1 );
			break;
		}
		case AbstractInsnNode.TABLESWITCH_INSN:
			// Opcodes: TABLESWITCH.
		{
			final List<?> labels = ((TableSwitchInsnNode)instruction).labels;
			for( int t=0; t<labels.size(); t++ ){
				final LabelNode targetInstruction = (LabelNode)labels.get(t);
				final int targetId = instructions.indexOf(targetInstruction);
				bbBoundAdrresses.add( targetId );
				bbMap.put( targetId, new BasicBlock(targetId) );
			}
			final LabelNode defaultTargetInstruction = ((TableSwitchInsnNode)instruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			bbBoundAdrresses.add( defaultTargetId );
			bbMap.put( defaultTargetId, new BasicBlock(defaultTargetId) );
			break;
		}
		}		
	}
	
	void populateBasicBlock( BasicBlock bb, int startOfBlock, 
			int endOfBlock, InsnList instructions ){
		AbstractInsnNode lastSignificantInstruction = null;
		
		// Add mnemonic instructions
		for( int i = startOfBlock ; i <= endOfBlock ; i++ ){
			if( instructions.get(i).getOpcode() != -1 ){
				lastSignificantInstruction = instructions.get(i);
			}
			bb.addInstruction( instructions.get(i) );
		}
		
		// Add outgoing edges
		switch( lastSignificantInstruction.getType() ){

		case AbstractInsnNode.JUMP_INSN:
			// Opcodes: IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
		    // IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
		    // IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
		{
			final LabelNode targetInstruction = ((JumpInsnNode)lastSignificantInstruction).label;
			final int targetId = instructions.indexOf(targetInstruction);
			bb.addEdge( bbMap.get(targetId), "T" );
			if( lastSignificantInstruction.getOpcode() != Opcodes.GOTO ){
				bb.addEdge( bbMap.get(endOfBlock + 1), "F" );
			}
			break;
		}
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			// Opcodes: LOOKUPSWITCH.
		{
			final List<?> keys = ((LookupSwitchInsnNode)lastSignificantInstruction).keys;
			final List<?> labels = ((LookupSwitchInsnNode)lastSignificantInstruction).labels;
			for( int t=0; t<labels.size(); t++ ){
				final int key = (Integer)keys.get(t);
				final LabelNode targetInstruction = (LabelNode)labels.get(t);
				final int targetId = instructions.indexOf(targetInstruction);
				bb.addEdge( bbMap.get(targetId), "Case " + key );
			}
			final LabelNode defaultTargetInstruction = ((LookupSwitchInsnNode)lastSignificantInstruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			bb.addEdge( bbMap.get(defaultTargetId), "default" );
			break;
		}
		case AbstractInsnNode.TABLESWITCH_INSN:
			// Opcodes: TABLESWITCH.
		{
			final int minKey = ((TableSwitchInsnNode)lastSignificantInstruction).min;
			final List<?> labels = ((TableSwitchInsnNode)lastSignificantInstruction).labels;
			for( int t=0; t<labels.size(); t++ ){
				final int key = minKey+t;
				final LabelNode targetInstruction = (LabelNode)labels.get(t);
				final int targetId = instructions.indexOf(targetInstruction);
				bb.addEdge( bbMap.get(targetId), "Case " + key );
			}
			final LabelNode defaultTargetInstruction = ((TableSwitchInsnNode)lastSignificantInstruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			bb.addEdge( bbMap.get(defaultTargetId), "default" );
			break;
		}
		case AbstractInsnNode.INSN:
		{
			if( lastSignificantInstruction.getOpcode() == Opcodes.IRETURN ||
				lastSignificantInstruction.getOpcode() == Opcodes.LRETURN ||
				lastSignificantInstruction.getOpcode() == Opcodes.FRETURN ||
				lastSignificantInstruction.getOpcode() == Opcodes.DRETURN ||
				lastSignificantInstruction.getOpcode() == Opcodes.ARETURN ||
				lastSignificantInstruction.getOpcode() == Opcodes.RETURN) {
				// Chain the return blocks with the "end" dummy basic block
				bb.addEdge( bbMap.get( instructions.size() ), "" );
			} else {
				if( endOfBlock != instructions.size() -1 ) // Don't chain last line
					bb.addEdge( bbMap.get(endOfBlock + 1), "" );
			}
			break;
		}
		default:
			if( endOfBlock != instructions.size() -1 ){
				// Don't chain last line
				bb.addEdge( bbMap.get(endOfBlock + 1), "" );
			}	
		}		
	}
	

	public static void main(final String[] args) throws FileNotFoundException, IOException {
		final String classFileName = args[0];
		final String methodNameAndDescriptor = args[1];
		
		final ClassReader cr = new ClassReader(new FileInputStream(classFileName));
		final ClassNode clazz = new ClassNode();
		cr.accept(clazz, 0);
		
		@SuppressWarnings("unchecked")
		MethodNode method = findMethod( clazz.methods, methodNameAndDescriptor );
		ControlFlowGraphExtractor cfgExt = new ControlFlowGraphExtractor();
		ControlFlowGraph graph = cfgExt.createCFG( clazz.name, method );
		
		DotFileCreator dotCreator = new DotFileCreator( graph, 
				method.instructions, clazz.name + "_" + method.name );
		dotCreator.generate();
	}

	public static MethodNode findMethod( List<MethodNode> methodList, String name ){
		for( int m = 0; m < methodList.size(); m++ ){
			final MethodNode method = methodList.get(m);
			if( method.name.equals(name) )
				return method;
		}
		return null;
	}
}
