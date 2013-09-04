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
//import com.crawljax.plugins.jsmodify.AstInstrumenter;
//import com.crawljax.plugins.jsmodify.JSModifyProxyPlugin;
import com.crawljax.plugins.utils.EventFunctionRelation;
import com.crawljax.plugins.utils.JSFunctionInfo;
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

	private final ConcurrentMap<String, StateVertex> visitedStates;
	private boolean warnedForElementsInIframe = false;

	
	// The event-function relation table EFT stores which functions were executed as a result of firing an event
	private ArrayList<EventFunctionRelation> newEFT = new ArrayList<EventFunctionRelation>();

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

		File folder = new File("/Users/aminmf/testsuiteextension-plugin/src/main/java/casestudies/originaltests/");
		
		// TODO: Instrumenting unit test files
		LOG.info("Instrumenting unit test files...");
		
		
		
		// Compiling the instrumented unit test files
		LOG.info("Compiling the instrumented unit test files located in {}", folder.getAbsolutePath());

		try {
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
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TODO: Executing the instrumented unit test files and logging the execution trace
		LOG.info("Executing the instrumented unit test files and logging the execution trace...");	
		
		// TODO: Re-executing based on the execution log to generate initial paths for the SFG
		LOG.info("Re-executing based on the execution log to generate initial paths for the SFG...");	
		
		// TODO: Creating a SFG with initial paths based on executed instrumented code
		LOG.info("Creating a SFG with initial paths based on executed instrumented code...");
		
	}


	
	/**
	 * Executes happy paths only once after the index state was created.
	 */
	@Override
	public void initialPathExecution(CrawljaxConfiguration conf, CrawlTaskConsumer firstConsumer) {
		WebElement webElement = null;
		LOG.debug("Setting up initial crawl paths from test cases");

		// Reseting the crawler before each test case
		firstConsumer.getCrawler().reset();

		browser = firstConsumer.getContext().getBrowser();
		config = conf;
		
		// This comes from Selenium test cases
		webElement = browser.getBrowser().findElement(By.id("login"));
		webElement.clear();
		webElement = browser.getBrowser().findElement(By.id("login"));
		webElement.sendKeys("nainy");
		webElement = browser.getBrowser().findElement(By.id("password"));
		webElement.clear();
		webElement = browser.getBrowser().findElement(By.id("password"));
		webElement.sendKeys("nainy");
		webElement = browser.getBrowser().findElement(By.cssSelector("button[type=\"submit\"]"));

		// generate corresponding Eventable for webElement
		Eventable event = getCorrespondingEventable(webElement, EventType.click, browser);

		// This comes from Selenium test cases
		webElement.click();

		// inspecting DOM changes and adding to SFG
		firstConsumer.getCrawler().inspectNewState(event);

		// This comes from Selenium test cases
		webElement = browser.getBrowser().findElement(By.linkText("Logout"));

		// generate corresponding Eventable for webElement
		event = getCorrespondingEventable(webElement, EventType.click, browser);

		webElement.click();

		firstConsumer.getCrawler().inspectNewState(event);

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
		
		// Save the EFT to file
		String eftFileName = "eft.ser";
		try {
			fos = new FileOutputStream(eftFileName);
			out = new ObjectOutputStream(fos);
			out.writeObject(newEFT);
			out.close();
			LOG.info("TestSuiteExtension successfully wrote EFT to eft.ser file");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
			
		// Read the EFT from file for testing
		/*ArrayList<EventFunctionRelation> EFT2 = new ArrayList<EventFunctionRelation>();
		try {
			fis = new FileInputStream(eftFileName);
			in = new ObjectInputStream(fis);
			EFT2 = (ArrayList<EventFunctionRelation>) in.readObject();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		LOG.info("EFT2: " + EFT2);
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

		// Create an event-function relation and add it to the EFT table
		EventFunctionRelation newRelation = new EventFunctionRelation(eventable, stateBefore, stateAfter, executedJSFunctions);
		newEFT.add(newRelation);
	}



}
