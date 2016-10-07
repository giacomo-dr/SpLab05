package ch.usi.inf.sp.cfg;

import java.io.FileInputStream;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;


/**
 * A JavaClassDisassembler can disassemble Java class files.
 * It presents an output similar to javap -c.
 * Given the name of the class file as a command line argument,
 * it prints the name of the class, a list of all methods,
 * and for each method, the list of all Java bytecode instructions.
 * <p>
 * The format of the disassembled bytecode includes the opcodes 
 * (in the form of mnemonics such as "ILOAD") and all the operands.
 * Some operands can be printed as simple integers, while others have to be printed in a more understandable form
 * (e.g. method or field names and descriptors).
 * Operands of branch instructions are shown as an "id" of the targeted instruction.
 * For this, all instructions of a method, including ASM's pseudo-instructions (LABEL, LINE, FRAME), 
 * are numbered, starting at 0.
 * The instruction id allows you to look up the corresponding instruction object in the instruction list:
 * AbstractInsnNode target = instructionList.get(targetId);
 * <p>
 * An example output:
 * 
 * <pre>
 * Class: ExampleClass
 *     ...
 *   Method: switchMethod2(I)I
 *   0:	 	 // label
 *   1:	 	 // line number information
 *   2:		ICONST_0 
 *   3:		ISTORE 2
 *   4:	 	 // label
 *   5:		 // line number information
 *   6:		ILOAD 1
 *   7:		LOOKUPSWITCH 0: 8, 1000: 13, 2000: 18, default: 23
 *   8:	 	 // label
 *   9:	 	 // line number information
 *   10:	ICONST_0 
 *   11:	ISTORE 2
 *   12:	GOTO 27
 *   13:	 // label
 *   14:	 // line number information
 *   15:	ICONST_1 
 *   16:	ISTORE 2
 *   17:	GOTO 27
 *   18:	 // label
 *   19:	 // line number information
 *   20:	ICONST_2 
 *   21:	ISTORE 2
 *   22:	GOTO 27
 *   23:	 // label
 *   24:	 // line number information
 *   25:	ICONST_M1 
 *   26:	ISTORE 2
 *   27:	 // label
 *   28:	 // line number information
 *   29:	ILOAD 2
 *   30:	IRETURN 
 *   31:	 // label
 * </pre>
 * 
 * @author Matthias.Hauswirth@usi.ch
 */
public final class JavaClassDisassembler {

	public static void main(final String[] args) throws Exception {
		// create a ClassReader that loads the Java .class file specified as the command line argument
		final String classFileName = args[0];
		final ClassReader cr = new ClassReader(new FileInputStream(classFileName));
		// create an empty ClassNode (in-memory representation of a class)
		final ClassNode clazz = new ClassNode();
		// have the ClassReader read the class file and populate the ClassNode with the corresponding information
		cr.accept(clazz, 0);
		// create a dumper and have it dump the given ClassNode
		final JavaClassDisassembler dumper = new JavaClassDisassembler();
		dumper.disassembleClass(clazz);
	}
	
	public void disassembleClass(final ClassNode clazz) {
		System.out.println("Class: "+clazz.name);
		// get the list of all methods in that class
		@SuppressWarnings("unchecked")
		final List<MethodNode> methods = clazz.methods;
		for (int m=0; m<methods.size(); m++) {
			final MethodNode method = methods.get(m);
			disassembleMethod(method);
		}
	}
	
	public void disassembleMethod(final MethodNode method) {
		System.out.println("  Method: "+method.name+method.desc);
		// get the list of all instructions in that method
		final InsnList instructions = method.instructions;
		for (int i=0; i<instructions.size(); i++) {
			final AbstractInsnNode instruction = instructions.get(i);
			disassembleInstruction(instruction, i, instructions);
		}
	}
	
	/**
	 * Hint: Check out org.objectweb.asm.MethodVisitor to determine which instructions (opcodes)
	 * have which instruction types (subclasses of AbstractInsnNode).
	 * 
	 * E.g. the comment in org.objectweb.asm.MethodVisitor.visitIntInsn(int opcode, int operand) 
	 * shows the list of all opcodes that are represented as instructions of type IntInsnNode.
	 * That list e.g. includes the BIPUSH opcode.
	 */
	public void disassembleInstruction(final AbstractInsnNode instruction, final int i, final InsnList instructions) {
		final int opcode = instruction.getOpcode();
		final String mnemonic = opcode==-1?"":Printer.OPCODES[instruction.getOpcode()];
		System.out.print(i+":\t"+mnemonic+" ");
		// There are different subclasses of AbstractInsnNode.
		// AbstractInsnNode.getType() represents the subclass as an int.
		// Note:
		// to check the subclass of an instruction node, we can either use:
		//   if (instruction.getType()==AbstractInsnNode.LABEL)
		// or we can use:
		//   if (instruction instanceof LabelNode)
		// They give the same result, but the first one can be used in a switch statement.
		switch (instruction.getType()) {
		case AbstractInsnNode.LABEL: 
			// pseudo-instruction (branch or exception target)
			System.out.print("// label");
			break;
		case AbstractInsnNode.FRAME:
			// pseudo-instruction (stack frame map)
			System.out.print("// stack frame map");
			break;
		case AbstractInsnNode.LINE:
			// pseudo-instruction (line number information)
			System.out.print("// line number information");
		case AbstractInsnNode.INSN:
			// Opcodes: NOP, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2,
		    // ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0,
			// FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD, LALOAD, FALOAD,
			// DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE,
			// DASTORE, AASTORE, BASTORE, CASTORE, SASTORE, POP, POP2, DUP,
			// DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP, IADD, LADD, FADD,
			// DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV,
			// FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL,
			// LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
			// I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B,
			// I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN,
			// FRETURN, DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW,
			// MONITORENTER, or MONITOREXIT.
			// zero operands, nothing to print
			break;
		case AbstractInsnNode.INT_INSN:
			// Opcodes: NEWARRAY, BIPUSH, SIPUSH.
			if (instruction.getOpcode()==Opcodes.NEWARRAY) {
				// NEWARRAY
				System.out.println(Printer.TYPES[((IntInsnNode)instruction).operand]);
			} else {
				// BIPUSH or SIPUSH
				System.out.println(((IntInsnNode)instruction).operand);
			}
			break;
		case AbstractInsnNode.JUMP_INSN:
			// Opcodes: IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
		    // IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
		    // IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
		{
			final LabelNode targetInstruction = ((JumpInsnNode)instruction).label;
			final int targetId = instructions.indexOf(targetInstruction);
			System.out.print(targetId);
			break;
		}
		case AbstractInsnNode.LDC_INSN:
			// Opcodes: LDC.
			System.out.print(((LdcInsnNode)instruction).cst);
			break;
		case AbstractInsnNode.IINC_INSN:
			// Opcodes: IINC.
			System.out.print(((IincInsnNode)instruction).var);
			//System.out.println(" ");
			System.out.print(" ");
			System.out.print(((IincInsnNode)instruction).incr);
			break;
		case AbstractInsnNode.TYPE_INSN:
			// Opcodes: NEW, ANEWARRAY, CHECKCAST or INSTANCEOF.
			System.out.print(((TypeInsnNode)instruction).desc);
			break;
		case AbstractInsnNode.VAR_INSN:
			// Opcodes: ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE,
		    // LSTORE, FSTORE, DSTORE, ASTORE or RET.
			System.out.print(((VarInsnNode)instruction).var);
			break;
		case AbstractInsnNode.FIELD_INSN:
			// Opcodes: GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
			System.out.print(((FieldInsnNode)instruction).owner);
			System.out.print(".");
			System.out.print(((FieldInsnNode)instruction).name);
			System.out.print(" ");
			System.out.print(((FieldInsnNode)instruction).desc);
			break;
		case AbstractInsnNode.METHOD_INSN:
			// Opcodes: INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC,
		    // INVOKEINTERFACE or INVOKEDYNAMIC.
			System.out.print(((MethodInsnNode)instruction).owner);
			System.out.print(".");
			System.out.print(((MethodInsnNode)instruction).name);
			System.out.print(" ");
			System.out.print(((MethodInsnNode)instruction).desc);
			break;
		case AbstractInsnNode.MULTIANEWARRAY_INSN:
			// Opcodes: MULTIANEWARRAY.
			System.out.print(((MultiANewArrayInsnNode)instruction).desc);
			System.out.print(" ");
			System.out.print(((MultiANewArrayInsnNode)instruction).dims);
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
				System.out.print(key+": "+targetId+", ");
			}
			final LabelNode defaultTargetInstruction = ((LookupSwitchInsnNode)instruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			System.out.print("default: "+defaultTargetId);
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
				System.out.print(key+": "+targetId+", ");
			}
			final LabelNode defaultTargetInstruction = ((TableSwitchInsnNode)instruction).dflt;
			final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
			System.out.print("default: "+defaultTargetId);
			break;
		}
		}		
		System.out.println();
	}
	
}
