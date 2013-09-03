package com.crawljax.plugins.utils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * JSFunctionInfo is used to store JavaScript functions information including function name, js file name, DOM accessing statements, and caller functions/scopes
 * 
 * @author Amin Milani Fard
 */
public class JSFunctionInfo implements Serializable{
	private String functionName;
	private String jsFileName;
	private ArrayList<String> parameters = new ArrayList<String>();
	private ArrayList<String> domAccessStatements = new ArrayList<String>();	// keeping statements that interact with the DOM
	private ArrayList<String> calledFunctions = new ArrayList<String>();		// keeping name of the functions that were called by this function
	
	public JSFunctionInfo(String functionName, String jsFileName, ArrayList<String> parameters) {
		this.functionName = functionName;
		this.jsFileName = jsFileName;
		this.parameters = parameters;
	}

	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public String getJsFileName() {
		return jsFileName;
	}

	public void setJsFileName(String jsFileName) {
		this.jsFileName = jsFileName;
	}

	public ArrayList<String> getParameters() {
		return parameters;
	}

	public void setParameters(ArrayList<String> parameters) {
		this.parameters = parameters;
	}

	public ArrayList<String> getDomAccessStatements() {
		return domAccessStatements;
	}

	public void setDomAccessStatements(ArrayList<String> domAccessStatements) {
		this.domAccessStatements = domAccessStatements;
	}

	public void addDomAccessStatements(String statement) {
		this.domAccessStatements.add(statement);
	}
	
	public ArrayList<String> getCalledFunctions() {
		return calledFunctions;
	}

	public void setCalledFunctions(ArrayList<String> callerFunctions) {
		this.calledFunctions = callerFunctions;
	}

	public void addCalledFunction(String called) {
		this.calledFunctions.add(called);
	}

	@Override
	public String toString() {
		return "JSFunctionInfo [functionName=" + functionName + ", jsFileName="
				+ jsFileName + ", parameters=" + parameters
				+ ", domAccessStatements=" + domAccessStatements
				+ ", calledFunctions=" + calledFunctions + "]";
	}
	

}
