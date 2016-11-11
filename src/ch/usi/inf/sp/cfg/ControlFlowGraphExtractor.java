package ch.usi.inf.sp.cfg;

import java.io.FileInputStream;
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
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.Printer;

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
	@SuppressWarnings("unchecked")
	public ControlFlowGraph createCFG( String className, MethodNode method ){
		//System.out.println(method.name);
		//final JavaClassDisassembler dumper = new JavaClassDisassembler();
		//dumper.disassembleMethod(method);
		
		// Get basic blocks bounds and create BasicBlock objects
		bbMap.put( -1, new BasicBlock(-1) ); // Dummy "start" basic block 
		bbBoundAdrresses.add( 0 );           // Start of the first basic block
		bbMap.put( 0, new BasicBlock(0) );   // First basic block
		final InsnList instructions = method.instructions;
		bbMap.put( instructions.size(), new BasicBlock(-2) ); // Dummy "end" basic block
		bbBoundAdrresses.add( instructions.size() );          // End of the last basic block + 1
		for( int i=0; i<instructions.size(); i++ ){
			final AbstractInsnNode instruction = instructions.get(i);
			extractAdrresses( instruction, i, instructions );
		}
		
		// Fill BasicBlocks with instructions and add edges
		Iterator<Integer> it = bbBoundAdrresses.iterator();
		int startOfBlock = it.next(); 
		while( it.hasNext() ){
			BasicBlock bb = bbMap.get(startOfBlock);
			int endOfBlock = it.next() - 1;
			populateBasicBlock( bb, startOfBlock, endOfBlock, instructions, method.tryCatchBlocks);
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
		
		if (isPEI(instruction)){
			bbBoundAdrresses.add( i+1 );
			bbMap.put( i+1, new BasicBlock(i+1) );
		}else{
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
					bbBoundAdrresses.add( i+1 );
					bbMap.put( i+1, new BasicBlock(i+1) );
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
	}
	
	void populateBasicBlock( BasicBlock bb, int startOfBlock, 
			int endOfBlock, InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks){
		AbstractInsnNode lastSignificantInstruction = null;
		
		// Add mnemonic instructions
		for( int i = startOfBlock ; i <= endOfBlock ; i++ ){
			if( instructions.get(i).getOpcode() != -1 ){
				lastSignificantInstruction = instructions.get(i);
			}
			bb.addInstruction( instructions.get(i) );
		}
		
		if(lastSignificantInstruction == null){
			return; //TO DO: Gestire label post return
		}
		
		// Add exception edges 
		if (isPEI(lastSignificantInstruction)){
			boolean isFinally = false;
			final int instNumber = instructions.indexOf(lastSignificantInstruction);
			for(TryCatchBlockNode block: tryCatchBlocks){
				final int start = instructions.indexOf(block.start); 
				final int end = instructions.indexOf(block.end);
				final int handler = instructions.indexOf(block.handler);
				if(instNumber>=start && instNumber<end){
					bb.addEdge( bbMap.get(handler), "ex" );
					if(block.type == null){
						isFinally=true;
						break;
					}
				}
			}
			if(!isFinally){
				bb.addEdge( bbMap.get(instructions.size()), "ex" );
			}
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
				if( endOfBlock != instructions.size() -1 && // Don't chain last line
					lastSignificantInstruction.getOpcode() != Opcodes.ATHROW ) // Exception 
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
	
	public boolean isPEI( AbstractInsnNode instruction ){
		switch (instruction.getOpcode()) {
		  case Opcodes.AALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.AASTORE: // NullPointerException, ArrayIndexOutOfBoundsException, ArrayStoreException
		  case Opcodes.ANEWARRAY: // NegativeArraySizeException, (linking)
		  case Opcodes.ARETURN: // IllegalMonitorStateException (if synchronized)
		  case Opcodes.ARRAYLENGTH: // NullPointerException
		  case Opcodes.ATHROW: // NullPointerException, IllegalMonitorStateException (if synchronized), 
		  case Opcodes.BALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.BASTORE: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.CALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.CASTORE: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.CHECKCAST: // ClassCastException, (linking)
		  case Opcodes.DALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.DASTORE: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.DRETURN: // IllegalMonitorStateException (if synchronized)
		  case Opcodes.FALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.FASTORE: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.FRETURN: // IllegalMonitorStateException (if synchronized)
		  case Opcodes.GETFIELD: // NullPointerException, (linking)
		  case Opcodes.GETSTATIC: // Error*, (linking)
		  case Opcodes.IALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.IASTORE: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.IDIV: // ArithmeticException
		  case Opcodes.INSTANCEOF: // (linking)
		  case Opcodes.INVOKEDYNAMIC: // what's this??
		  case Opcodes.INVOKEINTERFACE: // NullPointerException, IncompatibleClassChangeError, AbstractMethodError, IllegalAccessError, AbstractMethodError, UnsatisfiedLinkError, (linking)
		  case Opcodes.INVOKESPECIAL: // NullPointerException, UnsatisfiedLinkError, (linking)
		  case Opcodes.INVOKESTATIC: // UnsatisfiedLinkError, Error*, (linking)
		  case Opcodes.INVOKEVIRTUAL: // NullPointerException, AbstractMethodError, UnsatisfiedLinkError, (linking)
		  case Opcodes.IREM: // ArithmeticException
		  case Opcodes.IRETURN: // IllegalMonitorStateException (if synchronized)
		  case Opcodes.LALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.LASTORE: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.LDIV: // ArithmeticException
		  case Opcodes.LREM: // ArithmeticException
		  case Opcodes.LRETURN: // IllegalMonitorStateException (if synchronized)
		  case Opcodes.MONITORENTER: // NullPointerException
		  case Opcodes.MONITOREXIT: // NullPointerException, IllegalMonitorStateException
		  case Opcodes.MULTIANEWARRAY: // NegativeArraySizeException, (linking)
		  case Opcodes.NEW: // Error*, (linking)
		  case Opcodes.NEWARRAY: // NegativeArraySizeException
		  case Opcodes.PUTFIELD: // NullPointerException, (linking)
		  case Opcodes.PUTSTATIC: // Error*, (linking)
		  case Opcodes.RETURN: // IllegalMonitorStateException (if synchronized)
		  case Opcodes.SALOAD: // NullPointerException, ArrayIndexOutOfBoundsException
		  case Opcodes.SASTORE: // NullPointerException, ArrayIndexOutOfBoundsException
		    return true;
		}
		return false;
	}

	public static void main(final String[] args) throws IOException {
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

	public static void analyzeClassReader(ClassReader cr, String jarfile) throws IOException {
		final ClassNode clazz = new ClassNode();
		cr.accept(clazz, 0);

		for( int m = 0; m < clazz.methods.size(); m++ ){
			@SuppressWarnings("unchecked")
			final MethodNode method = ((List<MethodNode>)clazz.methods).get(m);
            ControlFlowGraphExtractor cfgExt = new ControlFlowGraphExtractor();
            ControlFlowGraph graph = cfgExt.createCFG( clazz.name, method );

            String packageName = clazz.name.substring(0, clazz.name.lastIndexOf('/'));
            CsvFileCreator.writeCsvStatistics(jarfile, packageName, graph, 
            	 	method.access, method.instructions.size());
		}
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
