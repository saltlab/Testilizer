package com.crawljax.plugins.testsuiteextension;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;


import org.jgrapht.GraphPath;
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
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.ExecuteInitialPathsPlugin;
import com.crawljax.core.plugin.OnFireEventSucceededPlugin;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnRevisitStatePlugin;
import com.crawljax.core.plugin.OnUrlLoadPlugin;
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
import com.crawljax.forms.RandomInputValueGenerator;
import com.crawljax.plugins.testsuiteextension.jsinstrumentor.JSModifyProxyPlugin;
import com.crawljax.plugins.testsuiteextension.seleniuminstrumentor.SeleniumInstrumentor;
import com.crawljax.plugins.testsuiteextension.testcasegenerator.JavaTestGenerator;
import com.crawljax.plugins.testsuiteextension.testcasegenerator.TestMethod;
import com.crawljax.util.AssertedElementPattern;
//import com.crawljax.plugins.jsmodify.AstInstrumenter;
//import com.crawljax.plugins.jsmodify.JSModifyProxyPlugin;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XPathHelper;
import com.google.common.collect.ImmutableList;

/**
 * TestSuiteExtension is Crawljax plugin tool which extends a current Selenium test suite of an Ajax application. 
 * It initiates the state-flow graph with Selenium test cases (happy paths) and crawl other paths around those happy paths.
 **/
public class TestSuiteExtension implements PreCrawlingPlugin, OnNewStatePlugin, PreStateCrawlingPlugin,
PostCrawlingPlugin, OnUrlLoadPlugin, OnFireEventSucceededPlugin, ExecuteInitialPathsPlugin, OnRevisitStatePlugin{

	// Setting for experiments on DOM-based assertion generation part (default should be true)
	static boolean addNewAssertion = true;

	private static final Logger LOG = LoggerFactory.getLogger(TestSuiteExtension.class);

	private EmbeddedBrowser browser = null;
	CrawljaxConfiguration config = null;

	private ArrayList<AssertedElementPattern> originalAssertedElementPatterns = new ArrayList<AssertedElementPattern>();

	private boolean inAssertionMode = false;

	// Keeping track of executed lines of a JavaScript code for Feedex	
	private Map<String,ArrayList<Integer>> JSCountList = new Hashtable<String,ArrayList<Integer>>(); 

	private String finalReport ="";

	
	BufferedWriter outForLogging;

		
	
	public TestSuiteExtension() {
		try {
			outForLogging = new BufferedWriter(new FileWriter("Log.txt"));			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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

		/**
		 * (1) Instrumenting original Selenium unit test files
		 */
		//String appName = "claroline";
		String appName = "photogallery";

		String originalFolderLoc = System.getProperty("user.dir");
		// On Linux/Mac
		originalFolderLoc += "/src/main/java/com/crawljax/plugins/testsuiteextension/casestudies/" + appName + "/originaltests/";
		// On Windows
		//originalFolderLoc += "\\src\\main\\java\\com\\crawljax\\plugins\\testsuiteextension\\casestudies\\" + appName +"\\originaltests\\";

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
		instrumentedFolderLoc += "/src/main/java/com/crawljax/plugins/testsuiteextension/casestudies/" + appName + "/instrumentedtests/";
		// On Windows
		//instrumentedFolderLoc += "\\src\\main\\java\\com\\crawljax\\plugins\\testsuiteextension\\casestudies\\" + appName +"\\instrumentedtests\\";

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

		try {
			fileManager.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

					SeleniumInstrumentor.writeToSeleniumExecutionTrace("NewTestCase " + file.getName());

					executeUnitTest(file.getAbsolutePath());
				}
				//break; // just to instrument and run one testcase...
			}
		}

		SeleniumInstrumentor.writeToSeleniumExecutionTrace("TestSuiteEnd");

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

		int numOfTestCases = 0;

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
		String assertedElementLocator = null;

		for (String st: trace){
			// read the value such as id, cssSelector, xpath, and etc. 
			ArrayList<String> methodValue = new ArrayList<String>();

			if (st.equals("TestSuiteBegin")){
				System.out.println("TestSuiteBegin");
				continue; // ignoring for now, may be considered in future
			}

			if (st.equals("TestSuiteEnd"))
				break; // terminating the execution of happy paths

			if (st.contains("NewTestCase")){
				System.out.println(st);
				numOfTestCases++;
				firstConsumer.getCrawler().reset();
			}

			if (st.equals("reset")){
				System.out.println("Reseting to the index page...");
				firstConsumer.getCrawler().reset();
			}

			else{
				methodValue = getMethodValue(st);

				if (methodValue.get(0)==null)  // to bypass some generated By.id, etc. that should be considered within assertions
					continue;

				System.out.println("method: " + methodValue.get(0));
				if (methodValue.size()==2)
					System.out.println("value: " + methodValue.get(1));

				switch (methodValue.get(0)){
				case "Alert":
					System.out.println("Closing the alert!");
					// alert, prompt, and confirm behave as if the OK button is always clicked.
					browser.executeJavaScript("window.alert = function(msg){return true;};"
							+ "window.confirm = function(msg){return true;};"
							+ "window.prompt = function(msg){return true;};");
					break;
				case " id:":
					how = Identification.How.id;
					howValue = methodValue.get(1);						
					webElement = browser.getBrowser().findElement(By.id(howValue));
					break;
				case " name:":
					how = Identification.How.name;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.name(howValue));
					break;
				case " xpath:":
					how = Identification.How.xpath;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.xpath(howValue));
					break;
				case " tag name:":
					how = Identification.How.tag;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.tagName(howValue));
					break;
				case " class name:":
					how = Identification.How.name;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.className(howValue));
					break;
				case " css selector:":
					how = Identification.How.cssSelector;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.cssSelector(howValue));
					break;
				case " link text:":
					how = Identification.How.text;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.linkText(howValue));
					break;
				case " partial link text:":
					how = Identification.How.partialText;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.partialLinkText(howValue));
					break;
				case "clear":
					// Do nothing. clear() would be called later when filling the form 
					break;
				case "sendKeys":
					// storing input values for an element to be clicked later
					String inputValue = methodValue.get(1);

					// dealing with random input data
					if (inputValue.equals("$RandValue")){
						inputValue = "RND-" + new RandomInputValueGenerator().getRandomString(4);
						System.out.println("Random string " + inputValue + " generated for inputValue");
					}


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
						event = getCorrespondingEventable(webElement, new Identification(how, howValue), EventType.click, browser);


						System.out.println("event: " + event);

						/*String xpath = getXPath(webElement);
							try {

								String xpath2 = XPathHelper.getXPathExpression(getElementFromXpath(xpath, browser));
								System.out.println("fast Element found has xpath: " + xpath2);

								//System.out.println("Fast Element found is: " + getElementFromXpath(xpath, browser));
							} catch (XPathExpressionException e) {
								e.printStackTrace();
							}*/


						//System.out.println("setting form inputs with: " + relatedFormInputs);
						CopyOnWriteArrayList<FormInput> relatedFormInputsCopy = new CopyOnWriteArrayList<FormInput>();
						relatedFormInputsCopy.addAll(relatedFormInputs);

						//event.setRelatedFormInputs(relatedFormInputs);
						event.setRelatedFormInputs(relatedFormInputsCopy);

						CopyOnWriteArrayList<FormInput> formInputsToCheck = event.getRelatedFormInputs();

						//System.out.println("formInputsToCheck: " + formInputsToCheck);							

						firstConsumer.getCrawler().handleInputElements(event);
						firstConsumer.getCrawler().waitForRefreshTagIfAny(event);

						// Applying the click with the form input values
						boolean fired = firstConsumer.getCrawler().fireEvent(event);

						if (fired)
							// inspecting DOM changes and adding to SFG
							firstConsumer.getCrawler().inspectNewStateForInitailPaths(event);
						else
							LOG.info("webElement {} not clicked because not all crawl conditions where satisfied",	webElement);

						// clearing the relatedFormInputs and inputValues to be set for the next click
						if (howValue.contains("submit"))
							relatedFormInputs.clear();
					}
					break;

					/****** Cases for assertions ******/

				case "By.id:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.id(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.id(howValue));
					break;
				case "By.linkText:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.linkText(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.linkText(howValue));
					break;
				case "By.partialLinkText:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.partialLinkText(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.partialLinkText(howValue));
					break;
				case "By.name:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.name(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.name(howValue));
					break;
				case "By.tagName:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.tagName(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.tagName(howValue));
					break;
				case "By.xpath:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.xpath(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.xpath(howValue));
					break;
				case "By.className:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.className(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.className(howValue));
					break;
				case "By.selector:":
					howValue = methodValue.get(1);
					assertedElementLocator = "By.cssSelector(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.cssSelector(howValue));
					System.out.println("Found webElement: " + webElement);
					break;
				case "assertion":

					org.w3c.dom.Element assertedSourceElement = null;
					String assertion = methodValue.get(1);

					if (assertion.contains(".findElement")){	// only for assertions that access a DOM element

						System.out.println("methodValue.get(1): " + methodValue.get(1));
						//wait.until(ExpectedConditions.presenceOfElementLocated(By.linkText(linkText)));

						// The call to getCorrespondingCandidateElement() fills the lastAccessedSourceElement with the org.w3c.dom.Element object accessed in the assertion
						String xpath = getXPath(webElement);
						//System.out.println("webElement:" + webElement);


						try {
							assertedSourceElement = getElementFromXpath(xpath, browser);
							System.out.println("The assertedSourceElement is: " + assertedSourceElement);
						} catch (XPathExpressionException e) {
							System.out.println("XPathExpressionException!");
							e.printStackTrace();
						}
					}

					// to distinguish original assertions from reused/generated ones
					AssertedElementPattern aep = new AssertedElementPattern(assertedSourceElement, assertion, assertedElementLocator);
					aep.setAssertionOrigin("original assertion");
					originalAssertedElementPatterns.add(aep);
					// adding assertion to the current DOM state in the SFG
					firstConsumer.getContext().getCurrentState().addAssertedElementPattern(aep);


					break;
				default:
				}
			}			
		}

		finalReport += "#Original test cases: " + Integer.toString(numOfTestCases) + "\n";

		LOG.info("Initial paths on the SFG was created based on executed instrumented code...");
		LOG.info("#states in the SFG after generating happy paths is " + firstConsumer.getContext().getSession().getStateFlowGraph().getNumberOfStates());	
		LOG.info("#transitions in the SFG after generating happy paths is " + firstConsumer.getContext().getSession().getStateFlowGraph().getAllEdges().size());	

		finalReport += "#states in the SFG after generating happy paths: " + Integer.toString(firstConsumer.getContext().getSession().getStateFlowGraph().getNumberOfStates()) + "\n";
		finalReport += "#transitions in the SFG after generating happy paths: " + Integer.toString(firstConsumer.getContext().getSession().getStateFlowGraph().getAllEdges().size()) + "\n";

		CrawlSession session = firstConsumer.getContext().getSession();

		//saveSFG(session);

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
		String[] withParameter = {" id:", " name:", " xpath:", " tag name:", " class name:", " css selector:", " partial link text:", " link text:", 
		"sendKeys"};
		String[] withParameterForAssertion = {"assertion", "By.id:", "By.linkText:", "By.partialLinkText:", "By.name:", "By.tagName:", "By.xpath:", "By.className:", "By.selector:"};
		String[] withoutParameter = {"clear", "click"}; 

		ArrayList<String> methodValue = new ArrayList<String>();
		String value = null;
		int startIndexOfValue, endIndexOfValue = 0;

		if (s.equals("Alert"))
			methodValue.add("Alert");
		else if (s.equals("assertionModeOn"))
			inAssertionMode = true;
		else if (s.equals("assertionModeOff"))
			inAssertionMode = false;
		else if (inAssertionMode == true){
			for (int i=0; i<withParameterForAssertion.length; i++){
				if (s.contains(withParameterForAssertion[i])){
					startIndexOfValue = s.indexOf(withParameterForAssertion[i]) + withParameterForAssertion[i].length() + 1;
					endIndexOfValue = s.length();
					value = s.substring(startIndexOfValue, endIndexOfValue);
					methodValue.add(withParameterForAssertion[i]);
					break;
				}
			}
		}else{ // inAssertionMode == false
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
		}
		methodValue.add(value);

		System.out.println("methodValue is " + methodValue);

		return methodValue;
	}


	private Eventable getCorrespondingEventable(WebElement webElement, Identification identification, EventType eventType, EmbeddedBrowser browser) {
		CandidateElement candidateElement = getCorrespondingCandidateElement(webElement, identification, browser);
		Eventable event = new Eventable(candidateElement, eventType);
		System.out.println(event);
		return event;
	}

	private CandidateElement getCorrespondingCandidateElement(WebElement webElement, Identification identification, EmbeddedBrowser browser) {
		Document dom;
		try {
			dom = DomUtils.asDocument(browser.getStrippedDomWithoutIframeContent());

			// Get the corresponding org.w3c.dom.Element of a WebElement
			String xpath = getXPath(webElement);
			org.w3c.dom.Element sourceElement = getElementFromXpath(xpath, browser);
			//CandidateElement candidateElement = new CandidateElement(sourceElement, new Identification(Identification.How.xpath, xpath), "");
			CandidateElement candidateElement = new CandidateElement(sourceElement, identification, "");
			LOG.debug("Found new candidate element: {} with eventableCondition {}",	candidateElement.getUniqueString(), null);
			candidateElement.setEventableCondition(null);
			return candidateElement;

			/* Previous inefficient way
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
						candidateElement.setEventableCondition(null);
						return candidateElement;
					}
				}
			}*/
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
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

	public org.w3c.dom.Element getElementFromXpath(String xpathToRetrieve, EmbeddedBrowser browser) throws XPathExpressionException {
		Document dom;
		org.w3c.dom.Element element = null;
		try {
			dom = DomUtils.asDocument(browser.getStrippedDomWithoutIframeContent());
			XPath xPath = XPathFactory.newInstance().newXPath();
			//System.out.println("value is " + xPath.evaluate(xpathToRetrieve, dom));
			element = (org.w3c.dom.Element) xPath.evaluate(xpathToRetrieve, dom, XPathConstants.NODE);
			//System.out.println("element.getNodeName(): " + element.getNodeName());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return element;
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
		System.out.println("List of asserted element paterns in assertedElementPatterns:");
		for (AssertedElementPattern	aep: originalAssertedElementPatterns)
			System.out.println(aep.getAssertion());

		System.out.println("***************");

		StateFlowGraph sfg = session.getStateFlowGraph();


		// DOM-based assertion generation part
		for (StateVertex s: sfg.getAllStates()){
			//System.out.println("DOM on state " + s.getName() + " is: " + s.getDom().replace("\n", "").replace("\r", "").replace(" ", ""));
			//System.out.println("There are " + s.getAssertions().size() + " asserted element patterns in state " + s.getName() + " before generation.");
			//if (s.getAssertions().size()>0){
			//	for (int i=0;i<s.getAssertions().size();i++)
			//		System.out.println(s.getAssertions().get(i));
			//}

			for (AssertedElementPattern	aep: originalAssertedElementPatterns){
				if (!s.getAssertions().contains(aep.getAssertion())){
					try {
						Document dom = DomUtils.asDocument(s.getDom());
						NodeList nodeList = dom.getElementsByTagName(aep.getTagName());

						org.w3c.dom.Element element = null;
						for (int i = 0; i < nodeList.getLength(); i++){
							element = (org.w3c.dom.Element) nodeList.item(i);

							AssertedElementPattern aepTemp = new AssertedElementPattern(element, "", aep.getAssertedElementLocator()); // creating an AssertedElementPattern without any assertion text
							String howElementMatched = aep.getHowElementMatch(aepTemp);
							String howPatternMatched = aep.getHowPatternMatch(aepTemp);

							//aep.getAssertionType();

							// AssertedElement-Level Assertion
							switch (howElementMatched){
							case "ElementFullMatch":
								outForLogging.write("\n"+aep);
								outForLogging.write("\n"+"ElementFullMatch");
								outForLogging.write("\n"+aepTemp);
								aepTemp.setAssertion(aep.getAssertion());
								aepTemp.setAssertionOrigin("reused assertion in case of ElementFullMatch");
								s.addAssertedElementPattern(aepTemp); // reuse the same AssertedElementPattern
								break;
							case "ElementTagAttMatch":
								outForLogging.write("\n"+aep);
								outForLogging.write("\n"+"ElementTagAttMatch");
								s.addAssertedElementPattern(generateElementAssertion(aep, howElementMatched));
								break;
							case "ElementTagMatch":
								outForLogging.write("\n"+aep);
								outForLogging.write("\n"+"ElementTagMatch");
								s.addAssertedElementPattern(generateElementAssertion(aep, howElementMatched));
								break;
							}


							// AssertedElementPattern-Level Assertion
							switch (howPatternMatched){
							case "PatternFullMatch":
								outForLogging.write("\n"+aep);
								outForLogging.write("\n"+"PatternFullMatch");
								s.addAssertedElementPattern(generatePatternAssertion(aep, howPatternMatched));
								break;
							case "PatternTagAttMatch":
								outForLogging.write("\n"+aep);
								outForLogging.write("\n"+"PatternTagAttMatch");
								s.addAssertedElementPattern(generatePatternAssertion(aep, howPatternMatched)); 
								break;
							case "PatternTagMatch":
								outForLogging.write("\n"+aep);
								outForLogging.write("\n"+"PatternTagMatch");
								s.addAssertedElementPattern(generatePatternAssertion(aep, howPatternMatched)); 
								break;
							}

						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				}

			}

			//System.out.println("There are " + s.getAssertions().size() + " asserted element patterns in state " + s.getName() + " after generation.");
			//if (s.getAssertions().size()>0){
			//	for (int i=0;i<s.getAssertions().size();i++)
			//		System.out.println(s.getAssertions().get(i));
			//}
		}

		generateTestSuite(session);

		LOG.info("TestSuiteExtension plugin has finished");
	}

	private AssertedElementPattern generatePatternAssertion(AssertedElementPattern aep, String howMatched) {
		// pattern assertion on BODY is useless
		if (aep.getTagName().toUpperCase().equals("BODY"))
			return null;

		String elementTag = aep.getTagName();
		String elementText = "";//aep.getTextContent();
		ArrayList<String> elementAttributes = new ArrayList<String>(aep.getAttributes());

		String parentTag =  aep.getParentTagName();
		String parentText =  "";//aep.getParentTextContent();
		ArrayList<String> parentAttributes = new ArrayList<String>(aep.getParentAttributes());

		ArrayList<String> childrenTags = new ArrayList<String>(aep.getChildrenTagName());
		//ArrayList<String> childrenTexts = new ArrayList<String>(aep.getChildrenTextContent());
		ArrayList<String> childrenTexts = new ArrayList<String>();
		ArrayList<ArrayList<String>> childrenAttributes = new ArrayList<ArrayList<String>>();
		for (int i=0; i < aep.getChildrenAttributes().size(); i++)
			childrenAttributes.add(aep.getChildrenAttributes().get(i));


		// DOMElement element = new DOMElement(String tagName, String textContent, ArrayList<String> attributes);
		String patternCheckAssertion = "element = new DOMElement(\"" + elementTag + "\", \"" + elementText.replace("\"", "\\\"") + "\", new ArrayList<String>(Arrays.asList(\"";
		for (int j=0; j < elementAttributes.size()-1; j++)
			patternCheckAssertion += elementAttributes.get(j).replace("\"", "\\\"") + "\",\"";
		patternCheckAssertion += elementAttributes.get(elementAttributes.size()-1).replace("\"", "\\\"") + "\")));\n";

		patternCheckAssertion += "\t\t\tparentElement = new DOMElement(\"" + parentTag + "\", \"" + parentText.replace("\"", "\\\"") + "\", new ArrayList<String>(Arrays.asList(\"";
		if (parentAttributes.size()>0){ // avoiding null
			for (int j=0; j < parentAttributes.size()-1; j++)
				patternCheckAssertion += parentAttributes.get(j).replace("\"", "\\\"") + "\",\"";
			patternCheckAssertion += parentAttributes.get(parentAttributes.size()-1).replace("\"", "\\\"") + "\")));\n";
		}else
			patternCheckAssertion += "\")));\n";

		patternCheckAssertion += "\t\t\tchildrenElements.clear();\n";
		for (int k=0; k<childrenTags.size(); k++){
			//patternCheckAssertion += "\t\t\tchildrenElements.add(new DOMElement(\"" + childrenTags.get(k) + "\", \"" + childrenTexts.get(k).replace("\"", "\\\"") + "\", new ArrayList<String>(Arrays.asList(\"";
			patternCheckAssertion += "\t\t\tchildrenElements.add(new DOMElement(\"" + childrenTags.get(k) + "\", \"\", new ArrayList<String>(Arrays.asList(\"";
			if (k < childrenAttributes.size() && childrenAttributes.get(k).size()>0){ // check if attributes are less than tags due to being null
				for (int j=0; j < childrenAttributes.get(k).size()-1; j++)
					patternCheckAssertion += childrenAttributes.get(k).get(j).replace("\"", "\\\"") + "\",\"";
				patternCheckAssertion += childrenAttributes.get(k).get(childrenAttributes.get(k).size()-1).replace("\"", "\\\"") + "\"))));\n";
			}else
				patternCheckAssertion += "\"))));\n";
		}

		if (howMatched.equals("PatternTagMatch"))
			patternCheckAssertion += "\t\t\tassertTrue(isElementPatternTagPresent(parentElement , element, childrenElements))";
		else
			patternCheckAssertion += "\t\t\tassertTrue(isElementPatternFullPresent(parentElement , element, childrenElements))";


		AssertedElementPattern aepMatch = new AssertedElementPattern(aep.getSourceElement(), patternCheckAssertion, aep.getAssertedElementLocator());
		aepMatch.setAssertionOrigin("generated assertion in case of " + howMatched);

		try {
			outForLogging.write("\n"+aepMatch);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return aepMatch;
	}


	private AssertedElementPattern generateElementAssertion(AssertedElementPattern aep, String howMatched) {
		// generating an AssertedElementPattern based on the matching result
		AssertedElementPattern newaep = new AssertedElementPattern(aep.getSourceElement(), "", aep.getAssertedElementLocator()); // creating an AssertedElementPattern without any assertion text

		switch (howMatched){
		case "ElementTagAttMatch":
			if (newaep.getAssertedElementLocator().toUpperCase().contains("BODY"))
				return null;
			newaep.setAssertion("assertTrue(isElementPresent("+ newaep.getAssertedElementLocator() +"))");
			try {
				outForLogging.write("\n"+newaep);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "ElementTagMatch":
			if (newaep.getTagName().toUpperCase().equals("BODY"))
				return null;			
			newaep.setAssertion("assertTrue(isElementPresent(By.tagName(\"" + newaep.getTagName() +"\")))");
			try {
				outForLogging.write("\n"+newaep);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
		newaep.setAssertionOrigin("generated assertion in case of " + howMatched);
		return newaep;
	}



	/**
	 * Generating the extended test suite in multiple files
	 * @param session
	 */
	private void generateTestSuite(CrawlSession session) {
		StateFlowGraph sfg = session.getStateFlowGraph();

		List<List<GraphPath<StateVertex, Eventable>>> results = sfg.getAllPossiblePaths(sfg.getInitialState());
		ArrayList<TestMethod> testMethods = new ArrayList<TestMethod>();
		String how, howValue, sendValue, seleniumAction1;

		int counter = 0, totalAssertions = 0, origAndReusedAssertions = 0, reusedAssertions = 0, generatedAssertions = 0, 
				ElementFullMatch = 0, ElementTagAttMatch = 0, ElementTagMatch = 0, PatternFullMatch = 0, PatternTagAttMatch = 0, PatternTagMatch = 0;

		for (List<GraphPath<StateVertex, Eventable>> paths : results) {
			//For each new sink node

			for (GraphPath<StateVertex, Eventable> p : paths) {
				//For each path to the sink node
				TestMethod testMethod = new TestMethod("method" + Integer.toString(counter));

				for (Eventable edge : p.getEdgeList()) {
					//For each eventable in the path

					testMethod.addStatement("//From state " + Integer.toString(edge.getSourceStateVertex().getId()) 
							+ " to state " + Integer.toString(edge.getTargetStateVertex().getId()));
					testMethod.addStatement("//" + edge.toString());

					//System.out.println("//From state " + edge.getSourceStateVertex().getId() + " to state " + edge.getTargetStateVertex().getId());
					//System.out.println("//" + edge.toString());


					if (edge.getRelatedFormInputs().size() > 0){
						// First fill the inputs 
						for (FormInput formInput : edge.getRelatedFormInputs()) {
							if (formInput.getInputValues().iterator().hasNext()) {

								if (formInput.getType().toLowerCase().startsWith("text")
										|| formInput.getType().equalsIgnoreCase("password")
										|| formInput.getType().equalsIgnoreCase("hidden")) {

									//System.out.println("how: " + formInput.getIdentification().getHow().toString());
									//System.out.println("name: " + formInput.getIdentification().getValue());
									//System.out.println("type: " + formInput.getType());
									//System.out.println("value: " + formInput.getInputValues().iterator().next().getValue());

									sendValue = formInput.getInputValues().iterator().next().getValue().replace("\"", "\\\"");

									how = formInput.getIdentification().getHow().toString();
									howValue = formInput.getIdentification().getValue().replace("\"", "\\\"");
									if (how.equals("text"))	
										how = "linkText";
									if (how.equals("partialText"))
										how = "partialLinkText";

									testMethod.addStatement("driver.findElement(By." + how + "(\"" + howValue + "\")).clear();");

									if (sendValue.startsWith("RND-")){
										testMethod.addStatement("String RandValue = \"RND-\" + new RandomInputValueGenerator().getRandomString(4);");
										testMethod.addStatement("driver.findElement(By." + how + "(\"" + howValue + "\")).sendKeys(RandValue);");
									}else
										testMethod.addStatement("driver.findElement(By." + how + "(\"" + howValue + "\")).sendKeys(\"" + sendValue + "\");");


								} else if (formInput.getType().equalsIgnoreCase("checkbox")) {
								} else if (formInput.getType().equalsIgnoreCase("radio")) {
								} else if (formInput.getType().startsWith("select")) {
								}

							}
						}
					}

					//System.out.println("how: " + edge.getIdentification().getHow().toString());
					//System.out.println("value: " + edge.getIdentification().getValue());
					//System.out.println("text: " + edge.getElement().getText().replaceAll("\"", "\\\\\"").trim());

					how = edge.getIdentification().getHow().toString();
					howValue = edge.getIdentification().getValue().replace("\"", "\\\"");
					if (how.equals("text"))	
						how = "linkText";
					if (how.equals("partialText"))
						how = "partialLinkText";


					testMethod.addStatement("driver.findElement(By." + how + "(\"" + howValue + "\")).click();");

					// adding assertions
					if (edge.getTargetStateVertex().getAssertions().size()>0){
						//adding DOM-mutator to be used for mutation testing of generated assertions
						testMethod.addStatement("mutateDOMTree();");

						for (int i=0;i<edge.getTargetStateVertex().getAssertedElementPatters().size();i++){
							totalAssertions++;
							String assertion = edge.getTargetStateVertex().getAssertedElementPatters().get(i).getAssertion();
							String assertionOringin = edge.getTargetStateVertex().getAssertedElementPatters().get(i).getAssertionOrigin(); 

							if (assertionOringin.contains("in case of"))
								testMethod.addStatement("if(shouldConsiderAddedAssertion()){");


							if (assertionOringin.contains("original assertion"))
								origAndReusedAssertions++;
							else if (assertionOringin.contains("reused assertion")){
								reusedAssertions++;
								if (assertionOringin.contains("ElementFullMatch"))
									ElementFullMatch++;	
							}
							else{
								generatedAssertions++;

								if (assertionOringin.contains("ElementTagAttMatch"))
									ElementTagAttMatch++;
								if (assertionOringin.contains("ElementTagMatch"))
									ElementTagMatch++;
								if (assertionOringin.contains("PatternFullMatch"))
									PatternFullMatch++;
								if (assertionOringin.contains("PatternTagAttMatch"))
									PatternTagAttMatch++;
								if (assertionOringin.contains("PatternTagMatch"))
									PatternTagMatch++;

							}

							//System.out.println(edge.getTargetStateVertex().getAssertions().get(i) + ";");
							//testMethod.addStatement(edge.getTargetStateVertex().getAssertions().get(i) + "; // " + edge.getTargetStateVertex().getAssertions().get(i));
							testMethod.addStatement(assertion + "; // " + assertionOringin);
							if (assertionOringin.contains("in case of"))
								testMethod.addStatement("}");
						}
					}

				}

				testMethods.add(testMethod);

				String TEST_SUITE_PATH = "src/test/java/generated";
				String CLASS_NAME = "GeneratedTestCase"+ Integer.toString(counter);
				String FILE_NAME_TEMPLATE = "TestCase.vm";

				try {
					DomUtils.directoryCheck(TEST_SUITE_PATH);
					String fileName = null;

					JavaTestGenerator generator =
							new JavaTestGenerator(CLASS_NAME, session.getInitialState().getUrl(), testMethods);

					fileName = generator.generate(DomUtils.addFolderSlashIfNeeded(TEST_SUITE_PATH), FILE_NAME_TEMPLATE);

					System.out.println("Tests succesfully generated in " + fileName);		

				} catch (IOException e) {
					System.out.println("Error in checking " + TEST_SUITE_PATH);
					e.printStackTrace();
				} catch (Exception e) {
					System.out.println("Error generating testsuite: " + e.getMessage());
					e.printStackTrace();
				}			

				counter++;
				testMethods.clear(); // clearing testMethods for the next JUnit file
			}
		}

		System.out.print(finalReport);

		
		System.out.println("Total #assertions in the original test suite:" + originalAssertedElementPatterns.size());

		System.out.println("Total #assertions in the test suite from happy paths: " + origAndReusedAssertions);

		System.out.println("Total #assertions in the extended test suite:" + totalAssertions);
		
		System.out.println("Total #cloned (reused) assertions in the extended test suite: " + reusedAssertions);

		int reusedOrigAssertions = origAndReusedAssertions-originalAssertedElementPatterns.size(); // original assertions that are reused in extended paths		
		reusedAssertions += reusedOrigAssertions;

		System.out.println("Total #reused assertions (cloned + added) in the extended test suite: " + reusedAssertions);
		System.out.println("Total #generated assertions in the extended test suite: " + generatedAssertions);


		System.out.println("Total #ElementFullMatch: " + ElementFullMatch);
		System.out.println("Total #ElementTagAttMatch: " + ElementTagAttMatch);
		System.out.println("Total #ElementTagMatch: " + ElementTagMatch);
		System.out.println("Total #PatternFullMatch: " + PatternFullMatch);
		System.out.println("Total #PatternTagAttMatch: " + PatternTagAttMatch);
		System.out.println("Total #PatternTagMatch: " + PatternTagMatch);

		
		try {
			outForLogging.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	public void onFireEvent(CrawlerContext context, StateVertex stateBefore, Eventable eventable, StateVertex stateAfter) {

		if (true)
			return;

		// Calculating JS statement code coverage for feedback-directed exploration
		for (String modifiedJS : JSModifyProxyPlugin.getModifiedJSList()){
			//System.out.println("MODIFIED CODES ARE: " + modifiedJS);
			try{
				Object counter =  this.browser.executeJavaScript("return " + modifiedJS + "_exec_counter;");
				setCountList(modifiedJS, counter);
			}catch (Exception e) {
				LOG.info("Could not execute script");
			}
		}
		double coverage = getCoverage();;
		context.getSession().getStateFlowGraph().setLatestCoverage(coverage);

		//LOG.info(Serializer.toPrettyJson(AstInstrumenter.jsFunctions));
	}


	/**
	 * Checking assertions on happy paths from existing test suite on new paths
	 * Currently dealing with assertions that are DOM related. We do not consider 
	 * those using variables in test cases for simplicity at this step.
	 */
	@Override
	public void onNewState(CrawlerContext context, StateVertex vertex) {

		if (true)
			return;

		// Calculate initial code coverage for the index page to be used by Feedex
		if (vertex.getId() == vertex.INDEX_ID){
			for (String modifiedJS : JSModifyProxyPlugin.getModifiedJSList()){
				// LOGGER.info("** MODIFIEDS ARE: " + modifiedJS);
				try{
					Object counter =  this.browser.executeJavaScript("return " + modifiedJS + "_exec_counter;");
					ArrayList countList = (ArrayList) counter;

					setCountList(modifiedJS, counter);
				}catch (Exception e) {
					LOG.info("Could not execute script: return " + modifiedJS + "_exec_counter;");
				}
			}
			double coverage = getCoverage();
			context.getSession().getStateFlowGraph().setInitialCoverage(vertex, coverage);
		}

	}


	// Keeping track of executed lines of a js which will be used to calcualte coverage	
	public void setCountList(String modifiedJS, Object counter){
		ArrayList<Integer> countList = new ArrayList<Integer>();
		ArrayList c = (ArrayList) counter;
		countList.clear(); // used as a temp list to be added to JSCountList

		if (!JSCountList.containsKey(modifiedJS)){ // if not exist add new js to the JSCountList	
			for (int i=0;i<c.size();i++)
				countList.add(((Long)c.get(i)).intValue());
			JSCountList.put(modifiedJS, countList);
		}else{ // update JSCountList
			for (int i: JSCountList.get(modifiedJS))
				countList.add(i);
			for (int i=0;i<c.size();i++)
				countList.set(i,countList.get(i)+((Long)c.get(i)).intValue());
			JSCountList.put(modifiedJS, countList);
		}
	}


	// Compute code coverage
	public double getCoverage(){

		double coverage = 0.0;
		int totalExecutedLines = 0, totalLines = 0;

		for (String modifiedJS : JSModifyProxyPlugin.getModifiedJSList()){
			if (JSCountList.containsKey(modifiedJS)){
				totalLines += JSCountList.get(modifiedJS).size();
				int executedLines = 0;

				LOG.info(" List of " + modifiedJS + " is: " + JSCountList.get(modifiedJS));

				for (int i: JSCountList.get(modifiedJS))
					if (i>0){
						totalExecutedLines++;
						executedLines++;
					}

				LOG.info("List of " + modifiedJS + " # lines ececuted: " + executedLines + " # tolal lines: " + JSCountList.get(modifiedJS).size() + " - code coverage: " + (double)executedLines/(double)JSCountList.get(modifiedJS).size()*100+"%\n");
			}
		}

		coverage = (double)totalExecutedLines/(double)totalLines;

		LOG.info("Code coverage: " + coverage*100+"%");
		return coverage;
	}


	@Override
	public void onRevisitState(CrawlerContext context, StateVertex currentState) {
		// TODO: check the assertions from test suite
	}



	// The following methods are helpers for the generated JUnit files

	public static boolean shouldConsiderAddedAssertion() {
		// To be used for executing added assertions for the experiments in the paper
		return addNewAssertion;
	}

	public static boolean getCoverageReport() {
		// To be used for calculating JS code coverage for the experiments in the paper
		return false;
	}

	public static String mutateDOMTreeCode() {
		// To be used for DOM mutation testing for the experiments in the paper
		return null;
	}

}
