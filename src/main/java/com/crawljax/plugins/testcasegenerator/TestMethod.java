package com.crawljax.plugins.testcasegenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Amin Milani Fard
 */
public class TestMethod {
	private String methodName;
	private ArrayList<String> statements = new ArrayList<String>();

	public TestMethod(String methodName) {
		this.methodName = methodName;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public ArrayList<String> getStatements() {
		return statements;
	}

	public void setStatements(ArrayList<String> statements) {
		this.statements = statements;
	}

	public void addStatement(String statement) {
		statements.add(statement);
	}

}
