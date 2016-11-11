package ch.usi.inf.sp.cfg;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.objectweb.asm.Opcodes;


public class CsvFileCreator {
	
	public static final String outfname = "bbsizes.csv";
	
	public static HashMap<Integer, String> accessTable;
	
	private static boolean inited = false;
	
	public static void writeCsvStatistics( String benchmark, String packageName, 
			ControlFlowGraph cfg, int access, int instructionsCount ) throws IOException{
		if (!inited) {
			FileWriter fstream = new FileWriter( outfname, false );
			fstream.write("benckmark,package,class,methodName,access,instructions,bblocks\n");
			fstream.close();
			accessTable = new HashMap<>();
			accessTable.put(Opcodes.ACC_PUBLIC, "public");
			accessTable.put(Opcodes.ACC_PRIVATE, "private");
			accessTable.put(Opcodes.ACC_PROTECTED, "protected");
			inited = true;
		}
		
		FileWriter fstream = new FileWriter( outfname, true );
		BufferedWriter out = new BufferedWriter(fstream);
		
		String className = cfg.getClassName().substring(cfg.getClassName().lastIndexOf('/') + 1);
		out.write(benchmark + ",");
		out.write(packageName + ",");
		out.write(className + ",");
		out.write(cfg.getMethodName() + ",");
		out.write(accessTable.getOrDefault(access, "other") + ",");
		out.write(instructionsCount+ ","); 
		out.write(cfg.getBasicBlockCount() - 2 + "\n");
		
		out.close();
	}
	
}
