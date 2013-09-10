package com.crawljax.plugins.testsuiteextension.casestudies.originaltests;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor;

public class test {

	private static final Logger LOG = LoggerFactory.getLogger(test.class);
	
	public static void main(String[] args) {
		
		SeleniumInstrumentor SI = new SeleniumInstrumentor();

		try {
			String folderLoc = System.getProperty("user.dir");
			// On Linux/Mac
			folderLoc += "/src/main/java/com/crawljax/plugins/testsuiteextension/casestudies/originaltests/";
			// On Windows
			//folderLoc += "\\src\\main\\java\\casestudies\\originaltests\\";

			File folder = new File(folderLoc);
			
			System.out.println(folderLoc);
			
			// Compiling the instrumented unit test files
			LOG.info("Compiling the instrumented unit test files located in {}", folder.getAbsolutePath());

			File[] listOfFiles = folder.listFiles(new FilenameFilter() {
		                  public boolean accept(File file, String name) {
		                      return name.endsWith(".java");
		                  }
		              });

			for (File file : listOfFiles) {
			    if (file.isFile()) {
			    	System.out.println(file.getName());
					LOG.info(file.getName());
			    }
			}
			
			System.out.println(System.getProperty("java.home"));
			
			//System.setProperty("java.home", "C:\\Program Files\\Java\\jdk1.7.0_05");
					
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();			
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(listOfFiles));
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
			boolean success = task.call();
			
			System.out.println(success);
			
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				LOG.info("Error on line {} in {}", diagnostic.getLineNumber(), diagnostic.getSource().toString());
				System.out.println("Error on line " + diagnostic.getLineNumber() + " in " + diagnostic.getSource().toString());
			}    

			fileManager.close();

			if(!success){
				// Executing the instrumented unit test files. This will produce a log of the execution trace
				LOG.info("Executing the instrumented unit test files and logging the execution trace...");
				for (File file : listOfFiles) {
					if (file.isFile()) {
						
						SI.instrument(file);
						
						System.out.println("Executing unit test: " + file.getName());
						//System.out.println("Executing unit test in " + file.getAbsolutePath());
						//LOG.info("Executing unit test in {}", file.getName());
						
						//executeUnitTest(file.getAbsolutePath());
					}
					
					break; // just to test one case...
					
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	public static void executeUnitTest(String test) {
		try {
			String fileName = getFileFullName(test);
			System.out.println("Executing test class: " + fileName);
			Class<?> forName = Class.forName(fileName);
			JUnitCore.runClasses(forName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	public static String getFileFullName(String file) {
		file = file.replace(System.getProperty("user.dir"), "");
		file = file.replace("/src/main/java/com/crawljax/plugins/testsuiteextension/", "");
		// handling windows format
		file = file.replace("\\src\\main\\java\\", "");
		file = (file.contains(".")) ? file.substring(0, file.indexOf(".")) : file;
		file = file.replace("/", ".");
		file = file.replace("\\", ".");
		return file;
	}

}
