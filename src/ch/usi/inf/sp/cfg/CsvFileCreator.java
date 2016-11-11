package ch.usi.inf.sp.cfg;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class CsvFileCreator {
		
	public static void writeCsvStatistics( String filename, String benchmark, String packageName, 
			ControlFlowGraph cfg, String kind, int instructionsCount ) throws IOException{
		FileWriter fstream = new FileWriter( filename, true );
		BufferedWriter out = new BufferedWriter(fstream);
		
		out.write(benchmark + ",");
		out.write(packageName + ",");
		out.write(cfg.getClassName() + ",");
		out.write(cfg.getMethodName() + ",");
		out.write(kind + ",");
		out.write(instructionsCount+ ","); 
		out.write(cfg.getBasicBlockCount() + ",");
		
		out.close();
	}
	
}
