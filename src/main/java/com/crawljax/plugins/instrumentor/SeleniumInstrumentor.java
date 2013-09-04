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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.plugins.utils.CompilationUnitUtils;


public class SeleniumInstrumentor {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeleniumInstrumentor.class);

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

}
