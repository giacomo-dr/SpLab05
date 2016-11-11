package ch.usi.inf.sp.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;

public class JarToCfg {
    public static void main(final String[] args) throws IOException {
        final String jarFileName = args[0];
        System.out.println("Analyzing "+jarFileName);
        final JarFile jar = new JarFile(jarFileName);
        final Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                System.out.println(entry.getName());
                final InputStream is = jar.getInputStream(entry);
                ControlFlowGraphExtractor.analyzeClassReader(new ClassReader(is), 
                		jarFileName.substring(jarFileName.lastIndexOf('/')+1));
            }
        }
        jar.close();
    }

}