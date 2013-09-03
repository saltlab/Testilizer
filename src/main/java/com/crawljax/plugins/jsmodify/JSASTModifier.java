package com.crawljax.plugins.jsmodify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that is used to define the interface and some functionality for the NodeVisitors
 * that modify JavaScript.
 */
public abstract class JSASTModifier implements NodeVisitor {

	private final Map<String, String> mapper = new HashMap<String, String>();
	
	private static final Logger LOG = LoggerFactory.getLogger(JSASTModifier.class);

	/**
	 * This is used by the JavaScript node creation functions that follow.
	 */
	private CompilerEnvirons compilerEnvirons = new CompilerEnvirons();

	/**
	 * Contains the scopename of the AST we are visiting. Generally this will be the filename
	 */
	private static String scopeName = null;

	// To store js corresponding name
	protected String jsName = null;

	private static Map<String, HashSet<String>> funcCallers = new HashMap<String, HashSet<String>>();
	
	/**
	 * @param scopeName
	 *            the scopeName to set
	 */
	public void setScopeName(String scopeName) {
		this.scopeName = scopeName;

		//Amin: This is used to name the array which stores execution count for the scope in URL 
		int index = scopeName.lastIndexOf('/');
		String s = scopeName.substring(index+1, scopeName.length());
		jsName = s.replace('.', '_');
	}
	
	/**
	 * @return the jsName
	 */
	public String getJSName() {
		return jsName;
	}
	
	/**
	 * @return the scopeName
	 */
	public String getScopeName() {
		return scopeName;
	}

	/**
	 * Abstract constructor to initialize the mapper variable.
	 */
	protected JSASTModifier() {
		/* add -<number of arguments> to also make sure number of arguments is the same */
		mapper.put("addClass", "attr('class')");
		mapper.put("removeClass", "attr('class')");
		mapper.put("css-2", "css(%0)");
		mapper.put("attr-2", "attr(%0)");
		mapper.put("prop-2", "attr(%0)");
		mapper.put("append", "text()");
		mapper.put("after", "parent().html()");
		mapper.put("appendTo", "html()");
		mapper.put("before","parent().html()");
		mapper.put("detach", "html()");
		mapper.put("remove", "html()");
		mapper.put("empty", "html()");
		mapper.put("height-1", "height()");
		mapper.put("width-1", "width()");
		mapper.put("insertBefore", "prev().html()");
		mapper.put("insertAfter", "next().html()");
		mapper.put("offset-1", "offset()");
		mapper.put("prepend", "html()");
		mapper.put("prependTo", "html()");
		mapper.put("html-1", "html()");
		mapper.put("setAttribute-2", "getAttribute(%0)");
		mapper.put("text-1", "text()");
	//	mapper.put("className", "className");
		mapper.put("addClass", "attr('class')");
		mapper.put("removeClass", "attr('class')");
		mapper.put("css-2", "css(%0)");
		mapper.put("attr-2", "attr(%0)");
		mapper.put("append", "html()");
	}
	
	
	/**
	 * Parse some JavaScript to a simple AST.
	 * 
	 * @param code
	 *            The JavaScript source code to parse.
	 * @return The AST node.
	 */
	public AstNode parse(String code) {
		//Parser p = new Parser(compilerEnvirons, null);
		compilerEnvirons.setErrorReporter(new ConsoleErrorReporter());
		Parser p = new Parser(compilerEnvirons, new ConsoleErrorReporter());

		//System.out.print(code+"*******\n");

		return p.parse(code, null, 0);
	}

	/**
	 * Find out the function name of a certain node and return "anonymous" if it's an anonymous
	 * function.
	 * 
	 * @param f
	 *            The function node.
	 * @return The function name.
	 */
	protected String getFunctionName(FunctionNode f) {
		Name functionName = f.getFunctionName();

		if (functionName == null) {
			return "anonymous" + f.getLineno();
		} else {
			return functionName.toSource();
		}
	}

	/**
	 * Creates a node that can be inserted at a certain point in function.
	 * 
	 * @param function
	 *            The function that will enclose the node.
	 * @param postfix
	 *            The postfix function name (enter/exit).
	 * @param lineNo
	 *            Linenumber where the node will be inserted.
	 * @return The new node.
	 */
	protected abstract AstNode createNode(FunctionNode function, String postfix, int lineNo);
	
	
	/**
	 * Creates a node that can be inserted at a certain point in the AST root.
	 * 
	 * @param root
	 * 			The AST root that will enclose the node.
	 * @param postfix
	 * 			The postfix name.
	 * @param lineNo
	 * 			Linenumber where the node will be inserted.
	 * @param rootCount
	 * 			Unique integer that identifies the AstRoot
	 * @return The new node
	 */
	protected abstract AstNode createNode(AstRoot root, String postfix, int lineNo, int rootCount);

	
	
	public static boolean innstrumentForCoverage(String URL) {		
		if (!URL.equals("http://127.0.0.1:8081/phormer331/"))
			if (scopeName.matches(".*min.*.js?.*")) {	// skip the code coverage for admin files
				return false;
			}
		return true;
	}
	
	
	/**
	 * JSNose version: AST actual node visiting method to detect code smells
	 * 
	 * @param node
	 *            The node that is currently visited.
	 * @return Whether to visit the children.
	 */	//@Override
	public boolean visit(AstNode node) {
		// Analyze AST for detecting code smells before JS code instrumentation
		//smellDetector.SetASTNode(node);
		//smellDetector.analyseAstNode();		
		
		// check if the file should be considered for coverage analysis
		if (innstrumentForCoverage(scopeName)==true)
			visitAndInstrument(node);
		
		return true;
	}
	
	
	/**
	 * Visiting and instrumenting the code
	 * 
	 * @param node
	 *            The node that is currently visited.
	 * @return Whether to visit the children.
	 */
	//@Override
	public boolean visitAndInstrument(AstNode node) {
		
		//int type = node.getType();
		//if (type == Token.NAME)
		//	System.out.println(node.);		
		
		//System.out.println(node.debugPrint());
		
		FunctionCall fcall;
		if (node instanceof FunctionCall){
			fcall = (FunctionCall) node;
			if (!fcall.getTarget().toSource().equals("$")){
				System.out.println("Function " + AstInstrumenter.jsFunctions.get(AstInstrumenter.jsFunctions.size()-1).getFunctionName() + " called " + fcall.getTarget().toSource());
				AstInstrumenter.jsFunctions.get(AstInstrumenter.jsFunctions.size()-1).addCalledFunction(fcall.getTarget().toSource());
			}
		}

		if (node instanceof ExpressionStatement) {
			if (isDOMAccessor(node.toSource())){
				//System.out.println("ExpressionStatement: " + node.toSource());
				String exp = node.toSource();
				exp = exp.replaceAll("\\s", "");	// removing all white spaces
				AstInstrumenter.jsFunctions.get(AstInstrumenter.jsFunctions.size()-1).addDomAccessStatements(exp);
			}
		}
		
		FunctionNode func;

		if (node instanceof FunctionNode) {
			func = (FunctionNode) node;
					
			/* instrument the code at the function entrance */
			AstNode newNode = createNode(func, ProgramPoint.ENTERPOSTFIX, func.getLineno());

			func.getBody().addChildToFront(newNode);

			node = (AstNode) func.getBody().getFirstChild();
			node = (AstNode) node.getNext(); //The first node is the node just added in front, so get next node

			//System.out.println(func.toSource());
		}
		else if (node instanceof AstRoot) {
			AstRoot rt = (AstRoot) node;

			if (rt.getSourceName() == null) { //make sure this is an actual AstRoot, not one we created
				return true;
			}

			/* instrument the code at the entry point of the AST root */
			m_rootCount++;
			AstNode newNode = createNode(rt, ProgramPoint.ENTERPOSTFIX, rt.getLineno(), m_rootCount);

			rt.addChildToFront(newNode);

			node = (AstNode) rt.getFirstChild();
			node = (AstNode) node.getNext(); //The first node is the node just added in front, so get next node	
		}
		return true;
	}

	/**
	 * A simple function to check DOM accessing statements in a JavaScript function
	 * @param func
	 * @return
	 */
	private boolean isDOMAccessor(String statement) {
		List<String> domAccessPatterns  = new ArrayList<String>();;

		domAccessPatterns.add(".getElement");
		domAccessPatterns.add("$(");

		for (String pattern : domAccessPatterns) {
			if (statement.contains(pattern)) {
				//System.out.println("Found DOM accesing statement (" + pattern + ") in statement: " + statement);
				return true;
			}
		}

		return false;
	}

	/**
	 * This method is called when the complete AST has been traversed.
	 * 
	 * @param node
	 *            The AST root node.
	 * @param jsName
	 *            The javascript scope name.	 
	 */
	public abstract void finish(AstRoot node);

	/**
	 * This method is called before the AST is going to be traversed.
	 */
	public abstract void start();
	
	private int m_rootCount = 0;
}
