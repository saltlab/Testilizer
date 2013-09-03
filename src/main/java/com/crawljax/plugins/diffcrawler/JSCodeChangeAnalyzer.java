package com.crawljax.plugins.diffcrawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;
import org.w3c.dom.Node;

import com.crawljax.plugins.utils.EventFunctionRelation;
import com.crawljax.plugins.utils.JSFunctionInfo;

/**
 * This is the JavaScript code change analyzer class. It applies simple string-based comparison on DOM accessing statements within each function  
 * 
 * @author Amin Milani Fard
 */
public class JSCodeChangeAnalyzer {

	private AstNode ASTNode;

	private static ArrayList<EventFunctionRelation> jsFunctions = new ArrayList<EventFunctionRelation>();

	private static String jsFileName;

	public JSCodeChangeAnalyzer() {
		ASTNode = null;
	}
	
	public void SetASTNode(AstNode node) {
		ASTNode = node;
	}

	// Applying string-based comparison on DOM accessing statements to find affected functions that needs to be re-executed
	public ArrayList<JSFunctionInfo> getAffectedFunctions(ArrayList<JSFunctionInfo> oldVersionFunctions, ArrayList<JSFunctionInfo> newVersionFunctions) {
		// TODO: complete this method
		
		ArrayList<JSFunctionInfo> affectedFunctions = new ArrayList<JSFunctionInfo>();
		
		return affectedFunctions;
	}
	
	/**
	 * Analysing ASTNode for DOM accessing statements.
	 */
	public void analyseAstNode() {
	
		String ASTNodeName = ASTNode.shortName();
		int type = ASTNode.getType();
		int ASTDepth = ASTNode.depth();
		
		//System.out.println("node.shortName() : " + ASTNodeName);
		//System.out.println("node.depth() : " + ASTDepth);
		//System.out.println("node.getLineno() : " + (ASTNode.getLineno()+1));
		
		if (ASTNodeName.equals("Name")){
			//System.out.println(ASTNode.debugPrint());

			for (Symbol s: ASTNode.getAstRoot().getSymbols()){
				int sType = s.getDeclType();
			    if (sType == Token.LP || sType == Token.VAR || sType == Token.LET || sType == Token.CONST){
			    	System.out.println("global detected: " + s.getName());
			    }
			}
			//System.out.println();
		}
//		else if (ASTNodeName.equals("FunctionNode")){
//			FunctionNode f = (FunctionNode) ASTNode;
//			for (Symbol s: f.getSymbols()){
//				int sType = s.getDeclType();
//			    if (sType == Token.LP || sType == Token.VAR || sType == Token.LET || sType == Token.CONST){
//			    	System.out.println("s.getName() : " + s.getName());
//			    }
//			}
//			
//			System.out.println(f.getSymbolTable());
//			System.out.println(f.getSymbols());
//		}        
		
		
		
		if (ASTNodeName.equals("Name"))
			analyseNameNode();
		else if (ASTNodeName.equals("VariableDeclaration"))
			analyseVariable();
		else if (ASTNodeName.equals("ObjectProperty"))
			analyseObjectPropertyNode();
		else if (ASTNodeName.equals("FunctionNode"))
			analyseFunctionNode();
		else if (ASTNodeName.equals("PropertyGet"))  // this is for inner function defined properties such as this.name = ...
			analysePropertyGetNode();
		else if (ASTNodeName.equals("FunctionCall"))
			analyseFunctionCallNode();


		//System.out.println();

		//System.out.println("node.toSource() : " + node.toSource());
		//System.out.println("node.getType() : " + node.getType());
		//System.out.println("node.getAstRoot() : " + node.getAstRoot());
		//System.out.println("node.debugPrint() : " + node.debugPrint());
	}


	// detecting local variable
	private void analyseVariable() {
	}


	/**
	 * checking if name is the name of an object, function, property, etc.
	 */
	public void analyseNameNode() {
	}


	public void analyseObjectPropertyNode() {
	}


	/**
	 * Extracting objects created by new keyword in javaScript
	 */
	public void analyseFunctionNode() {
	
		FunctionNode f = (FunctionNode) ASTNode;

		String fName = "";
		if (f.getFunctionName()!=null){
			fName = f.getFunctionName().getIdentifier();
		}
		
		int numOfParam = f.getParams().size();
		int lineNumber = ASTNode.getLineno()+1;
		int fLength = f.getEndLineno() - f.getLineno();
		int fDepth = ASTNode.depth();
		
		//System.out.println(f.debugPrint());


		
	}	


	public void analysePropertyGetNode() {
		// nextName would be properties such as this.name = ... defined in a function 
	}


	public void analyseFunctionCallNode() {

		FunctionCall fcall = (FunctionCall) ASTNode;
		//System.out.println(ASTNode.debugPrint());

		// check for callback
		boolean detected = false;
		for (AstNode node : fcall.getArguments())
			if (node.shortName().equals("FunctionNode")){
				//System.out.println("callback found at line : " + ( ASTNode.getLineno()+1));
			}
	}


	
	// just some parsing to get the identifier in front of the NAME
	private String getName(String astDebugFormat, int j) {
		String Name = "";
		j++; while (astDebugFormat.charAt(j) != ' ') // skipping numbers
			j++;
		j++; while (astDebugFormat.charAt(j) != ' ') // skipping numbers
			j++;
		j++;
		Name = "";
		while (astDebugFormat.charAt(j) != '\n'){ // adding node type to the result string
			Name+=astDebugFormat.charAt(j);
			j++;
		}		
		return Name;
	}


	/**
	 * Analysing inline scripts
	 */
	public static void analyseInline(String scopeName, String code, HashSet<String> jsInTag) {
		//System.out.println("Inline JavaScript found inside HTML: " + code);
		
		//for (String jTag: jsInTag)
		//	jsInTagFound.add(jTag);
		
		//if (!inlineJavaScriptScopeName.contains(scopeName)){
		//	inlineJavaScriptScopeName.add(scopeName);
		//}
		
		// TODO: analyze inline
	}


	public static void setJSName(String jsName) {
		JSCodeChangeAnalyzer.jsFileName = jsName;
	}



}



