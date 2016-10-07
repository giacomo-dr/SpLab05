package ch.usi.inf.sp.da;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;

public class DotFileCreator {
	
	ControlFlowGraph cfg;
	InsnList instructions;
	BufferedWriter out;
	
	public DotFileCreator( ControlFlowGraph cfg, InsnList instructions,
			String filename ) throws IOException{
		this.cfg = cfg;
		this.instructions = instructions;
		FileWriter fstream = new FileWriter( filename + ".gv", false );
		out = new BufferedWriter(fstream);
	}
	
	public void generate() throws IOException{
		printPrologue();
		printBBNodes();
		printEpilogue();
		out.close();
	}
	
	private void printPrologue() throws IOException{
		out.write( "digraph " );
		out.write( cfg.getClassName() + "_" + cfg.getMethodName() );
		out.write( " {\n" );
		out.write( "\tnode [shape=record]\n" );
	}
	
	private void printBBNodes() throws IOException{
		Iterator<BasicBlock> bbIterator = cfg.getBasicBlocks();
		while( bbIterator.hasNext() ){
			BasicBlock bb = bbIterator.next();
//			if(bb.getEdgesLenght()==0){
//				continue;
//			}
			printDotNode( bb );
			printEdges( bb );
		}
	}
	
	private void printDotNode( BasicBlock bb ) throws IOException{
		if( bb.getBBAddress() == -1 && bb.getEdges().hasNext() ){
			// "Start" dummy node
			out.write( "\tS [label=\"S\", shape=ellipse]\n" );
		}else if( bb.getBBAddress() == -2 && !bb.getEdges().hasNext() ){
			// "End" dummy node
			out.write( "\tE [label=\"E\", shape=ellipse]\n" );
		}else{
			// Inner node
			out.write( "\tB" + bb.getBBAddress() + " [\n" );
			out.write( "\t\tlabel=\"\\(B" + bb.getBBAddress() + "\\) | { <top> " );
			int currentAddress = bb.getBBAddress();
			boolean firstNode = true;
			for( int i = 0 ; i < bb.getInstructionLenght() ; i++){
				AbstractInsnNode instruction = bb.getInstruction(i);
				int opcode = instruction.getOpcode();
				if( opcode != -1){
					if( !firstNode ){
						out.write( " |" );
						// Add
						if( i ==  bb.getInstructionLenght() - 1 ){
							out.write( " <bottom> " );
						}
					}
					out.write( "" + currentAddress + ": ");
					out.write( formatInstruction(instruction) );
					firstNode = false;
				}
				currentAddress++;
			}
			out.write( " }\"\n" );
			out.write( "\t\t]\n" );
			out.write( "\n" );
		}
	}
	
	private void printEdges( BasicBlock bb ) throws IOException{
		Iterator<BasicBlock> edges = bb.getEdges();
		while( edges.hasNext() ){
			BasicBlock targetBb = edges.next();
			String label = bb.getEdge( targetBb );
			String sourceName = bb.getBBAddress() == -1 ? "S" : "B" + bb.getBBAddress();
			String targetName = targetBb.getBBAddress() == -2 ? "E" : "B" + targetBb.getBBAddress();
			out.write( "\t" + sourceName + ":<bottom> -> ");
			out.write( targetName + ":<top>");
			if(label=="ex"){
				out.write( " [style=dotted]\n\n");
			}else{
				out.write( " [label=\"" + label + "\"]\n\n");
			}
		}
	}

	String formatInstruction( AbstractInsnNode instruction ){
		String result = "";
		final int opcode = instruction.getOpcode();
		result += Printer.OPCODES[opcode] + " ";
		
		switch (instruction.getType()) {
		case AbstractInsnNode.INT_INSN:
			// Opcodes: NEWARRAY, BIPUSH, SIPUSH.
			if (instruction.getOpcode()==Opcodes.NEWARRAY) {
				// NEWARRAY
				result += Printer.TYPES[((IntInsnNode)instruction).operand];
			} else {
				// BIPUSH or SIPUSH
				result += ((IntInsnNode)instruction).operand;
			}
			break;
		case AbstractInsnNode.JUMP_INSN:
			// Opcodes: IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
		    // IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
		    // IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
		{
			final LabelNode targetInstruction = ((JumpInsnNode)instruction).label;
			result += "(B" + instructions.indexOf(targetInstruction) + ")";
			break;
		}
		case AbstractInsnNode.LDC_INSN:
			// Opcodes: LDC.
			result += ((LdcInsnNode)instruction).cst;
			break;
		case AbstractInsnNode.IINC_INSN:
			// Opcodes: IINC.
			result += ((IincInsnNode)instruction).var;
			result += " ";
			result += ((IincInsnNode)instruction).incr;
			break;
		case AbstractInsnNode.TYPE_INSN:
			// Opcodes: NEW, ANEWARRAY, CHECKCAST or INSTANCEOF.
			result += ((TypeInsnNode)instruction).desc;
			break;
		case AbstractInsnNode.VAR_INSN:
			// Opcodes: ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE,
		    // LSTORE, FSTORE, DSTORE, ASTORE or RET.
			result += ((VarInsnNode)instruction).var;
			break;
		case AbstractInsnNode.FIELD_INSN:
			// Opcodes: GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
			result += ((FieldInsnNode)instruction).owner;
			result += ".";
			result += ((FieldInsnNode)instruction).name;
			result += " ";
			result += ((FieldInsnNode)instruction).desc;
			break;
		case AbstractInsnNode.METHOD_INSN:
			// Opcodes: INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC,
		    // INVOKEINTERFACE or INVOKEDYNAMIC.
			result += ((MethodInsnNode)instruction).owner;
			result += ".";
			result += ((MethodInsnNode)instruction).name;
			result += " ";
			result += ((MethodInsnNode)instruction).desc;
			break;
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			// Opcodes: MULTIANEWARRAY.
			result += ((MultiANewArrayInsnNode)instruction).desc;
			result += " ";
			result += ((MultiANewArrayInsnNode)instruction).dims;
			break;
		case AbstractInsnNode.LOOKUPSWITCH_INSN:
			// Opcodes: LOOKUPSWITCH.
		{
			final List<?> keys = ((LookupSwitchInsnNode)instruction).keys;
			final List<?> labels = ((LookupSwitchInsnNode)instruction).labels;
			for (int t=0; t<keys.size(); t++) {
				final int key = (Integer)keys.get(t);
				final LabelNode targetInstruction = (LabelNode)labels.get(t);
				final int targetId = instructions.indexOf(targetInstruction);
				result += key + ": (B" + targetId + "), ";
			}
			final LabelNode defaultTargetInstruction = ((LookupSwitchInsnNode)instruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			result += "default: (B" + defaultTargetId + ")";
			break;
		}
		case AbstractInsnNode.TABLESWITCH_INSN:
			// Opcodes: TABLESWITCH.
		{
			final int minKey = ((TableSwitchInsnNode)instruction).min;
			final List<?> labels = ((TableSwitchInsnNode)instruction).labels;
			for (int t=0; t<labels.size(); t++) {
				final int key = minKey+t;
				final LabelNode targetInstruction = (LabelNode)labels.get(t);
				final int targetId = instructions.indexOf(targetInstruction);
				result += key + ": (B" + targetId + "), ";
			}
			final LabelNode defaultTargetInstruction = ((TableSwitchInsnNode)instruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			result += "default: (B" + defaultTargetId + ")";
			break;
		}
		}		
		
		return result;
	}
	
	private void printEpilogue() throws IOException{
		out.write( "}\n" );
	}
}
