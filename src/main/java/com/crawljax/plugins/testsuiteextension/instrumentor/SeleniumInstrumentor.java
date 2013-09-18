package com.crawljax.plugins.testsuiteextension.instrumentor;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.PrimitiveType.Primitive;
import japa.parser.ast.visitor.GenericVisitor;
import japa.parser.ast.visitor.VoidVisitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.plugins.utils.CompilationUnitUtils;



public class SeleniumInstrumentor {

	private static final Logger LOG = LoggerFactory.getLogger(SeleniumInstrumentor.class);

	public static By lastUsedBy;
	public static String seleniumExecutionTrace = "SeleniumExecutionTrace.txt";

	public SeleniumInstrumentor() {
	}


	public void instrument(File file) {
		LOG.info("Instrumenting the Selenium test cases in {}", file.getAbsolutePath());
		instrument(file, true);
	}

	
	/**
	 * The pattern to be saved in the log file is as following:
	 * 
	 * TestSuiteBegin
	 * NewTestCase
	 * By.id: login
	 * sendKeys: ...
	 * click
	 * ...
	 * NewTestCase
	 * ...
	 * TestSuiteEnd
	 */					
	public void instrument(File file, boolean writeBack) {
		try {
			writeToSeleniumExecutionTrace("NewTestCase");

			TestCaseParser tcp = new TestCaseParser();
			CompilationUnit cu;
			cu = TestCaseParser.getCompilationUnitOfFileName(file.getAbsolutePath());

			ArrayList<MethodDeclaration> testCases = tcp.getTestMethodDeclaration(cu);

			for (MethodDeclaration testCaseMethod : testCases) {
				LOG.info("testcase: {}", testCaseMethod.getName());
				System.out.println("testcase: " + testCaseMethod.getName());

				ArrayList<MethodCallExpr> methodCalls = tcp.getMethodCalls(testCaseMethod, TestCaseParser.seleniumDomRelatedMethodCallList);
				//for (MethodCallExpr mce : methodCalls)
				//	System.out.println(mce);

				// add a body to the method
				BlockStmt block = new BlockStmt();

				for (Statement stmt : testCaseMethod.getBody().getStmts()) {
					System.out.println("stmt: " + stmt);

					// no instrumentation needed if there is no method calls
					boolean hasMethodCall = false;
					for (MethodCallExpr mce : methodCalls) {
						if (stmt.toString().contains(mce.toString()))
							hasMethodCall = true;
					}
					// do not change non method calls statements
					if (hasMethodCall == false)
						ASTHelper.addStmt(block, stmt);

					for (MethodCallExpr mce : methodCalls) {
						
						if (stmt.toString().contains(mce.toString())){
							// add a statement do the method body
							String s = stmt.toString();
							s = s.replaceAll("[\n\r]", "");
							s = s.replace(";", "");
							s = s.replace("\\", "\\\\");
							s = s.replace("\"", "\\\"");
							
							Statement inject = null;

							if (mce.getName().equals("clear") || mce.getName().equals("click")){
								// instrument with a new code
								
								Expression scope = mce.getScope();
								//System.out.println("com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.getWebElement("+ scope.toString() +");");
								//inject = JavaParser.parseStatement("com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.getWebElement("+ scope.toString() +");");

								System.out.println("com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.getWebElement("+ scope.toString() +")." + mce.getName() + "();");
								inject = JavaParser.parseStatement("com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.getWebElement("+ scope.toString() +")." + mce.getName() + "();");							
							}else{
								// otherwise also add the original statements
								ASTHelper.addStmt(block, stmt);
							}
							
							//inject = JavaParser.parseStatement("com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.parseStatement();");
							
							//Statement inject = JavaParser.parseStatement("com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.parseStatement(\""+ s +"\");");
							
							//System.out.println("mce.getName(): " + mce.getName());
							//System.out.println("mce.getArgs(): " + mce.getArgs());
							
							if (inject!=null)
								ASTHelper.addStmt(block, inject);

							break; // TODO: remove this as it considers only one mce at this point...
						}
					}
				}

				testCaseMethod.setBody(block);

			}

			
			HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> methodCalls = tcp.getSeleniumDomRelateMethodCallExpressions(cu);

			for (MethodDeclaration e : methodCalls.keySet()) {
				LOG.info("testcase: {}", e.getName());
				System.out.println("testcase: " + e.getName());

				for (MethodCallExpr mce : methodCalls.get(e)) {
					this.instrumentMethodCall(mce);
					System.out.println(mce);
				}
			}

			
			if (writeBack == true){
				String newFileLoc = System.getProperty("user.dir");
				// On Linux/Mac
				newFileLoc += "/src/main/java/com/crawljax/plugins/testsuiteextension/casestudies/instrumentedtests/";
				// On Windows
				//newFileLoc += "\\src\\main\\java\\casestudies\\instrumentedtests\\";

				FileOutputStream newFile = new FileOutputStream(newFileLoc+file.getName());
				cu.setPackage(new PackageDeclaration(new NameExpr("com.crawljax.plugins.testsuiteextension.casestudies.instrumentedtests")));
				CompilationUnitUtils.writeCompilationUnitToFile(cu, newFileLoc+file.getName(), false);

				LOG.info("done writing");
			}

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}



	public void instrument2(File file, boolean writeBack) {
		try {
			TestCaseParser tcp = new TestCaseParser();
			CompilationUnit cu;
			cu = TestCaseParser.getCompilationUnitOfFileName(file.getAbsolutePath());

			HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> methodCalls = tcp.getSeleniumDomRelateMethodCallExpressions(cu);

			for (MethodDeclaration e : methodCalls.keySet()) {
				LOG.info("testcase: {}", e.getName());
				System.out.println("testcase: " + e.getName());


				for (MethodCallExpr mce : methodCalls.get(e)) {
					this.instrumentMethodCall(mce);
					System.out.println(mce);
				}
			}

			if (writeBack == true){

				String newFileLoc = System.getProperty("user.dir");
				// On Linux/Mac
				newFileLoc += "/src/main/java/com/crawljax/plugins/testsuiteextension/casestudies/instrumentedtests/";
				// On Windows
				//newFileLoc += "\\src\\main\\java\\casestudies\\instrumentedtests\\";

				FileOutputStream newFile = new FileOutputStream(newFileLoc+file.getName());

				cu.setPackage(new PackageDeclaration(new NameExpr("com.crawljax.plugins.testsuiteextension.casestudies.instrumentedtests")));
				CompilationUnitUtils.writeCompilationUnitToFile(cu, newFileLoc+file.getName(), false);
				//CompilationUnitUtils.writeCompilationUnitToFile(unitToInject, newFileLoc+file.getName(), true);


				LOG.info("done writing");
			}

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public MethodCallExpr instrumentMethodCall(MethodCallExpr mce) {
		List<Expression> oldArgs = mce.getArgs();
		// create a methodcall expre
		String codeToInstrument = null;
		String methodCallName = mce.getName();
		// logging the values
		switch(mce.getName()){
		case "findElement":
			codeToInstrument = "com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.getBy";
			break;
		case "sendKeys":
			codeToInstrument = "com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.getInput";
			break;
		case "clear":
			System.out.println("CLEAR:");
			break;

		
		}

		if (codeToInstrument!=null){
			MethodCallExpr call = new MethodCallExpr(null, codeToInstrument);
			call.setArgs(oldArgs);
			// put oldargs as it's argument
			// setarguments of mce
			List<Expression> newArgs = new ArrayList<Expression>();
			newArgs.add(call);
			mce.setArgs(newArgs);
		}
		return mce;
	}


	public static By getBy(By by) {
		try {
			FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //appending new data
			fw.write(by.toString() + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException: " + e.getMessage());
		}
		System.out.println(by.toString());
		lastUsedBy = by;
		return by;
	}

	public static String getInput(String input) {
		try {
			FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //appending new data
			fw.write("sendKeys: " + input + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("sendKeys: " + input);

		return input;
	}	



	public static void parseStatement(String statement) {
		// writing commands and values in separated to the seleniumExecutionTrace file
		String action = null;
		if (statement.contains(".clear()"))
			action = "clear";
		else if (statement.contains(".click()"))
			action = "click";
		
		if (action!=null)
			writeToSeleniumExecutionTrace(action);
	}

	public static WebElement getWebElement(WebElement webElement) {
		System.out.println("getWebElement: " + webElement);
		writeToSeleniumExecutionTrace(webElement.toString());
		return webElement;
	}	

	public static void writeToSeleniumExecutionTrace(String string) {
		try {
			FileWriter fw = new FileWriter(seleniumExecutionTrace, true); //appending new data
			fw.write(string + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException: " + e.getMessage());
		}
	}

	
	public static ArrayList<String> readFromSeleniumExecutionTrace() {
		ArrayList<String> content = new ArrayList<String>();
		try {
			FileReader fileReader = new FileReader(new File(seleniumExecutionTrace));
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				//System.out.println("read from the file: " + line);
				content.add(line);
			}
			fileReader.close();
			return content;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}
}
