package com.crawljax.plugins.testsuiteextension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.runner.JUnitCore;
import org.openqa.selenium.By;
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
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.configuration.CrawlElement;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.ExecuteInitialPathsPlugin;
import com.crawljax.core.plugin.OnFireEventSucceededPlugin;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnRevisitStatePlugin;
import com.crawljax.core.plugin.OnUrlLoadPlugin;
import com.crawljax.core.plugin.Plugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.plugin.PreCrawlingPlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.Eventable.EventType;
import com.crawljax.core.state.Identification.How;
import com.crawljax.forms.FormInput;
import com.crawljax.plugins.testsuiteextension.instrumentor.SeleniumInstrumentor;
//import com.crawljax.plugins.jsmodify.AstInstrumenter;
//import com.crawljax.plugins.jsmodify.JSModifyProxyPlugin;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XPathHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * TestSuiteExtension is Crawljax plugin tool which extends a current Selenium test suite of an Ajax application. 
 * It initiates the state-flow graph with Selenium test cases (happy paths) and crawl other paths around those happy paths.
 **/
public class TestSuiteExtension implements PreCrawlingPlugin, OnNewStatePlugin, PreStateCrawlingPlugin,
PostCrawlingPlugin, OnUrlLoadPlugin, OnFireEventSucceededPlugin, ExecuteInitialPathsPlugin, OnRevisitStatePlugin{
	
	private static final Logger LOG = LoggerFactory.getLogger(TestSuiteExtension.class);

	private EmbeddedBrowser browser = null;
	CrawljaxConfiguration config = null;
	
	private ArrayList<AssertedElementPattern> assertedElementPatterns = new ArrayList<AssertedElementPattern>();
	
	
	public TestSuiteExtension() {
		// TODO: initialization
		LOG.info("Initialized the TestSuiteExtension plugin");
	}


	/**
	 * Instrumenting Selenium test suite to get the execution trace
	 */
	@Override
	public void preCrawling(CrawljaxConfiguration config) {
		LOG.info("TestSuiteExtension plugin started");

		// Bypassing instrumenting and getting exec trace if already done
		if(true)
			return;
		
		
		SeleniumInstrumentor SI = new SeleniumInstrumentor();

		try {
			/**
			 * (1) Instrumenting original Selenium unit test files
			 */
			String originalFolderLoc = System.getProperty("user.dir");
			// On Linux/Mac
			//folderLoc += "/src/main/java/com/crawljax/plugins/testsuiteextension/casestudies/originaltests/";
			// On Windows
			originalFolderLoc += "\\src\\main\\java\\com\\crawljax\\plugins\\testsuiteextension\\casestudies\\originaltests\\";

			File originalFolder = new File(originalFolderLoc);
			LOG.info("originalFolderLoc: {} " , originalFolderLoc);

			File[] listOfOriginalFiles = originalFolder.listFiles(new FilenameFilter() {
				public boolean accept(File file, String name) {
					return name.endsWith(".java");
				}
			});


			for (File file : listOfOriginalFiles) {
				if (file.isFile()) {
					LOG.info("file.getName(): {}", file.getName());
					SI.instrument(file);
					//break; // instrument only one file...
				}
			}
			

			/**
			 * (2) Compiling the instrumented Selenium unit test files
			 */
			String instrumentedFolderLoc = System.getProperty("user.dir");
			// On Linux/Mac
			//folderLoc += "/src/main/java/com/crawljax/plugins/testsuiteextension/casestudies/instrumentedtests/";
			// On Windows
			instrumentedFolderLoc += "\\src\\main\\java\\com\\crawljax\\plugins\\testsuiteextension\\casestudies\\instrumentedtests\\";

			File instrumentedFolder = new File(instrumentedFolderLoc);
			LOG.info("instrumentedFolderLoc: {}" , instrumentedFolderLoc);
			LOG.info("Compiling the instrumented unit test files located in {}", instrumentedFolder.getAbsolutePath());

			File[] listOfInstrumentedFiles = instrumentedFolder.listFiles(new FilenameFilter() {
				public boolean accept(File file, String name) {
					return name.endsWith(".java");
				}
			});

			LOG.info(System.getProperty("java.home"));
			//Not set on my Mac
			System.setProperty("java.home", "C:\\Program Files\\Java\\jdk1.7.0_05");

			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();			
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(listOfInstrumentedFiles));
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
			boolean success = task.call();

			LOG.info("success = {}", success);

			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				LOG.info("Error on line {} in {}", diagnostic.getLineNumber(), diagnostic.getSource().toString());
				System.out.println("Error on line " + diagnostic.getLineNumber() + " in " + diagnostic.getSource().toString());
			}    

			fileManager.close();

			// Not set on my Mac
			if(success){
				// Executing the instrumented unit test files. This will produce a log of the execution trace
				LOG.info("Instrumenting unit test files and logging the execution trace...");

				SeleniumInstrumentor.writeToSeleniumExecutionTrace("TestSuiteBegin");

				for (File file : listOfInstrumentedFiles) {
					if (file.isFile()) {
						System.out.println("Executing unit test: " + file.getName());
						System.out.println("Executing unit test in " + file.getAbsolutePath());
						LOG.info("Executing unit test in {}", file.getName());

						SeleniumInstrumentor.writeToSeleniumExecutionTrace("NewTestCase");

						executeUnitTest(file.getAbsolutePath());
					}
					//break; // just to instrument and run one testcase...
				}
			}

			SeleniumInstrumentor.writeToSeleniumExecutionTrace("TestSuiteEnd");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void executeUnitTest(String test) {
		try {
			String fileName = "com.crawljax.plugins.testsuiteextension." + getFileFullName(test);
			System.out.println("Executing test class: " + fileName);
			Class<?> forName = Class.forName(fileName);
			JUnitCore.runClasses(forName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static String getFileFullName(String file) {
		file = file.replace(System.getProperty("user.dir"), "");
		file = file.replace("/src/main/java/com/crawljax/plugins/testsuiteextension/", "");
		// handling windows format
		file = file.replace("\\src\\main\\java\\com\\crawljax\\plugins\\testsuiteextension\\", "");
		file = (file.contains(".")) ? file.substring(0, file.indexOf(".")) : file;
		file = file.replace("/", ".");
		file = file.replace("\\", ".");
		return file;
	}


	/**
	 * Executes happy paths only once after the index state was created.
	 */
	@Override
	public void initialPathExecution(CrawljaxConfiguration conf, CrawlTaskConsumer firstConsumer) {
		//if (true)
		//	return;
		
		browser = firstConsumer.getContext().getBrowser();
		config = conf;

		// Re-executing based on the execution log to generate initial paths for the SFG
		LOG.info("Re-executing Selenium commands based on the execution log to generate initial paths for the SFG...");	

		WebElement webElement = null;
		Eventable event = null;

		ArrayList<String> trace = SeleniumInstrumentor.readFromSeleniumExecutionTrace();

		CopyOnWriteArrayList<FormInput> relatedFormInputs = new CopyOnWriteArrayList<FormInput>();

		How how = null;
		String howValue = null;

		for (String st: trace){
			// read the value such as id, cssSelector, xpath, and etc. 
			ArrayList<String> methodValue = new ArrayList<String>();


			if (st.equals("TestSuiteBegin"))
				continue; // ignoring for now, may be considered in future

			if (st.equals("TestSuiteEnd"))
				break; // terminating the execution of happy paths

			if (st.equals("NewTestCase"))
				firstConsumer.getCrawler().reset();

			else{
				methodValue = getMethodValue(st);
				System.out.println("method: " + methodValue.get(0));
				if (methodValue.size()==2)
					System.out.println("value: " + methodValue.get(1));
								
				switch (methodValue.get(0)){
					case "id:":
						how = Identification.How.id;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.id(howValue));
						break;
					case "name:":
						how = Identification.How.name;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.name(howValue));
						break;
					case "xpath:":
						how = Identification.How.xpath;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.xpath(howValue));
						break;
					case "tag name:":
						how = Identification.How.tag;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.tagName(howValue));
						break;
					case "class name:":
						how = Identification.How.name;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.className(howValue));
						break;
					case "css selector:":
						how = Identification.How.cssSelector;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.cssSelector(howValue));
						break;
					case "link text:":
						how = Identification.How.text;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.linkText(howValue));
						break;
					case "partial link text:":
						how = Identification.How.partialText;
						howValue = methodValue.get(1);
						webElement = browser.getBrowser().findElement(By.partialLinkText(howValue));
						break;
					case "clear":
						// changed to do nothing. clear() would be called later when filling the form 
						//if (webElement!=null)
							//webElement.clear();
						break;
					case "sendKeys":
						// storing input values for an element to be clicked later
						String inputValue = methodValue.get(1);

						// setting form input values for the Eventable
						//System.out.println("adding " + inputValue + " to inputs");
						relatedFormInputs.add(new FormInput(webElement.getTagName() , new Identification(how, howValue), inputValue));
						//System.out.println("relatedFormInputs: " + relatedFormInputs);

						//if (webElement!=null)
						//webElement.sendKeys(methodValue.get(1));
						break;
					case "click":
						if (webElement!=null){
							// generate corresponding Eventable for webElement
							event = getCorrespondingEventable(webElement, EventType.click, browser);
							
							//System.out.println("setting form inputs with: " + relatedFormInputs);
							
							event.setRelatedFormInputs(relatedFormInputs);

							CopyOnWriteArrayList<FormInput> formInputsToCheck = event.getRelatedFormInputs();

							System.out.println("formInputsToCheck: " + formInputsToCheck);							

							firstConsumer.getCrawler().handleInputElements(event);
							firstConsumer.getCrawler().waitForRefreshTagIfAny(event);

							boolean fired = firstConsumer.getCrawler().fireEvent(event);

							if (fired){
								// inspecting DOM changes and adding to SFG
								firstConsumer.getCrawler().inspectNewState(event);
							}
							else
								LOG.info("webElement {} not clicked because not all crawl conditions where satisfied",	webElement);



							// Applying the click with the form input values
							//webElement.click();

							// clearing the relatedFormInputs and inputValues to be set for the next click
							//relatedFormInputs.clear();
						}
						break;
					case "asserton":
						// TODO: not yet accomplished in instrumentation step
						String assertion = methodValue.get(1);
						// we have not seen the element yet...
						AssertedElementPattern aep = new AssertedElementPattern(sourceElement, assertion);
						assertedElementPatterns.add(aep);
						System.out.println(aep);
						
						firstConsumer.getContext().getCurrentState().addAssertedElementPattern(assertion);
						break;
					default:
				}
			}			
		}

		LOG.info("Initial paths on the SFG was created based on executed instrumented code...");
		
		CrawlSession session = firstConsumer.getContext().getSession();
		saveSFG(session);
		
	}

	private void saveSFG(CrawlSession session) {
		LOG.info("Saving the SFG based on executed Selenium test cases...");
		StateFlowGraph sfg = session.getStateFlowGraph();

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

	}



	// TODO: This method should be rafactored later
	/**
	 * Returning method with value
	 * Transforming [[FirefoxDriver: firefox on XP (4fd65769-108b-450b-8511-298d1bf67632)] -> css selector: button[type="submit"]]
	 * to <"css selector:", "button[type="submit"]">
	 */
	private ArrayList<String> getMethodValue(String s){
		String[] withParameter = {"id:", "name:", "xpath:", "tag name:", "class name:", "css selector:", "link text:", "partial link text:", 
				"sendKeys"};

		String[] withoutParameter = {"clear", "click"}; 
		
		ArrayList<String> methodValue = new ArrayList<String>();
		String value = null;
		int startIndexOfValue, endIndexOfValue = 0;
		for (int i=0; i<withParameter.length; i++){
			if (s.contains(withParameter[i])){
				startIndexOfValue = s.indexOf(withParameter[i]) + withParameter[i].length() + 1;
				if (withParameter[i].equals("sendKeys"))
					endIndexOfValue = s.length();
				else
					endIndexOfValue = s.length()-1;

				value = s.substring(startIndexOfValue, endIndexOfValue);
				methodValue.add(withParameter[i]);
				break;
			}
		}

		for (int i=0; i<withoutParameter.length; i++){
			if (s.contains(withoutParameter[i])){
				value = "";
				methodValue.add(withoutParameter[i]);
				break;
			}
		}
		
		methodValue.add(value);
		return methodValue;
	}


	
	private Eventable getCorrespondingEventable(WebElement webElement, EventType eventType, EmbeddedBrowser browser) {
		CandidateElement candidateElement = getCorrespondingCandidateElement(webElement, browser);
		Eventable event = new Eventable(candidateElement, eventType);
		System.out.println(event);
		return event;
	}

	private CandidateElement getCorrespondingCandidateElement(WebElement webElement, EmbeddedBrowser browser) {
		Document dom;
		try {
			dom = DomUtils.asDocument(browser.getStrippedDomWithoutIframeContent());

			for (CrawlElement crawlTag : config.getCrawlRules().getAllCrawlElements()) {
				// checking all tags defined in the crawlRules
				NodeList nodeList = dom.getElementsByTagName(crawlTag.getTagName());

				//String xpath1 = getXPath(webElement);
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
						
						// TODO: should only add asserted elements
						// creating an AssertedElementPattern from sourceElement
						String assertion = "";
						AssertedElementPattern aep = new AssertedElementPattern(sourceElement, assertion);
						assertedElementPatterns.add(aep);
						System.out.println(aep);
						
						
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
	
	public void addToAssertedElementPatterns(AssertedElementPattern aep){
		
		// TODO: check availability for later tme...
		/*
		// check if aep structure matches one in the assertedElementPatterns list
		for (AssertedElementPattern a: assertedElementPatterns){
			if (a.matchPatternStructure(aep)){
				// aep structure already exist in the assertedElementPatterns list, add assertion and inc the count
				a.increaseCount();
				a.addAssertion(aep.getAssertion().get(0)); // aep has only one assertion at this point
				return;
			}
		}
		*/
		assertedElementPatterns.add(aep);
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


	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitStatus) {
		
		for (AssertedElementPattern	aep: assertedElementPatterns)
			System.out.println(aep);
		
		LOG.info("TestSuiteExtension plugin has finished");
	}

	@Override
	public String toString() {
		return "TestSuiteExtension plugin";
	}

	@Override
	public void onUrlLoad(CrawlerContext context) {
		// TODO Reset for crawling from states in the happy paths
	}

	/**
	 * After a successful event firing, calculate the code coverage
	 */
	@Override
	public void onFireEvent(CrawlerContext context, StateVertex stateBefore,
			Eventable eventable, StateVertex stateAfter) {

		ArrayList<String> executedJSFunctions = new ArrayList<String>();
		executedJSFunctions.clear();

		// TODO: calculate code coverage for feedback-directed exploration
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

	
	/**
	 * Checking assertions on happy paths from existing test suite on new paths
	 * Currently dealing with assertions that are DOM related. We do not consider 
	 * those using variables in test cases for simplicity at this step.
	 */
	@Override
	public void onNewState(CrawlerContext context, StateVertex vertex) {
		// TODO: check the assertions from test suite
	}

	@Override
	public void onRevisitState(CrawlerContext context, StateVertex currentState) {
		// TODO: check the assertions from test suite
	}


}
