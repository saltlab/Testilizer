package com.crawljax.plugins.testsuiteextension.instrumentor;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.stmt.Statement;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.crawljax.plugins.utils.CompilationUnitUtils;
import com.crawljax.plugins.utils.MethodCallVisitor;


public class TestCaseParser {

	public static final String[] seleniumDomRelatedMethodCallList = new String[] { "sendKeys", "clear", "click" , "findElement", "assertTrue", "assertEquals", "assertNotNull", "assertNull" };

	public HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> getSeleniumDomRelateMethodCallExpressions(CompilationUnit cu) throws FileNotFoundException, ParseException, IOException {
		ArrayList<MethodDeclaration> testMethodsofCompilationUnit = CompilationUnitUtils.testMethodsofCompilationUnit(cu);
		return getMethodCallExpressions(testMethodsofCompilationUnit, seleniumDomRelatedMethodCallList);

	}
	
	private HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> getMethodCallExpressions(ArrayList<MethodDeclaration> testMethodsofCompilationUnit, String[] methodCalls) {
		HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> domRelateMethodCallExps = new HashMap<MethodDeclaration, ArrayList<MethodCallExpr>>();

		for (MethodDeclaration methodDeclaration : testMethodsofCompilationUnit) {
			// find location of all findelement, etc. methods
			MethodCallVisitor mcv = new MethodCallVisitor();
			ArrayList<String> elementsToCover = new ArrayList<String>();
			elementsToCover.addAll(Arrays.asList(methodCalls));
			mcv.applyFilter(elementsToCover);

			mcv.visit(methodDeclaration, null);

			domRelateMethodCallExps.put(methodDeclaration, mcv.getMethodCallExpressions());
			// instrument them
			// Utils.printArrayList( mcv.getMethodCallExpressions());
		}
		return domRelateMethodCallExps;
	}

	public HashMap<MethodDeclaration, ArrayList<MethodCallExpr>> getSeleniumDomRelateMethodCallExpressions(String fileName) throws FileNotFoundException, ParseException, IOException {
		CompilationUnit cu = getCompilationUnitOfFileName(fileName);
		return getSeleniumDomRelateMethodCallExpressions(cu);
	}
		
	public static CompilationUnit getCompilationUnitOfFileName(String fileName) throws FileNotFoundException, ParseException, IOException {
		FileInputStream in = new FileInputStream(fileName);

		CompilationUnit cu;
		try {
			// parse the file
			cu = JavaParser.parse(in);
		} finally {
			in.close();
		}
		return cu;
	}


	public ArrayList<MethodDeclaration> getTestMethodDeclaration(CompilationUnit cu) {
		return CompilationUnitUtils.testMethodsofCompilationUnit(cu);
	}
	

	public ArrayList<MethodCallExpr> getMethodCalls(MethodDeclaration methodDeclaration, String[] methodCalls) {
		// find location of all findelement, etc. methods
		MethodCallVisitor mcv = new MethodCallVisitor();
		ArrayList<String> elementsToCover = new ArrayList<String>();
		elementsToCover.addAll(Arrays.asList(methodCalls));
		mcv.applyFilter(elementsToCover);
		mcv.visit(methodDeclaration, null);
		return mcv.getMethodCallExpressions();
	}


}
