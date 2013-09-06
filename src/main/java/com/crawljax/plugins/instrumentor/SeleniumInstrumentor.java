package com.crawljax.plugins.instrumentor;

import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.MethodCallExpr;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

	
	public void instrument(String fileName) {
		LOG.info("Instrumenting the Selenium test cases in {}", fileName);
		instrument(fileName, true);
	}

	public void instrument(String fileName, boolean writeBack) {
		try {
			TestCaseParser tcp = new TestCaseParser();
			CompilationUnit cu;
			cu = TestCaseParser.getCompilationUnitOfFileName(fileName);

			HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> srmce = tcp.getSeleniumDomRelateMethodCallExpressions(cu);

			for (MethodDeclaration e : srmce.keySet()) {
				LOG.info("testcase: {}", e.getName());
				System.out.println("testcase: " + e.getName());
				for (MethodCallExpr mce : srmce.get(e)) {
					this.instrumentMethodCall(mce);
					System.out.println(mce);
				}
			}
			if (writeBack == true)
				CompilationUnitUtils.writeCompilationUnitToFile(cu, fileName);
			LOG.info("done writing");

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
		try {
			fos = new FileOutputStream(seleniumExecutionTrace);
			out = new ObjectOutputStream(fos);

			
			out.writeObject(by.toString());
			LOG.info("Successfully wrote {} to {} file" , by, seleniumExecutionTrace);
			System.out.println("Successfully wrote " + by + " to " + seleniumExecutionTrace);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return by;
	}
	
	// should be static to be used by instrumented test cases
	public static String getInput(String input) {
		try {

			fos = new FileOutputStream(seleniumExecutionTrace);
			out = new ObjectOutputStream(fos);
			
			out.writeObject(input);
			LOG.info("Successfully wrote {} to {} file" , input, seleniumExecutionTrace);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
