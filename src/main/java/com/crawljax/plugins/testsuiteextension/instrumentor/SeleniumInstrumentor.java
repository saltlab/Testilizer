package com.crawljax.plugins.testsuiteextension.instrumentor;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.plugins.utils.CompilationUnitUtils;



public class SeleniumInstrumentor {

	private static final Logger LOG = LoggerFactory.getLogger(SeleniumInstrumentor.class);

    //Amin injected
    public static By lastUsedBy;
	private static String seleniumExecutionTrace = "SeleniumExecutionTrace.txt";

	public SeleniumInstrumentor() {
	}

	
	public void instrument(File file) {
		LOG.info("Instrumenting the Selenium test cases in {}", file.getAbsolutePath());
		instrument(file, true);
	}

	public void instrument(File file, boolean writeBack) {
		try {
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
					ASTHelper.addStmt(block, stmt);

					for (MethodCallExpr mce : methodCalls) {
						if (stmt.toString().contains(mce.toString())){
							// add a statement do the method body
							String s = stmt.toString();
							s = s.replaceAll("[\n\r]", "");
							s = s.replace(";", "");
							s = s.replace("\\", "\\\\");
							s = s.replace("\"", "\\\"");
							Statement inject = JavaParser.parseStatement("com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor.parseStatement(\""+ s +"\");");
							//System.out.println("mce.getName(): " + mce.getName());
							//System.out.println("mce.getArgs(): " + mce.getArgs());
							//Statement inject = JavaParser.parseStatement("i++;");
							ASTHelper.addStmt(block, inject);

							break; // consider only one mce
						}
					}
				}

				testCaseMethod.setBody(block);

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
		switch(mce.getName()){
		case "findElement":
			codeToInstrument = "com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy";
			break;
		case "sendKeys":
			codeToInstrument = "com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getInput";
			break;
		case "clear":
			System.out.println("mce.getParentNode() is " + mce);
			System.out.println("mce.getParentNode() is " + mce.getParentNode());
						
			//mce.setName("findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getNextMethod(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.lastUsedBy, \"clear\")).clear");
			break;
		case "click":
			//mce.setName("findElement(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getNextMethod(com.crawljax.plugins.instrumentor.SeleniumInstrumentor.lastUsedBy, \"click\")).click");
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

	
	public static By getNextMethod(By by, String method) {
		try {
		    FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //appending new data
		    fw.write(method + "\n");
			System.out.println("by is" + by.toString());
			System.out.println(method);
		    
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		return by;
	}


	public static void parseStatement(String string) {
		try {
		    FileWriter fw = new FileWriter(seleniumExecutionTrace, true); //appending new data
		    fw.write(string + "\n");
		    fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		    System.err.println("IOException: " + e.getMessage());
		}	
	}
	
	
}
