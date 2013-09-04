package com.crawljax.plugins.testsuiteextension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.runner.JUnitCore;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlTaskConsumer;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.configuration.CrawlElement;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.ExecuteInitialPathsPlugin;
import com.crawljax.core.plugin.OnFireEventFailedPlugin;
import com.crawljax.core.plugin.OnFireEventSucceededPlugin;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnUrlLoadPlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.plugin.PreCrawlingPlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.Eventable.EventType;
import com.crawljax.plugins.utils.Utils;
//import com.crawljax.plugins.jsmodify.AstInstrumenter;
//import com.crawljax.plugins.jsmodify.JSModifyProxyPlugin;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XPathHelper;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * TestSuiteExtension is Crawljax plugin tool which extends a current Selenium test suite of an Ajax application. 
 * It initiates the state-flow graph with Selenium test cases (happy paths) and crawl other paths around those happy paths.
 **/
public class TestSuiteExtension implements PreCrawlingPlugin, OnNewStatePlugin, PreStateCrawlingPlugin,
PostCrawlingPlugin, OnFireEventFailedPlugin, OnUrlLoadPlugin, OnFireEventSucceededPlugin, ExecuteInitialPathsPlugin{

	private static final Logger LOG = LoggerFactory.getLogger(TestSuiteExtension.class);

	public static final String DISTANCES_ARRAY = "Distances.csv";
	public static final String[] seleniumDomRelatedMethodCallList = new String[] { "findElement" };
	public static final String[] ELEMENTS_NOT_COUNT = new String[] { "p", "form", "tbody", "tabular", "span", "thead", "h1", "h2", "h3", "br", "hr", "code", "i", "kbd", "pre", "small", "strong", "abbr", "ul", "ol", "dl", "th", "select" };
	public static final String[] ELEMENTS_TO_COUNT = new String[] { "div", "input", "a", "li", "td", "tr", "table", "option", "img", "dt", "iframe", "textarea" };
	public static final String INJECT_ELEMENT_ACCESS_JS = "elementaccessinject.js";

	private final ConcurrentMap<String, StateVertex> visitedStates;
	private boolean warnedForElementsInIframe = false;

	

	//private StateFlowGraph oldSFG = null;

	private EmbeddedBrowser browser = null;
	CrawljaxConfiguration config = null;
	
	public TestSuiteExtension(File outputFolder) {
		Preconditions
		.checkNotNull(outputFolder, "Output folder cannot be null");
		visitedStates = Maps.newConcurrentMap();
		LOG.info("Initialized the TestSuiteExtension plugin");
	}

	
	/**
	 * Initializing the SFG with Selenium test cases
	 */
	@Override
	public void preCrawling(CrawljaxConfiguration config) {
		LOG.info("TestSuiteExtension plugin started");

		try {
			File folder = new File("/Users/aminmf/testsuiteextension-plugin/src/main/java/casestudies/originaltests/");

			// TODO: Instrumenting unit test files and save them in another folder
			LOG.info("Instrumenting unit test files...");
			/**
			 * The pattern to be saved in the log file is as following:
			 * "TestSuiteBegin"
			 * "NewTestCase"
			 * By.id("login")
			 * clear, senkeys, ....
			 * ...
			 * "NewTestCase"
			 * 
			 * "TestSuiteEnd"
			 */




			// Compiling the instrumented unit test files
			LOG.info("Compiling the instrumented unit test files located in {}", folder.getAbsolutePath());

			File[] listOfFiles = folder.listFiles(new FilenameFilter() {
		                  public boolean accept(File file, String name) {
		                      return name.endsWith(".java");
		                  }
		              });

			for (File file : listOfFiles) {
			    if (file.isFile()) {
					LOG.info(file.getName());
			    }
			}
			
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(listOfFiles));
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
			boolean success = task.call();

			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				LOG.info("Error on line {} in {}", diagnostic.getLineNumber(), diagnostic.getSource().toString());
			}    

			fileManager.close();
		
			// Executing the instrumented unit test files. This will produce a log of the execution trace
			LOG.info("Executing the instrumented unit test files and logging the execution trace...");
			for (File file : listOfFiles) {
			    if (file.isFile()) {
			    	executeUnitTest(file.getName());
					LOG.info("Executing unit test in {}", file.getName());
			    }
			}

		
		
		} catch (IOException e) {
			e.printStackTrace();
		}

		
				
	}


	public void executeUnitTest(String fileName) {
		try {
			Class<?> forName = Class.forName(fileName);
			JUnitCore.runClasses(forName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Executes happy paths only once after the index state was created.
	 */
	@Override
	public void initialPathExecution(CrawljaxConfiguration conf, CrawlTaskConsumer firstConsumer) {

		browser = firstConsumer.getContext().getBrowser();
		config = conf;

		// Re-executing based on the execution log to generate initial paths for the SFG
		LOG.info("Re-executing Selenium commands based on the execution log to generate initial paths for the SFG...");	


		WebElement webElement = null;
		Eventable event = null;
		// TODO: Reading from the log file...
		String command = null;
		while(!command.equals("TestSuiteEnd")){
			if (command.equals("NewTestCase")){
				// Reseting the crawler before each test case
				firstConsumer.getCrawler().reset();
			}
			// read the value such as id, cssSelector, xpath, and etc. 
			String value = null;
			switch (command){
				case "By.id":
					webElement = browser.getBrowser().findElement(By.id(value));
					break;
				case "By.name":
					webElement = browser.getBrowser().findElement(By.name(value));
					break;
				case "By.cssSelector":
					webElement = browser.getBrowser().findElement(By.cssSelector(value));
					break;
				case "By.linkText":
					webElement = browser.getBrowser().findElement(By.linkText(value));
					break;
				case "clear":
					webElement.clear();
					break;
				case "sendKeys":
					webElement.sendKeys(value);
					break;
				case "click":
					webElement.sendKeys(value);
					// generate corresponding Eventable for webElement
					event = getCorrespondingEventable(webElement, EventType.click, browser);
					webElement.click();
					// inspecting DOM changes and adding to SFG
					firstConsumer.getCrawler().inspectNewState(event);
					break;
				default:
			}


		}

		
		// This comes from Selenium test cases
		/*webElement = browser.getBrowser().findElement(By.id("login"));
		webElement.clear();
		webElement = browser.getBrowser().findElement(By.id("login"));
		webElement.sendKeys("nainy");
		webElement = browser.getBrowser().findElement(By.id("password"));
		webElement.clear();
		webElement = browser.getBrowser().findElement(By.id("password"));
		webElement.sendKeys("nainy");
		webElement = browser.getBrowser().findElement(By.cssSelector("button[type=\"submit\"]"));

		Eventable event = getCorrespondingEventable(webElement, EventType.click, browser);

		webElement.click();

		firstConsumer.getCrawler().inspectNewState(event);
		*/

		LOG.info("Initial paths on the SFG was created based on executed instrumented code...");
	}
	
	//Amin
	private Eventable getCorrespondingEventable(WebElement webElement, EventType eventType, EmbeddedBrowser browser) {
		CandidateElement candidateElement = getCorrespondingCandidateElement(webElement, browser);
		Eventable event = new Eventable(candidateElement, eventType);
		System.out.println(event);
		return event;
	}

	//Amin
	private CandidateElement getCorrespondingCandidateElement(WebElement webElement, EmbeddedBrowser browser) {
		Document dom;
		
		try {
			dom = DomUtils.asDocument(browser.getStrippedDomWithoutIframeContent());

			for (CrawlElement crawlTag : config.getCrawlRules().getAllCrawlElements()) {
				// checking all tags defined in the crawlRules
				NodeList nodeList = dom.getElementsByTagName(crawlTag.getTagName());

				String xpath1 = getXPath(webElement);
				String xpath2 = null;
				org.w3c.dom.Element sourceElement = null;

				for (int k = 0; k < nodeList.getLength(); k++){
					sourceElement = (org.w3c.dom.Element) nodeList.item(k);
					// check if sourceElement is webElement
					if (checkEqulity(webElement, sourceElement)){
						xpath2 = XPathHelper.getXPathExpression(sourceElement);
						// System.out.println("xpath : " + xpath2);
						CandidateElement candidateElement = new CandidateElement(sourceElement, new Identification(Identification.How.xpath, xpath2), "");
						LOG.debug("Found new candidate element: {} with eventableCondition {}",	candidateElement.getUniqueString(), null);
						candidateElement.setEventableCondition(null);
						return candidateElement;
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("could not find the corresponding CandidateElement");
		return null;
	}

	//Amin
	private boolean checkEqulity(WebElement webElement,	org.w3c.dom.Element sourceElement) {
		
		//get xpath of the WebElement
		String xpath1 = getXPath(webElement);

		System.out.println("WebElement: " + webElement);
		System.out.println("has xpath : " + xpath1);

		// check if xpaths are the same
		String xpath2 = XPathHelper.getXPathExpression(sourceElement);
		System.out.println("sourceElement: " + sourceElement);
		System.out.println("has xpath : " + xpath2);

		// removing "[1]" from xpath2 for consistency with the xpath1 format 
		xpath2 = xpath2.replace("[1]","");  

		if (xpath2.equals(xpath1)){
			System.out.println("xpaths are equal");
			return true;
		}
		
		System.out.println("xpaths are not equal");
		return false;
	}

	//Amin
	public String getXPath(WebElement element) {

		String jscript = "function getElementXPath(elt) " +   
				"{" + 
				"var path = \"\";" +
				"for (; elt && elt.nodeType == 1; elt = elt.parentNode)" + 
				"{" +        
				"idx = getElementIdx(elt);" + 
				"xname = elt.tagName;" +
				"if (idx > 1) xname += \"[\" + idx + \"]\";" + 
				"path = \"/\" + xname + path;" + 
				"}" +  
				"return path;" + 
				"} " + 
				"function getElementIdx(elt) "+ 
				"{"  + 
				"var count = 1;" + 
				"for (var sib = elt.previousSibling; sib ; sib = sib.previousSibling) " + 
				"{" + 
				"if(sib.nodeType == 1 && sib.tagName == elt.tagName) count++; " + 
				"} " +     
				"return count;" + 
				"} " +
				"return getElementXPath(arguments[0]);";

		String xpath = (String) browser.executeJavaScriptWithParam(jscript, element);
		return xpath;
	} 
	
	
	
	@Override
	public void onNewState(CrawlerContext context, StateVertex vertex) {
	}

	/**
	 * Logs all the candidate elements so that the plugin knows which elements were the candidate
	 * elements.
	 */
	@Override
	public void preStateCrawling(CrawlerContext context,
			ImmutableList<CandidateElement> candidateElements, StateVertex state) {
		LOG.debug("preStateCrawling");
		LOG.info("Prestate found new state {} with {} candidates",
				state.getName(), candidateElements.size());

	}

	private WebElement getWebElement(EmbeddedBrowser browser, CandidateElement element) {
		try {
			if (!Strings.isNullOrEmpty(element.getRelatedFrame())) {
				warnUserForInvisibleElements();
				return null;
			} else {
				return browser.getWebElement(element.getIdentification());
			}
		} catch (WebDriverException e) {
			LOG.info("Could not locate element for positioning {}", element);
			return null;
		}
	}

	private void warnUserForInvisibleElements() {
		if (!warnedForElementsInIframe) {
			LOG.warn("Some elemnts are in an iFrame. We cannot display it in the Crawl overview");
			warnedForElementsInIframe = true;
		}
	}

	/**
	 * Generated the report.
	 */
	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitStatus) {
		LOG.debug("postCrawling");
		StateFlowGraph sfg = session.getStateFlowGraph();
		
		// Writing event-function relation table, SFG, and jsFunctions to corresponding files
		FileOutputStream fos = null;
		ObjectOutputStream out = null;

		// Save the SFG to file
		String sfgFileName = "sfg.ser";
		try {
			fos = new FileOutputStream(sfgFileName);
			out = new ObjectOutputStream(fos);
			out.writeObject(sfg);
			out.close();
			LOG.info("TestSuiteExtension successfully wrote SFG to sfg.ser file");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// Read the SFG from file for testing
		/*StateFlowGraph sfg2 = null;
		try {
			fis = new FileInputStream(sfgFileName);
			in = new ObjectInputStream(fis);
			sfg2 = (StateFlowGraph) in.readObject();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		sfg2 = null;
		
		//LOG.info(Serializer.toPrettyJson(sfg));

		if (Serializer.toPrettyJson(sfg).equals(Serializer.toPrettyJson(sfg2)))
			LOG.info("ERROR!");
		 */
		
		LOG.info("TestSuiteExtension plugin has finished");
	}

	@Override
	public String toString() {
		return "TestSuiteExtension plugin";
	}

	@Override
	public void onFireEventFailed(CrawlerContext context, Eventable eventable,
			List<Eventable> pathToFailure) {
		return;
	}

	@Override
	public void onUrlLoad(CrawlerContext context) {
		// TODO Reset for crawling from states in the happy paths
	}

	/**
	 * After a successful event firing, calculate the code coverage
	 * 
	 * @param context
	 * @param stateBefore
	 * @param eventable
	 * @param stateAfter
	 */
	@Override
	public void onFireEvent(CrawlerContext context, StateVertex stateBefore,
			Eventable eventable, StateVertex stateAfter) {

		ArrayList<String> executedJSFunctions = new ArrayList<String>();
		executedJSFunctions.clear();

		// TODO: calculate code coverage
		/*for (String modifiedJS : JSModifyProxyPlugin.getModifiedJSList()){
			try{
				Object executedFunctions =  context.getBrowser().executeJavaScript("return " + modifiedJS + "_executed_functions;");
				ArrayList tempList = (ArrayList) executedFunctions;
				LOG.info("JS functions " + tempList + " were executed after firing the eventable " + eventable);
				for (int i=0; i<tempList.size(); i++){
					if (!((String)tempList.get(i)).equals(""))
						executedJSFunctions.add((String)tempList.get(i));
				}
			}catch (Exception e) {
				LOG.info("Could not execute script: " + "return " + modifiedJS + "_executed_functions;");
			}
		}*/

		//LOG.info(Serializer.toPrettyJson(AstInstrumenter.jsFunctions));

	}



}
