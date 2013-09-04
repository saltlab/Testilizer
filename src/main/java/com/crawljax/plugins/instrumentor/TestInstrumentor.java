package com.crawljax.plugins.instrumentor;

import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.MethodCallExpr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.crawljax.plugins.utils.CompilationUnitUtils;


public class TestInstrumentor {

	public void instrumentTestSuite(String folder) {
		List<String> allTests = TestUtil.getAllTests(folder);
		for (String test : allTests) {
			instrument(test);
		}
	}

	public MethodCallExpr instrumentMethodCall(MethodCallExpr mce) {

		// ASTHelper.

		List<Expression> oldArgs = mce.getArgs();
		// create a methodcall expre
		String codeToInstrument = "com.crawljax.plugins.instrumentor.DomCoverageClass.collectData";
		MethodCallExpr call = new MethodCallExpr(null, codeToInstrument);
		MethodCallExpr calltoPageSource = new MethodCallExpr(null, mce.getScope().toString() + ".getPageSource");
		MethodCallExpr calltoClassName = new MethodCallExpr(null, "this.getClass().getName()+\".\"+new Object(){}.getClass().getEnclosingMethod().getName");
		oldArgs.add(calltoPageSource);
		oldArgs.add(calltoClassName);

		call.setArgs(oldArgs);
		// put oldargs as it's argument

		// setarguments of mce
		List<Expression> newArgs = new ArrayList<Expression>();
		newArgs.add(call);
		mce.setArgs(newArgs);

		return mce;
	}

	public void instrument(String fileName) {
		instrument(fileName, true);
	}

	public void instrument(String fileName, boolean writeBack) {
		try {
			TestCaseParser tcp = new TestCaseParser();
			CompilationUnit cu;
			cu = TestCaseParser.getCompilationUnitOfFileName(fileName);

			HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> srmce = tcp.getSeleniumDomRelateMethodCallExpressions(cu);

			for (MethodDeclaration e : srmce.keySet()) {
				System.out.println("testcase: " + e.getName());
				for (MethodCallExpr mce : srmce.get(e)) {
					this.instrumentMethodCall(mce);
					System.out.println(mce);
				}
			}
			if (writeBack == true)
				CompilationUnitUtils.writeCompilationUnitToFile(cu, fileName);
			System.out.println("done writing");

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

}
