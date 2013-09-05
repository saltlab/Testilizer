package casestudies.originaltests;

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

public class test {

	private static final Logger LOG = LoggerFactory.getLogger(test.class);
	
	public static void main(String[] args) {
		try {
			File folder = new File("/Users/aminmf/testsuiteextension-plugin/src/main/java/casestudies/originaltests/");

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
			
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(listOfFiles));
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
			boolean success = task.call();
			
			System.out.println(success);
			
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				LOG.info("Error on line {} in {}", diagnostic.getLineNumber(), diagnostic.getSource().toString());
			}    

			fileManager.close();

			if(success){
				// Executing the instrumented unit test files. This will produce a log of the execution trace
				LOG.info("Executing the instrumented unit test files and logging the execution trace...");
				for (File file : listOfFiles) {
					if (file.isFile()) {
						executeUnitTest(file.getName());
						LOG.info("Executing unit test in {}", file.getName());
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	public static void executeUnitTest(String fileName) {
		try {
			Class<?> forName = Class.forName(fileName);
			JUnitCore.runClasses(forName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
}
