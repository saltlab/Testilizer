package com.crawljax.plugins.instrumentor;

import japa.parser.ASTHelper;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.stmt.BlockStmt;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.plugins.utils.CompilationUnitUtils;


public class SeleniumInstrumentor {

	private static final Logger LOG = LoggerFactory.getLogger(SeleniumInstrumentor.class);

	private static String seleniumExecutionTrace = "SeleniumExecutionTrace.txt";
	private static FileOutputStream fos = null;
	private static ObjectOutputStream out = null;

	public SeleniumInstrumentor() {
		try {
			fos = new FileOutputStream(seleniumExecutionTrace);
			out = new ObjectOutputStream(fos);
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
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

			HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> srmce = tcp.getSeleniumDomRelateMethodCallExpressions(cu);

			for (MethodDeclaration e : srmce.keySet()) {
				LOG.info("testcase: {}", e.getName());
				System.out.println("testcase: " + e.getName());
				for (MethodCallExpr mce : srmce.get(e)) {
					this.instrumentMethodCall(mce);
					System.out.println(mce);
				}
			}
			
			// TODO: add method 

	        // create the type declaration 
	        //ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(ModifierSet.PUBLIC, false, "GeneratedClass");
	        //ASTHelper.addTypeDeclaration(cu, type);


			// create a method	
			MethodDeclaration method = new MethodDeclaration(ModifierSet.PUBLIC, ASTHelper.VOID_TYPE, "main");
	        //method.setModifiers(ModifierSet.addModifier(method.getModifiers(), ModifierSet.STATIC));
	        ASTHelper.addMember(cu.getTypes().get(0), method);

	        // add a parameter to the method
	        Parameter param = ASTHelper.createParameter(ASTHelper.createReferenceType("String", 0), "args");
	        param.setVarArgs(true);
	        ASTHelper.addParameter(method, param);

	        // add a body to the method
	        BlockStmt block = new BlockStmt();
	        method.setBody(block);

	        // add a statement do the method body
	        NameExpr clazz = new NameExpr("System");
	        FieldAccessExpr field = new FieldAccessExpr(clazz, "out");
	        MethodCallExpr call = new MethodCallExpr(field, "println");
	        ASTHelper.addArgument(call, new StringLiteralExpr("Hello World!"));
	        ASTHelper.addStmt(block, call);

			
			// TODO: driver.findElement((By.id("login"))).X(clear,driver.findElement((By.id("login"))));
			
			
			
			if (writeBack == true){
	
				String newFileLoc = System.getProperty("user.dir");
				// On Linux/Mac
				//newFileLoc.concat("/src/main/java/casestudies/originaltests/");
				// On Windows
				newFileLoc += "\\src\\main\\java\\casestudies\\instrumentedtests\\";

				
				
				FileOutputStream newFile = new FileOutputStream(newFileLoc+file.getName());
				
				cu.setPackage(new PackageDeclaration(new NameExpr("casestudies.instrumentedtests")));
				CompilationUnitUtils.writeCompilationUnitToFile(cu, newFileLoc+file.getName());
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
		if (mce.getName().equals("findElement"))
			codeToInstrument = "com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getBy";
		else if (mce.getName().equals("sendKeys"))
			codeToInstrument = "com.crawljax.plugins.instrumentor.SeleniumInstrumentor.getInput";
		
		
		// TODO: add method 
		
		
		// TODO: driver.findElement((By.id("login"))).X(clear,driver.findElement((By.id("login"))));

		
		MethodCallExpr call = new MethodCallExpr(null, codeToInstrument);
		call.setArgs(oldArgs);
		// put oldargs as it's argument
		// setarguments of mce
		List<Expression> newArgs = new ArrayList<Expression>();
		newArgs.add(call);
		mce.setArgs(newArgs);
		return mce;
	}

	// should be static to be used by instrumented test cases
	public static By getBy(By by) {
		/*try {
		    FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //the true will append the new data
		    fw.write(by.toString() + "\n");//appends the string to the file
		    fw.close();

			LOG.info("Successfully wrote {} to {} file" , by, seleniumExecutionTrace);
			System.out.println("Successfully wrote " + by + " to " + seleniumExecutionTrace);
			
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		    System.err.println("IOException: " + ioe.getMessage());
		}*/

		System.out.println(by.toString());

		return by;
	}
	
	// should be static to be used by instrumented test cases
	public static String getInput(String input) {
		/*try {
		    FileWriter fw = new FileWriter(seleniumExecutionTrace,true); //the true will append the new data
		    fw.write(input + "\n");//appends the string to the file
		    fw.close();

			LOG.info("Successfully wrote {} to {} file" , input, seleniumExecutionTrace);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		System.out.println("sendKeys: " + input);

		return input;
	}	
	
	
	private void closeFile(){
		try {
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}		
	}
	

}
