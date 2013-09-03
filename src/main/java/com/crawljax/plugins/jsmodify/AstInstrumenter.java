package com.crawljax.plugins.jsmodify;

import java.util.ArrayList;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.plugins.diffcrawler.model.Serializer;
import com.crawljax.plugins.jsmodify.JSASTModifier;
import com.crawljax.plugins.utils.JSFunctionInfo;

/**
 * This class is used to visit all JS nodes. When a node matches a certain condition, this class
 * will add instrumentation code near this code.
 */
public class AstInstrumenter extends JSASTModifier {

	private int instrumentedFunctionsCounter = 0;
	
	private static final Logger LOG = LoggerFactory.getLogger(AstInstrumenter.class);

	public static ArrayList<JSFunctionInfo> jsFunctions = new ArrayList<JSFunctionInfo>();

	
	public AstInstrumenter() {
		super();
	}

	/**
	 * This will be added to the beginning of the script
	 * 
	 * @return The AstNode which contains array.
	 */
	private AstNode jsFunctionExectutionCounter() {

		String code = "var " + jsName + "_executed_functions = new Array(); " +
				"for (var i=0;i<" + instrumentedFunctionsCounter + ";i++)" +
				"if("+jsName + "_executed_functions[i]== undefined || "+jsName + "_executed_functions[i]== null) "+jsName + "_executed_functions[i]=\"\";";
		
		// instrumentedFunctionsCounter resets to 0 for the next codes
		instrumentedFunctionsCounter = 0;
		//System.out.println(code);
		
		return parse(code);
	}

	@Override // instrumenting within a function
	protected AstNode createNode(FunctionNode function, String postfix, int lineNo) {
		String name = getFunctionName(function);
		String code = jsName + "_executed_functions[" + Integer.toString(instrumentedFunctionsCounter) + "] = \"" + name + "\";";
		
		//System.out.println("function " + function.getName() + " has parameters: ");
		ArrayList<String> parameters = new ArrayList<String>();
		for (AstNode a: function.getParams()){
			//System.out.println(a.toSource());
			parameters.add(a.toSource());
		}
		
		JSFunctionInfo jsf = new JSFunctionInfo(name, jsName, parameters);
		System.out.println(jsf);
		jsFunctions.add(jsf);
		
		instrumentedFunctionsCounter++;
		
		return parse(code);
	}

	@Override// instrumenting out of function
	protected AstNode createNode(AstRoot root, String postfix, int lineNo, int rootCount) {
		String code = jsName + "_executed_functions[" + Integer.toString(instrumentedFunctionsCounter) + "] = \"GlobalScope\";";
		
		JSFunctionInfo jsf = new JSFunctionInfo("GlobalScope", jsName, null);
		System.out.println(jsf);
		jsFunctions.add(jsf);
		
		instrumentedFunctionsCounter++;

		return parse(code);
	}

	@Override
	public void finish(AstRoot node) {
		// add initialization code for the count of executed functions array
		node.addChildToFront(jsFunctionExectutionCounter());
		// instrumentedFunctionsCounter resets to 0 for the next codes
		instrumentedFunctionsCounter = 0;
		
		LOG.info("jsFunctions are: " + jsFunctions);
	}

	@Override
	public void start() {
		// just to be sure that index start from 0
		instrumentedFunctionsCounter = 0;
	}	
}
