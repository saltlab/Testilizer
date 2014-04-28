package com.crawljax.plugins.testilizer;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;


import org.jgrapht.GraphPath;
import org.junit.runner.JUnitCore;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlTaskConsumer;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.DomChangeNotifierPlugin;
import com.crawljax.core.plugin.ExecuteInitialPathsPlugin;
import com.crawljax.core.plugin.OnCloneStateDetectedPlugin;
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
import com.crawljax.plugins.testilizer.seleniuminstrumentor.SeleniumInstrumentor;
import com.crawljax.plugins.testilizer.svm.svm_predict;
import com.crawljax.plugins.testilizer.svm.svm_train;
import com.crawljax.plugins.testilizer.testcasegenerator.JavaTestGenerator;
import com.crawljax.plugins.testilizer.testcasegenerator.TestMethod;
import com.crawljax.util.AssertedElementRegion;
//import com.crawljax.plugins.jsmodify.AstInstrumenter;
//import com.crawljax.plugins.jsmodify.JSModifyProxyPlugin;
import com.crawljax.util.DomUtils;
import com.crawljax.util.ElementFeatures;
import com.crawljax.util.XPathHelper;
import com.google.common.collect.ImmutableList;

/**
 * TestSuiteExtension is Crawljax plugin tool which extends a current Selenium test suite of an Ajax application. 
 * It initiates the state-flow graph with Selenium test cases (happy paths) and crawl other paths around those happy paths.
 **/
public class TestSuiteExtension implements PreCrawlingPlugin, OnNewStatePlugin, PreStateCrawlingPlugin,
PostCrawlingPlugin, OnUrlLoadPlugin, OnFireEventSucceededPlugin, ExecuteInitialPathsPlugin, DomChangeNotifierPlugin, OnCloneStateDetectedPlugin{

	/**
	 * Settings for my experiments
	 */
	//static String appName = "claroline";
	static String appName = "photogallery";
	//static String appName = "wolfcms";
	//static String appName = "eshop1";
	//static String appName = "eshop2";

	//private String testSuiteNameToGenerate = appName + "_EP_new";
	private String testSuiteNameToGenerate = appName + "_IP";
	//private String testSuiteNameToGenerate = appName + "_EP_Learned1";

	// one should only be true! if two are false then creates sfg files
	static boolean loadInitialSFGFromFile = true;
	static boolean loadExtendedSFGFromFile = false;

	static boolean saveNewTrainingDatasetToFile = false;

	static boolean addAllAssertions = false; // setting for experiment on DOM-based assertion generation part (default should be true)
	static boolean addOriginalAssertions = false; // setting for experiment on DOM-based assertion generation part (default should be true)
	static boolean addReusedAssertions = false; // setting for experiment on DOM-based assertion generation part (default should be true)
	static boolean addGeneratedAssertions = false; // setting for experiment on DOM-based assertion generation part (default should be true)
	static boolean addLearnedAssertions = false; // setting for experiment on DOM-based assertion generation part (default should be true)

	// this is to create a baseline for comparison reasons -> generating random assertions on the page.	static boolean randomAssertionGeneration = false;  
	static boolean addRandomAssertions = true; // getting code coverage by JSCover tool proxy (default should be false)
	// These are to store assertions for all DOM elements in browser state to be added to states in the SFG
	ArrayList<String> tempListOfelementTagAttAssertions  = new ArrayList<String>(); 
	ArrayList<String> tempListOfelementFullAssertions  = new ArrayList<String>(); 
	ArrayList<String> tempListOfregionTagAttAssertions  = new ArrayList<String>(); 
	ArrayList<String> tempListOfregionTagAssertions  = new ArrayList<String>(); 
	ArrayList<String> tempListOfregionFullAssertions  = new ArrayList<String>(); 
	int randElementTagAttAssertions = 0;
	int randRegionTagAssertions = 0;
	int randRegionFullAssertions = 0;

	//TODO: Attention! I am now using the same port number for mutations as for the JSCover at this point! Should be changed for the next test suites...
	static boolean getCoverageReport = false; // getting code coverage by JSCover tool proxy (default should be false)

	// DOM-mutation testing settings
	static boolean mutateDOM = true;  // on DOM-based mutation testing to randomly mutate current DOM state (default should be false)
	public static int MutationOperatorCode = 0; // DOM mutation operator is set via test suite runner
	public static int StateToBeMutated = -1; // This is also set via test suite runner
	public static int [][] SelectedRandomElementInDOM = new int[4][500];  // SelectedRandomElementInDOM[i][j]: the selected random DOM element id using operator i in state j (max=1000 for now)

	public static void initializeSelectedRandomElements(){
		for (int i=0; i<4; i++)
			for (int j=0;j<500;j++)
				SelectedRandomElementInDOM[i][j]=-1;
	}

	public static int [][] getSelectedRandomElements(){
		return SelectedRandomElementInDOM;
	}


	// SVM training set
	ArrayList<ElementFeatures> trainingPositiveSetElementFeatures = new ArrayList<ElementFeatures>();
	ArrayList<ElementFeatures> trainingNegetiveSetElementFeatures = new ArrayList<ElementFeatures>();
	boolean manualTestPathsCreated = false;




	private static final Logger LOG = LoggerFactory.getLogger(TestSuiteExtension.class);

	CrawljaxConfiguration config = null;
	private EmbeddedBrowser browser = null;

	private ArrayList<AssertedElementRegion> originalAssertedElementRegions = new ArrayList<AssertedElementRegion>();

	private boolean inAssertionMode = false;

	// Keeping track of executed lines of a JavaScript code for Feedex	
	private Map<String,ArrayList<Integer>> JSCountList = new Hashtable<String,ArrayList<Integer>>(); 

	private String finalReport ="";


	private ArrayList<ElementFeatures> indexPageElementsFeatures;



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

		/**
		 * (1) Instrumenting original Selenium unit test files
		 */

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


		/*
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
		 */

		// Not set on my Mac
		boolean success = true;
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
		if (loadInitialSFGFromFile)
			return;
		if (loadExtendedSFGFromFile)
			return;


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
		String elementLocator = null;

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

			if (st.equals("sleep")){
				System.out.println("wating 500 miliseconds...");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
					elementLocator = "By.id(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.id(howValue));
					break;
				case " name:":
					how = Identification.How.name;
					howValue = methodValue.get(1);
					elementLocator = "By.name(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.name(howValue));
					break;
				case " xpath:":
					how = Identification.How.xpath;
					howValue = methodValue.get(1);
					elementLocator = "By.xpath(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.xpath(howValue));
					break;
				case " tag name:":
					how = Identification.How.tag;
					howValue = methodValue.get(1);
					elementLocator = "By.tagName(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.tagName(howValue));
					break;
				case " class name:":
					how = Identification.How.name;
					howValue = methodValue.get(1);
					elementLocator = "By.className(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.className(howValue));
					break;
				case " css selector:":
					how = Identification.How.cssSelector;
					howValue = methodValue.get(1);
					webElement = browser.getBrowser().findElement(By.cssSelector(howValue));
					elementLocator = "By.cssSelector(\"" + howValue.replace("\"", "\\\"") +"\")";
					break;
				case " link text:":
					how = Identification.How.text;
					howValue = methodValue.get(1);
					elementLocator = "By.linkText(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.linkText(howValue));
					break;
				case " partial link text:":
					how = Identification.How.partialText;
					howValue = methodValue.get(1);
					elementLocator = "By.partialLinkText(\"" + howValue +"\")";
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
						inputValue = "RND" + new RandomInputValueGenerator().getRandomString(4);
						System.out.println("Random string " + inputValue + " generated for inputValue");
					}
					if (inputValue.equals("$RandValue@example.com")){  // Only the case for wolfcms app for unique email address
						inputValue = "RND" + new RandomInputValueGenerator().getRandomString(4) + "@example.com";
						System.out.println("Random string " + inputValue + " generated for inputValue");
					}
					if (inputValue.equals("$RandValue.com")){  // Only the case for eshop app for unique domain address
						inputValue = "RND" + new RandomInputValueGenerator().getRandomString(4) + ".com";
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

						if (fired){
							// inspecting DOM changes and adding to SFG
							firstConsumer.getCrawler().inspectNewStateForInitailPaths(event);

						}

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
					elementLocator = "By.id(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.id(howValue));
					break;
				case "By.linkText:":
					howValue = methodValue.get(1);
					elementLocator = "By.linkText(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.linkText(howValue));
					break;
				case "By.partialLinkText:":
					howValue = methodValue.get(1);
					elementLocator = "By.partialLinkText(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.partialLinkText(howValue));
					break;
				case "By.name:":
					howValue = methodValue.get(1);
					elementLocator = "By.name(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.name(howValue));
					break;
				case "By.tagName:":
					howValue = methodValue.get(1);
					elementLocator = "By.tagName(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.tagName(howValue));
					break;
				case "By.xpath:":
					howValue = methodValue.get(1);
					elementLocator = "By.xpath(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.xpath(howValue));
					break;
				case "By.className:":
					howValue = methodValue.get(1);
					elementLocator = "By.className(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.className(howValue));
					break;
				case "By.selector:":
					howValue = methodValue.get(1);
					elementLocator = "By.cssSelector(\"" + howValue +"\")";
					webElement = browser.getBrowser().findElement(By.cssSelector(howValue));
					System.out.println("Found webElement: " + webElement);
					break;
				case "assertion":

					org.w3c.dom.Element assertedSourceElement = null;
					String assertion = methodValue.get(1);

					if (assertion.contains(".findElement") || assertion.contains("By.")){	// only for assertions that access a DOM element

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


						// Generate feature vector of the asserted element with label +1 and add to dataset file to be used for training the SVM
						//addToTrainingSet(getAssertedElemFeatureVector(webElement));


						ElementFeatures ef = getAssertedElemFeatureVector(webElement);

						boolean elementExist = false;
						for (ElementFeatures currentEF : trainingPositiveSetElementFeatures){
							if (currentEF.equals(ef)){
								currentEF.increaseCount();
								elementExist = true;
								break;
							}
							//if (currentEF.cosineSimilarity(ef) > 0.5){
							//	elementExist = true;
							//	break;
							//}					
						}
						if (elementExist == false)
							trainingPositiveSetElementFeatures.add(ef);



					}


					// to distinguish original assertions from reused/generated ones
					AssertedElementRegion aep = new AssertedElementRegion(assertedSourceElement, assertion, elementLocator);
					aep.setAssertionOrigin("original assertion");
					// adding assertion to the current DOM state in the SFG
					StateVertex currentState = firstConsumer.getContext().getCurrentState();
					boolean assertionAdded = currentState.addAssertedElementRegion(aep);
					// adding an AssertedElemetRegion-level assertion for the original assertion
					if (assertionAdded)
						currentState.addAssertedElementRegion(generateRegionAssertion(aep, "AEP for Original")); 


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

		manualTestPathsCreated = true;

		CrawlSession session = firstConsumer.getContext().getSession();

		if (!appName.equals("claroline"))
			saveInitialSFG(session);

	}

	private void addToTrainingSet(ElementFeatures elemFeatureVector) {
		if (saveNewTrainingDatasetToFile==false)
			return;

		// Dumping the feature vectore of asserted element into training dataset
		// sample data format: <label> <featureIndex>:<featureValue>. E.g: +1 1:0.708333 2:1 3:1 4:-0.320755 5:-0.105023 ...
		String label = elemFeatureVector.getClassLabel()>0 ? "+1" : "-1";
		/*String sample = label + 
				" 1:" + elemFeatureVector.getFreshness() +
				" 2:" + elemFeatureVector.getTextImportance() +
				" 3:" + String.format("%.3f", elemFeatureVector.getNormalBlockWidth()) +
				" 4:" + String.format("%.3f", elemFeatureVector.getNormalBlockHeight()) +
				" 5:" + String.format("%.3f", elemFeatureVector.getNormalBlockCenterX()) +
				" 6:" + String.format("%.3f", elemFeatureVector.getNormalBlockCenterY()) +
				" 7:" + String.format("%.3f", elemFeatureVector.getInnerHtmlDensity()) +
				" 8:" + String.format("%.3f", elemFeatureVector.getLinkDensity()) +
				" 9:" + String.format("%.3f", elemFeatureVector.getBlockDensity());
		 */
		String sample = label + 
				" 1:" + elemFeatureVector.getTextImportance() +
				" 2:" + String.format("%.3f", elemFeatureVector.getNormalBlockWidth()) +
				" 3:" + String.format("%.3f", elemFeatureVector.getNormalBlockHeight()) +
				" 4:" + String.format("%.3f", elemFeatureVector.getNormalBlockCenterX()) +
				" 5:" + String.format("%.3f", elemFeatureVector.getNormalBlockCenterY()) +
				" 6:" + String.format("%.3f", elemFeatureVector.getInnerHtmlDensity()) +
				" 7:" + String.format("%.3f", elemFeatureVector.getLinkDensity()) +
				" 8:" + String.format("%.3f", elemFeatureVector.getNormalNumOfChildren());


		try {
			FileWriter fw = new FileWriter("trainingSet", true); //appending new data
			fw.write(sample + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException: " + e.getMessage());
		}


	}


	private ElementFeatures getAssertedElemFeatureVector(WebElement assertedElement) {

		String xpath = getXPath(assertedElement);


		String[] blockTags = {"/div>", "/span>", "/p>", "/table>"};
		//String[] listTags = {"/ul>", "/ol>", "/li>"};
		String[] tableTags = {"/table>", "/tr>"};
		String[] linkTags = {"/a>"};
		String[] phraseTags = {"/b>", "/h1>", "/h2>", "/h3>", "/h4>", "/h5>", "/h1>", "/i>", "/em>", "/strong>", "/dfn>", "/code>", "/samp>", "/kbd>", "/var>"};
		int totalBlocksCount = 0, totalLinksCount = 0, totalTablesCount = 0;

		// extract info from the <body> part
		WebElement body = browser.getBrowser().findElements(By.tagName("body")).get(0);

		String bodyInnerHTML = body.getAttribute("innerHTML");
		double bodyWidth = (double) body.getSize().getWidth();
		double bodyHeight = (double) body.getSize().getHeight();

		for (int i=0; i<blockTags.length; i++){
			Pattern p = Pattern.compile(blockTags[i]);
			Matcher m = p.matcher(bodyInnerHTML);
			while (m.find())
				totalBlocksCount++;
		}	
		for (int i=0; i<linkTags.length; i++){
			Pattern p = Pattern.compile(linkTags[i]);
			Matcher m = p.matcher(bodyInnerHTML);
			while (m.find())
				totalLinksCount++;
		}
		for (int i=0; i<tableTags.length; i++){
			Pattern p = Pattern.compile(tableTags[i]);
			Matcher m = p.matcher(bodyInnerHTML);
			while (m.find())
				totalTablesCount++;
		}

		// extract info from the blocks
		int classLabel = 1;	// since it is an asserted element
		int freshness = 1, textImportance = 0; // 1: true, 0: false
		String blockInnerHTML = assertedElement.getAttribute("innerHTML");
		double innerHtmlDensity = (double) blockInnerHTML.length() / (double) bodyInnerHTML.length();

		double blockXPos = (double) assertedElement.getLocation().getX();
		double blockYPos = (double) assertedElement.getLocation().getY();
		double blockWidth = (double) assertedElement.getSize().getWidth();
		double blockHeight = (double) assertedElement.getSize().getHeight();

		double normalBlockWidth = blockWidth / bodyWidth;
		double normalBlockHeight = blockHeight / bodyHeight;
		double normalBlockCenterX = (blockXPos + blockWidth/2) / bodyWidth;
		double normalBlockCenterY = (blockYPos + blockHeight/2) / bodyHeight;

		int blockBlocksCount = 0, blockLinksCount = 0, blockTablesCount = 0;
		for (int i=0; i<blockTags.length; i++){
			Pattern p = Pattern.compile(blockTags[i]);
			Matcher m = p.matcher(blockInnerHTML);
			while (m.find())
				blockBlocksCount++;
			//System.out.println(blockBlocksCount);
		}	
		for (int i=0; i<linkTags.length; i++){
			Pattern p = Pattern.compile(linkTags[i]);
			Matcher m = p.matcher(blockInnerHTML);
			while (m.find())
				blockLinksCount++;
			//System.out.println(blockLinksCount);
		}
		for (int i=0; i<tableTags.length; i++){
			Pattern p = Pattern.compile(tableTags[i]);
			Matcher m = p.matcher(blockInnerHTML);
			while (m.find())
				blockTablesCount++;
			//System.out.println(blockTablesCount);
		}
		double linkDensity = 0;
		if (totalLinksCount!=0)
			linkDensity = (double)blockLinksCount / (double)totalLinksCount;
		double blockDensity = 0;
		if (totalBlocksCount!=0)
			blockDensity = (double)blockBlocksCount / (double)totalBlocksCount;


		for (int i=0; i<phraseTags.length; i++){
			if (blockInnerHTML.contains(phraseTags[i])){
				textImportance = 1;
				break;
			}
		}



		// storing parent and child info to be used in similar region assertions
		Element SourceElement;
		double normalNumOfChildren = 0;
		try {
			SourceElement = getElementFromXpath(xpath, browser);
			int numOfChildren = SourceElement.getChildNodes().getLength();
			normalNumOfChildren = (double) numOfChildren / 10.0;
			if (normalNumOfChildren>1.0)
				normalNumOfChildren = 1.0;
		} catch (XPathExpressionException e) {
			System.out.println("XPathExpressionException!");
			e.printStackTrace();
		}


		ElementFeatures elementFeatures = new ElementFeatures(xpath, freshness, textImportance, normalBlockWidth, normalBlockHeight, 
				normalBlockCenterX, normalBlockCenterY, innerHtmlDensity, linkDensity, blockDensity, normalNumOfChildren, classLabel);

		System.out.println("features for element " + assertedElement + " is: " + elementFeatures);
		return elementFeatures;
	}


	private ArrayList<ElementFeatures> getDOMElementsFeatures() {

		ArrayList<ElementFeatures> DOMElementsFeatures = new ArrayList<ElementFeatures>();

		int classLabel = -1;	// Default for block elements on manual-test states

		// No need for this. SVM does not consider label at the time of prediction
		//if (manualTestPathsCreated==true)
		//	classLabel = 0;		// These vectors are not going to be used for SVM training, they are stored to be used later for prediction step.

		String[] blockTags = {"/div>", "/span>", "/p>", "/table>"};
		//String[] listTags = {"/ul>", "/ol>", "/li>"};
		String[] tableTags = {"/table>", "/tr>"};
		String[] linkTags = {"/a>"};
		String[] phraseTags = {"/b>", "/h1>", "/h2>", "/h3>", "/h4>", "/h5>", "/h1>", "/i>", "/em>", "/strong>", "/dfn>", "/code>", "/samp>", "/kbd>", "/var>"};
		int totalBlocksCount = 0, totalLinksCount = 0, totalTablesCount = 0;

		// extract info from the <body> part
		WebElement body = browser.getBrowser().findElements(By.tagName("body")).get(0);

		String bodyInnerHTML = body.getAttribute("innerHTML");
		double bodyWidth = (double) body.getSize().getWidth();
		double bodyHeight = (double) body.getSize().getHeight();

		for (int i=0; i<blockTags.length; i++){
			Pattern p = Pattern.compile(blockTags[i]);
			Matcher m = p.matcher(bodyInnerHTML);
			while (m.find())
				totalBlocksCount++;
		}	
		for (int i=0; i<linkTags.length; i++){
			Pattern p = Pattern.compile(linkTags[i]);
			Matcher m = p.matcher(bodyInnerHTML);
			while (m.find())
				totalLinksCount++;
		}
		for (int i=0; i<tableTags.length; i++){
			Pattern p = Pattern.compile(tableTags[i]);
			Matcher m = p.matcher(bodyInnerHTML);
			while (m.find())
				totalTablesCount++;
		}



		String[] blocks = {"div", "span", "p", "table"};

		for (int k=0; k<blockTags.length; k++){

			// fresshness will be calculated based on previous state. If was not found in the previous state it is a new element
			int freshness = 0, textImportance = 0; // 1: true, 0: false

			List<WebElement> blockElements = browser.getBrowser().findElements(By.tagName(blocks[k]));
			for (WebElement block: blockElements){

				String xpath = getXPath(block);

				String blockInnerHTML = block.getAttribute("innerHTML");
				double innerHtmlDensity = (double) blockInnerHTML.length() / (double) bodyInnerHTML.length();

				double blockXPos = (double) block.getLocation().getX();
				double blockYPos = (double) block.getLocation().getY();
				double blockWidth = (double) block.getSize().getWidth();
				double blockHeight = (double) block.getSize().getHeight();

				double normalBlockWidth = blockWidth / bodyWidth;
				double normalBlockHeight = blockHeight / bodyHeight;
				double normalBlockCenterX = (blockXPos + blockWidth/2) / bodyWidth;
				double normalBlockCenterY = (blockYPos + blockHeight/2) / bodyHeight;

				int blockBlocksCount = 0, blockLinksCount = 0, blockTablesCount = 0;
				for (int i=0; i<blockTags.length; i++){
					Pattern p = Pattern.compile(blockTags[i]);
					Matcher m = p.matcher(blockInnerHTML);
					while (m.find())
						blockBlocksCount++;
				}	
				for (int i=0; i<linkTags.length; i++){
					Pattern p = Pattern.compile(linkTags[i]);
					Matcher m = p.matcher(blockInnerHTML);
					while (m.find())
						blockLinksCount++;
				}
				for (int i=0; i<tableTags.length; i++){
					Pattern p = Pattern.compile(tableTags[i]);
					Matcher m = p.matcher(blockInnerHTML);
					while (m.find())
						blockTablesCount++;
				}
				double linkDensity = 0;
				if (totalLinksCount!=0)
					linkDensity = (double)blockLinksCount / (double)totalLinksCount;
				double blockDensity = 0;
				if (totalBlocksCount!=0)
					blockDensity = (double)blockBlocksCount / (double)totalBlocksCount;

				for (int i=0; i<phraseTags.length; i++){
					if (blockInnerHTML.contains(phraseTags[i])){
						textImportance = 1;
						break;
					}
				}

				// storing parent and child info to be used in similar region assertions
				Element SourceElement;
				ElementFeatures elementFeatures = null;
				double normalNumOfChildren = 0;
				try {
					SourceElement = getElementFromXpath(xpath, browser);
					AssertedElementRegion aep = new AssertedElementRegion(SourceElement, "", xpath);
					int numOfChildren = 0;
					if (SourceElement!=null)
						if (SourceElement.getChildNodes()!=null)
							numOfChildren = SourceElement.getChildNodes().getLength();
					normalNumOfChildren = (double) numOfChildren / 10.0;
					if (normalNumOfChildren>1.0)
						normalNumOfChildren = 1.0;

					elementFeatures = new ElementFeatures(xpath, freshness, textImportance, normalBlockWidth, normalBlockHeight, 
							normalBlockCenterX, normalBlockCenterY, innerHtmlDensity, linkDensity, blockDensity, normalNumOfChildren, classLabel);

					elementFeatures.addElementRegionAssertion(generateRegionAssertion(aep, "SimilarAssertion").getAssertion());
				} catch (XPathExpressionException e) {
					System.out.println("XPathExpressionException!");
					e.printStackTrace();
				}

				//System.out.println("features for element " + block + " is: " + elementFeatures);

				DOMElementsFeatures.add(elementFeatures);

			}

		}

		return DOMElementsFeatures;
	}



	private void saveInitialSFG(CrawlSession session) {
		LOG.info("Saving the SFG...");		
		StateFlowGraph sfg = session.getStateFlowGraph();
		// saving used IDs to be used for mutation testing
		for (StateVertex s: sfg.getAllStates())
			writeMPStateIDToFile(Integer.toString(s.getId()));

		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		String sfgFileName = "sfg_init.ser";
		// Save the SFG to file
		try {
			fos = new FileOutputStream(sfgFileName);
			out = new ObjectOutputStream(fos);
			out.writeObject(sfg);
			out.close();
			LOG.info("TestSuiteExtension successfully wrote SFG to sfg_init.ser file");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	private void saveExtendedSFG(CrawlSession session) {
		LOG.info("Saving the SFG...");		
		StateFlowGraph sfg = session.getStateFlowGraph();
		// saving used IDs to be used for mutation testing
		for (StateVertex s: sfg.getAllStates())
			writeEPStateIDToFile(Integer.toString(s.getId()));

		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		String sfgFileName = "sfg_extend.ser";
		// Save the SFG to file
		try {
			fos = new FileOutputStream(sfgFileName);
			out = new ObjectOutputStream(fos);
			out.writeObject(sfg);
			out.close();
			LOG.info("TestSuiteExtension successfully wrote SFG to sfg_extend.ser file");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	private StateFlowGraph loadInitialSFG() {
		LOG.info("Loading the SFG...");
		FileInputStream fis = null;
		ObjectInputStream in = null;
		String sfgFileName = "sfg_init.ser";
		// Read the SFG from file for testing
		StateFlowGraph sfg = null;
		try {
			fis = new FileInputStream(sfgFileName);
			in = new ObjectInputStream(fis);
			sfg = (StateFlowGraph) in.readObject();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//LOG.info(Serializer.toPrettyJson(sfg));
		//if (Serializer.toPrettyJson(sfg).equals(Serializer.toPrettyJson(sfg2)))
		//	LOG.info("ERROR!");

		return sfg;

	}

	private StateFlowGraph loadExtendedSFG() {
		LOG.info("Loading the SFG...");
		FileInputStream fis = null;
		ObjectInputStream in = null;
		String sfgFileName = "sfg_extend.ser";
		// Read the SFG from file for testing
		StateFlowGraph sfg = null;
		try {
			fis = new FileInputStream(sfgFileName);
			in = new ObjectInputStream(fis);
			sfg = (StateFlowGraph) in.readObject();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//LOG.info(Serializer.toPrettyJson(sfg));
		//if (Serializer.toPrettyJson(sfg).equals(Serializer.toPrettyJson(sfg2)))
		//	LOG.info("ERROR!");

		return sfg;

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
		System.out.println("List of asserted element paterns in assertedElementRegions:");

		StateFlowGraph sfg;
		if (loadInitialSFGFromFile)
			sfg = loadInitialSFG();
		else if (loadExtendedSFGFromFile)
			sfg = loadExtendedSFG();
		else{
			sfg = session.getStateFlowGraph();
			if (!appName.equals("claroline"))
				saveExtendedSFG(session);
		}

		for (StateVertex s: sfg.getAllStates()){
			for (AssertedElementRegion	aep: s.getAssertedElementRegions())
				if (aep.getAssertionOrigin().equals("original assertion"))
					originalAssertedElementRegions.add(aep);
		}		

		for (AssertedElementRegion	aep: originalAssertedElementRegions)
			System.out.println(aep.getAssertion());

		System.out.println("***************");


		if (!loadInitialSFGFromFile && !loadExtendedSFGFromFile){
			System.out.println("Training the SVM for assertion prediction...");

			for (int k = 0; k < trainingPositiveSetElementFeatures.size(); k++){
				//System.out.println(trainingNegetiveSetElementFeatures.get(k));
				addToTrainingSet(trainingPositiveSetElementFeatures.get(k));
			}


			// choosing top-k frequent features from the ArrayList to be written in the training set file
			Collections.sort(trainingNegetiveSetElementFeatures,new ElementFeatureComp());
			int maxToSelect = trainingPositiveSetElementFeatures.size();   // cheeos at most to the number of positive vectors
			if (maxToSelect > trainingNegetiveSetElementFeatures.size())
				maxToSelect = trainingNegetiveSetElementFeatures.size();
			for (int k = 0; k < maxToSelect; k++){
				//System.out.println(trainingNegetiveSetElementFeatures.get(k));
				addToTrainingSet(trainingNegetiveSetElementFeatures.get(k));
			}
		}

		// SVM training, generates trainingSet.model file
		svmTrain();

		// DOM-based assertion generation part
		for (StateVertex s: sfg.getAllStates()){

			//System.out.println("Abstract state " + s.getName() + " has " + s.getElementTagAttAssertions().size() + " ElementTagAttAssertions for non-abstract DOM elements.");
			//System.out.println("Abstract state " + s.getName() + " has " + s.getRegionTagAssertions().size() + " RegionTagAssertions for non-abstract DOM elements.");
			//System.out.println("Abstract state " + s.getName() + " has " + s.getRegionSimilarAssertions().size() + " RegionSimilarAssertions for non-abstract DOM elements.");
			//System.out.println("Abstract state " + s.getName() + " has " + s.getRegionFullAssertions().size() + " RegionFullAssertions for non-abstract DOM elements.");


			//System.out.println("DOM on state " + s.getName() + " is: " + s.getDom().replace("\n", "").replace("\r", "").replace(" ", ""));
			//System.out.println("There are " + s.getAssertions().size() + " asserted element regions in state " + s.getName() + " before generation.");
			//if (s.getAssertions().size()>0)
			//	for (int i=0;i<s.getAssertions().size();i++)
			//		System.out.println(s.getAssertions().get(i));


			for (AssertedElementRegion	aep: originalAssertedElementRegions){
				//System.out.println("aep: " +  aep);

				if (!s.getAssertions().contains(aep.getAssertion())){
					try {
						Document dom = DomUtils.asDocument(s.getDom());
						NodeList nodeList = dom.getElementsByTagName(aep.getTagName());

						org.w3c.dom.Element element = null;
						for (int i = 0; i < nodeList.getLength(); i++){
							element = (org.w3c.dom.Element) nodeList.item(i);

							AssertedElementRegion aepTemp = new AssertedElementRegion(element, "", aep.getAssertedElementLocator()); // creating an AssertedElementRegion without any assertion text
							String howElementMatched = aep.getHowElementMatch(aepTemp);
							String howRegionMatched = aep.getHowRegionMatch(aepTemp);

							//aep.getAssertionType();

							// AssertedElement-Level Assertion
							switch (howElementMatched){
							case "ElementFullMatch":
								//System.out.println(aep);
								//System.out.println("ElementFullMatch");
								//System.out.println(aepTemp);
								aepTemp.setAssertion(aep.getAssertion());
								aepTemp.setAssertionOrigin("reused assertion in case of ElementFullMatch");
								s.addAssertedElementRegion(aepTemp); // reuse the same AssertedElementRegion
								break;
							case "ElementTagAttMatch":
								//System.out.println(aep);
								//System.out.println("ElementTagAttMatch");
								s.addAssertedElementRegion(generateElementAssertion(aep, howElementMatched));
								break;
								//case "ElementTagMatch":
								//System.out.println(aep);
								//System.out.println("ElementTagMatch");
								//s.addAssertedElementRegion(generateElementAssertion(aep, howElementMatched));
								//break;
							}

							// AssertedElementRegion-Level Assertion
							switch (howRegionMatched){
							case "RegionFullMatch":
								//System.out.println(aep);
								//System.out.println("RegionFullMatch");
								s.addAssertedElementRegion(generateRegionAssertion(aep, howRegionMatched));
								break;
							case "RegionTagAttMatch":
								//System.out.println(aep);
								//System.out.println("RegionTagAttMatch");
								s.addAssertedElementRegion(generateRegionAssertion(aep, howRegionMatched)); 
								break;
							case "RegionTagMatch":
								//System.out.println(aep);
								//System.out.println("RegionTagMatch");
								s.addAssertedElementRegion(generateRegionAssertion(aep, howRegionMatched)); 
								break;
							}

						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				}

			}

			//System.out.println("There are " + s.getAssertions().size() + " asserted element regions in state " + s.getName() + " after generation.");
			//if (s.getAssertions().size()>0)
			//	for (int i=0;i<s.getAssertions().size();i++)
			//		System.out.println(s.getAssertions().get(i));


			// **************TODO: suppressing redundant assertions ***********
			/*ArrayList<AssertedElementRegion> AEP = s.getAssertedElementPatters();
			ArrayList<AssertedElementRegion> toRemove = new ArrayList<AssertedElementRegion>();


			Element tempElement;
			for (int i=0;i<AEP.size();i++)
				for (int j=0;j<AEP.size();j++){
					if (i!=j){
						if (AEP.get(i).getSourceElement()==AEP.get(j).getSourceElement());
						while (AEP.get(i).getSourceElement().getParentNode().getNodeName().toUpperCase()!="BODY")


						tempElement = tempElement.getParentNode();
					}
				}
			 */


		}

		generateTestSuite(sfg);

		LOG.info("#states in the final SFG: " + sfg.getNumberOfStates());	
		LOG.info("#transitions in the final SFG: " + sfg.getAllEdges().size());	


		LOG.info("TestSuiteExtension plugin has finished");
	}

	// Training the SVM
	private void svmTrain() {
		String arguments[] = {"trainingSet"};
		svm_train t = new svm_train();
		try {
			t.run(arguments);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}


	// SVM predicting for a feature vector
	private boolean svmPredict(ElementFeatures ef) {
		svm_model model;
		try {
			model = svm.svm_load_model("trainingSet.model");

			svm_node[] x = new svm_node[8];
			for(int j=0;j<8;j++){
				x[j] = new svm_node();
				x[j].index = j+1;
			}

			/*
				x[0].value = ef.getFreshness();
				x[1].value = ef.getTextImportance();
				x[2].value = ef.getNormalBlockWidth();
				x[3].value = ef.getNormalBlockHeight();
				x[4].value = ef.getNormalBlockCenterX();
				x[5].value = ef.getNormalBlockCenterY();
				x[6].value = ef.getInnerHtmlDensity();
				x[7].value = ef.getLinkDensity();
				x[8].value = ef.getBlockDensity();
			 */

			x[0].value = ef.getTextImportance();
			x[1].value = ef.getNormalBlockWidth();
			x[2].value = ef.getNormalBlockHeight();
			x[3].value = ef.getNormalBlockCenterX();
			x[4].value = ef.getNormalBlockCenterY();
			x[5].value = ef.getInnerHtmlDensity();
			x[6].value = ef.getLinkDensity();
			//x[7].value = ef.getBlockDensity();
			x[7].value = ef.getNormalNumOfChildren();

			int prediction = (int) svm.svm_predict(model,x);
			if (prediction == 1){
				return true;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return false;
	}

	// SVM predicting for feature vectors in a state
	private ArrayList<String> svmPredict(StateVertex state) {
		ArrayList<String> xpathList = new ArrayList<String>();

		svm_model model;
		try {
			model = svm.svm_load_model("trainingSet.model");
			for (ElementFeatures ef: state.getElementFeatures()){

				svm_node[] x = new svm_node[8];
				for(int j=0;j<8;j++){
					x[j] = new svm_node();
					x[j].index = j+1;
				}

				/*
				x[0].value = ef.getFreshness();
				x[1].value = ef.getTextImportance();
				x[2].value = ef.getNormalBlockWidth();
				x[3].value = ef.getNormalBlockHeight();
				x[4].value = ef.getNormalBlockCenterX();
				x[5].value = ef.getNormalBlockCenterY();
				x[6].value = ef.getInnerHtmlDensity();
				x[7].value = ef.getLinkDensity();
				x[8].value = ef.getBlockDensity();
				 */

				x[0].value = ef.getTextImportance();
				x[1].value = ef.getNormalBlockWidth();
				x[2].value = ef.getNormalBlockHeight();
				x[3].value = ef.getNormalBlockCenterX();
				x[4].value = ef.getNormalBlockCenterY();
				x[5].value = ef.getInnerHtmlDensity();
				x[6].value = ef.getLinkDensity();
				//x[7].value = ef.getBlockDensity();
				x[7].value = ef.getNormalNumOfChildren();


				int prediction = (int) svm.svm_predict(model,x);
				if (prediction == 1)
					xpathList.add(ef.getXpath());
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return xpathList;
	}



	private AssertedElementRegion generateRegionAssertion(AssertedElementRegion aep, String howMatched) {
		// region assertion on BODY is useless
		if (aep.getTagName().toUpperCase().equals("BODY"))
			return null;

		String elementTag = aep.getTagName();
		if (elementTag.equals("")) // do not create AEP-level assertion for undefined asserted elements (such as those on title, alerts, url, etc.)
			return null;

		String elementText = "";//aep.getTextContent();
		if (aep.getTextContent().length() < 50)
			elementText = aep.getTextContent();
		ArrayList<String> elementAttributes = new ArrayList<String>(aep.getAttributes());

		String parentTag =  aep.getParentTagName();
		String parentText =  "";//aep.getParentTextContent();
		if (aep.getParentTextContent().length() < 50)
			elementText = aep.getParentTextContent();
		ArrayList<String> parentAttributes = new ArrayList<String>(aep.getParentAttributes());

		ArrayList<String> childrenTags = new ArrayList<String>(aep.getChildrenTagName());
		ArrayList<String> childrenTexts = new ArrayList<String>();

		for (String text: aep.getChildrenTextContent())
			if (text.length() < 50)
				childrenTexts.add(text);
			else
				childrenTexts.add("");


		ArrayList<ArrayList<String>> childrenAttributes = new ArrayList<ArrayList<String>>();
		for (int i=0; i < aep.getChildrenAttributes().size(); i++)
			childrenAttributes.add(aep.getChildrenAttributes().get(i));


		// DOMElement element = new DOMElement(String tagName, String textContent, ArrayList<String> attributes);
		String regionCheckAssertion = "element = new DOMElement(\"" + elementTag + "\", \"" + elementText.replace("\"", "\\\"") + "\", new ArrayList<String>(Arrays.asList(\"";
		if (elementAttributes.size()>0){ // avoiding null
			for (int j=0; j < elementAttributes.size()-1; j++)
				regionCheckAssertion += elementAttributes.get(j).replace("\"", "\\\"") + "\",\"";
			regionCheckAssertion += elementAttributes.get(elementAttributes.size()-1).replace("\"", "\\\"") + "\")));\n";
		}else
			regionCheckAssertion += "\")));\n";

		regionCheckAssertion += "\t\tparentElement = new DOMElement(\"" + parentTag + "\", \"" + parentText.replace("\"", "\\\"") + "\", new ArrayList<String>(Arrays.asList(\"";
		if (parentAttributes.size()>0){ // avoiding null
			for (int j=0; j < parentAttributes.size()-1; j++)
				regionCheckAssertion += parentAttributes.get(j).replace("\"", "\\\"") + "\",\"";
			regionCheckAssertion += parentAttributes.get(parentAttributes.size()-1).replace("\"", "\\\"") + "\")));\n";
		}else
			regionCheckAssertion += "\")));\n";

		regionCheckAssertion += "\t\tchildrenElements.clear();\n";
		for (int k=0; k<childrenTags.size(); k++){
			regionCheckAssertion += "childrenElements.add(new DOMElement(\"" + childrenTags.get(k) + "\", \"" + childrenTexts.get(k).replace("\"", "\\\"") + "\", new ArrayList<String>(Arrays.asList(\"";
			//regionCheckAssertion += "\t\tchildrenElements.add(new DOMElement(\"" + childrenTags.get(k) + "\", \"\", new ArrayList<String>(Arrays.asList(\"";
			if (k < childrenAttributes.size() && childrenAttributes.get(k).size()>0){ // check if attributes are less than tags due to being null
				for (int j=0; j < childrenAttributes.get(k).size()-1; j++)
					regionCheckAssertion += childrenAttributes.get(k).get(j).replace("\"", "\\\"") + "\",\"";
				regionCheckAssertion += childrenAttributes.get(k).get(childrenAttributes.get(k).size()-1).replace("\"", "\\\"") + "\"))));\n";
			}else
				regionCheckAssertion += "\"))));\n";
		}

		if (howMatched.equals("RegionTagMatch"))
			regionCheckAssertion += "\t\tassertTrue(isElementRegionTagPresent(parentElement , element, childrenElements))";
		else if (howMatched.equals("RegionTagAttMatch") || howMatched.equals("SimilarAssertion"))
			regionCheckAssertion += "\t\tassertTrue(isElementRegionTagAttPresent(parentElement , element, childrenElements))";
		else
			regionCheckAssertion += "\t\tassertTrue(isElementRegionFullPresent(parentElement , element, childrenElements))";


		AssertedElementRegion aepMatch = new AssertedElementRegion(aep.getSourceElement(), regionCheckAssertion, aep.getAssertedElementLocator());
		aepMatch.setAssertionOrigin("generated assertion in case of " + howMatched);


		//System.out.println(aepMatch);

		return aepMatch;
	}


	private AssertedElementRegion generateElementAssertion(AssertedElementRegion aep, String howMatched) {
		// generating an AssertedElementRegion based on the matching result
		AssertedElementRegion newaep = new AssertedElementRegion(aep.getSourceElement(), "", aep.getAssertedElementLocator()); // creating an AssertedElementRegion without any assertion text

		if (newaep.getTagName().toUpperCase().equals("BODY"))
			return null;


		ArrayList<String> atts = new ArrayList<String>(aep.getAttributes());
		String locator = newaep.getAssertedElementLocator();
		//locator.replace("\")", "\")");
		String newLocator ="";

		// To combine the css locators for multiple attributes:
		// => css=E[foo='abc'][foo1='abcs'][foo2='abcd'] and so on...
		// To combine the xpath locators for multiple attributes:
		// => //E[@foo="bar" and @foo1="bar1"]

		if (locator.contains("xpath")){
			if (locator.contains("]\")")){	// xpath finished with ]
				newLocator += locator.replace("]\")", "");
				if (atts.size()>0){ // avoiding null
					for (int j=0; j < atts.size()-1; j++){
						newLocator += " @" + atts.get(j).replace("\"", "\\\"") + " and ";
					}
					newLocator += atts.get(atts.size()-1).replace("\"", "\\\"");
				}
				newLocator += "]\")";
			}else{  // xpath finished withouth ]
				newLocator += locator.replace("\")", "") + "[";
				if (atts.size()>0){ // avoiding null
					for (int j=0; j < atts.size()-1; j++){
						newLocator += "@" + atts.get(j).replace("\"", "\\\"") + " and @";
					}
					newLocator += atts.get(atts.size()-1).replace("\"", "\\\"");
				}
				newLocator += "]\")";

			}
		}else if (locator.contains("cssSelector")){
			newLocator += locator.replace("\")", "") + "[";
			if (atts.size()>0){ // avoiding null
				for (int j=0; j < atts.size()-1; j++){
					newLocator += atts.get(j).replace("\"", "\\\"") + "][";
				}
				newLocator += atts.get(atts.size()-1).replace("\"", "\\\"");
			}
			newLocator += "]\")";
		} else{
			newLocator = locator;
		}


		switch (howMatched){
		case "ElementTagAttMatch":
			newaep.setAssertion("assertTrue(isElementPresent("+ newLocator +"))");
			//System.out.println(newaep);
			break;
		case "ElementTagMatch":
			newaep.setAssertion("assertTrue(isElementPresent(By.tagName(\"" + newaep.getTagName() +"\")))");
			//System.out.println(newaep);
			break;
		}
		newaep.setAssertionOrigin("generated assertion in case of " + howMatched);
		return newaep;
	}



	/**
	 * Generating the extended test suite in multiple files
	 */
	private void generateTestSuite(StateFlowGraph sfg) {

		// This is to store which states were observed in this test case
		writeStatesForTestCasesToFile("int [][] " + testSuiteNameToGenerate + " = new int[100][300];");


		List<List<GraphPath<StateVertex, Eventable>>> results = sfg.getAllPossiblePaths(sfg.getInitialState());
		ArrayList<TestMethod> testMethods = new ArrayList<TestMethod>();
		ArrayList<TestMethod> checkMethods = new ArrayList<TestMethod>(); // these methods checks the assertions for each state
		String how, howValue, sendValue;

		int counter = 0, totalAssertions = 0, predictedAssertions = 0, origAndReusedAssertions = 0, reusedAssertions = 0, generatedAssertions = 0, 
				ElementFullMatch = 0, ElementTagAttMatch = 0, RegionFullMatch = 0, RegionTagAttMatch = 0, RegionTagMatch = 0, AEPforOriginalAssertions=0;

		int randElementTagAttAssertionsOnStates = 0, randRegionTagAssertionsOnStates =0, randRegionTagAttAssertionsOnStates =0, randRegionFullAssertionsOnStates = 0;

		List<String> randomAssertionsPool = new ArrayList<String>();

		int numberOfStatesInTestSuite = 0;


		for (List<GraphPath<StateVertex, Eventable>> paths : results) {
			//For each new sink node

			for (GraphPath<StateVertex, Eventable> p : paths) {
				//For each path to the sink node
				TestMethod testMethod = new TestMethod("method" + Integer.toString(counter));

				int pathLength = p.getEdgeList().size();
				int pathCount = 0;

				//printing generated paths
				//System.out.print("0->");

				for (Eventable edge : p.getEdgeList()) {
					//For each eventable in the path
					pathCount++;

					writeStatesForTestCasesToFile(testSuiteNameToGenerate + "[" + counter + "][" + Integer.toString(edge.getSourceStateVertex().getId()) + "] = 1;");

					testMethod.addStatement("//From state " + Integer.toString(edge.getSourceStateVertex().getId()) 
							+ " to state " + Integer.toString(edge.getTargetStateVertex().getId()));
					testMethod.addStatement("//" + edge.toString());

					//printing generated paths
					//System.out.print(edge.getTargetStateVertex().getId() + "->");
					//System.out.println("//From state " + edge.getSourceStateVertex().getId() + " to state " + edge.getTargetStateVertex().getId());
					//System.out.println("//" + edge.toString());

					numberOfStatesInTestSuite++;

					// adding DOM-mutator to be used for mutation testing of generated assertions, it stores DOM states before mutating it
					testMethod.addStatement("mutateDOMTree(" + edge.getSourceStateVertex().getId() + ");");

					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_OriginalAssertions();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_ReusedAssertions();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_GeneratedAssertions();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_LearnedAssertions();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_AllAssertions();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions1();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions2();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions3();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions4();");
					testMethod.addStatement("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions5();");

					TestMethod checkMethod1 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_OriginalAssertions");
					TestMethod checkMethod2 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_ReusedAssertions");
					TestMethod checkMethod3 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_GeneratedAssertions");
					TestMethod checkMethod4 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_LearnedAssertions");
					TestMethod checkMethod5 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_AllAssertions");

					TestMethod checkMethod6 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions1");
					TestMethod checkMethod7 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions2");
					TestMethod checkMethod8 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions3");
					TestMethod checkMethod9 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions4");
					TestMethod checkMethod10 = new TestMethod("checkState" + edge.getSourceStateVertex().getId() + "_RandAssertions5");

					/* Adding 5 random assertions */
					if (addRandomAssertions==true){
						
						// applying randomized assertion generation
						randomAssertionsPool.clear();
						// shuffling the assertions to select a random subset
						for (String assertion: edge.getSourceStateVertex().getElementTagAttAssertions())
							if (assertion.length()<4000)
								randomAssertionsPool.add(assertion);
						for (String assertion: edge.getSourceStateVertex().getRegionTagAssertions())
							if (assertion.length()<4000)
								randomAssertionsPool.add(assertion);
						for (String assertion: edge.getSourceStateVertex().getRegionTagAttAssertions())
							if (assertion.length()<4000)
								randomAssertionsPool.add(assertion);
						for (String assertion: edge.getSourceStateVertex().getRegionFullAssertions())
							if (assertion.length()<4000)
								randomAssertionsPool.add(assertion);

						int maxToSelelect = 5;
						if (randomAssertionsPool.size() < maxToSelelect)
							maxToSelelect = randomAssertionsPool.size();

						Collections.shuffle(randomAssertionsPool);
						for (int i=0; i<maxToSelelect; i++)
							checkMethod6.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
						Collections.shuffle(randomAssertionsPool);
						for (int i=0; i<maxToSelelect; i++)
							checkMethod7.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
						Collections.shuffle(randomAssertionsPool);
						for (int i=0; i<maxToSelelect; i++)
							checkMethod8.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
						Collections.shuffle(randomAssertionsPool);
						for (int i=0; i<maxToSelelect; i++)
							checkMethod9.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
						Collections.shuffle(randomAssertionsPool);
						for (int i=0; i<maxToSelelect; i++)
							checkMethod10.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
					}

					// Adding original assertion to the method
					if (addOriginalAssertions){
						if (edge.getSourceStateVertex().getAssertions().size()>0){
							for (int i=0;i<edge.getSourceStateVertex().getAssertedElementRegions().size();i++){
								String assertion = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertion();
								String assertionOringin = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
								if (assertionOringin.contains("original assertion")){
									checkMethod1.addStatement(assertion + "; // " + assertionOringin+"\n");
									if (addAllAssertions){
										checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
										totalAssertions++;
									}
									origAndReusedAssertions++;
								}
							}

						}
					}

					// Adding reused/generated assertions
					if (edge.getSourceStateVertex().getAssertions().size()>0)
						for (int i=0;i<edge.getSourceStateVertex().getAssertedElementRegions().size();i++){
							String assertion = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertion();
							String assertionOringin = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 

							// problem with Claroline app
							if (edge.getSourceStateVertex().getId()==0 && assertion.equals("assertTrue(isElementPresent(By.linkText(\"Logout\")))"))
								continue;

							// Adding reused assertion to the method
							if (addReusedAssertions){
								if (assertionOringin.contains("reused assertion")){
									checkMethod2.addStatement(assertion + "; // " + assertionOringin);
									if (addAllAssertions){
										checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
										totalAssertions++;
									}
									reusedAssertions++;
									ElementFullMatch++;
								}
							}
						}


					// prioritizing assertions: add RegionFullMatch as much as possible 
					if (edge.getSourceStateVertex().getAssertions().size()>0)
						for (int i=0;i<edge.getSourceStateVertex().getAssertedElementRegions().size();i++){
							String assertion = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertion();
							if (assertion.length()>4000)
								continue;
							String assertionOringin = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
							// Adding generated assertion to the method
							if (addGeneratedAssertions){
								if (assertionOringin.contains("generated assertion")){
									//testMethod.addStatement("\n");
									if (assertionOringin.contains("RegionFullMatch") || assertionOringin.contains("AEP for Original")){
										if (checkMethod3.getStatements().size() < 5)
											checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
										if (checkMethod5.getStatements().size() < 5)
											if (addAllAssertions){
												checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
												totalAssertions++;
											}
										generatedAssertions++;
										if (assertionOringin.contains("RegionFullMatch"))
											RegionFullMatch++;
										if (assertionOringin.contains("AEP for Original"))
											AEPforOriginalAssertions++;	
									}
								}
							}
						}
					// RegionTagAttMatch
					if (edge.getSourceStateVertex().getAssertions().size()>0)
						for (int i=0;i<edge.getSourceStateVertex().getAssertedElementRegions().size();i++){
							String assertion = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertion();
							if (assertion.length()>4000)
								continue;
							String assertionOringin = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
							// Adding generated assertion to the method
							if (addGeneratedAssertions){
								if (assertionOringin.contains("generated assertion")){
									//testMethod.addStatement("\n");
									if (assertionOringin.contains("RegionTagAttMatch")){
										if (checkMethod3.getStatements().size() < 5)
											checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
										if (checkMethod5.getStatements().size() < 5)
											if (addAllAssertions){
												checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
												totalAssertions++;
											}
										generatedAssertions++;
										if (assertionOringin.contains("RegionTagAttMatch"))
											RegionTagAttMatch++;
									}
								}
							}
						}

					// Similar Region
					// Adding learned assertions (SVM predicted for a feature vector)
					if (addLearnedAssertions){
						// Adding SP assertion
						HashSet<String> uniqueAssertions = new HashSet<String>();

						if (edge.getSourceStateVertex().getId()==0){
							for (ElementFeatures ef: indexPageElementsFeatures)
								edge.getSourceStateVertex().addElementFeatures(ef);
						}

						for (ElementFeatures ef: edge.getSourceStateVertex().getElementFeatures()){
							if (svmPredict(ef)==true){
								String elementRegionAssertion = ef.getElementRegionAssertion();
								if (elementRegionAssertion!=null && elementRegionAssertion.length()<4000)
									uniqueAssertions.add(elementRegionAssertion);
							}
						}
						for (String assertion: uniqueAssertions){
							if (checkMethod4.getStatements().size() < 5)
								checkMethod4.addStatement(assertion + "; // predicted region assertion\n");
							if (checkMethod5.getStatements().size() < 5)
								if (addAllAssertions){
									checkMethod5.addStatement(assertion + "; // predicted region assertion\n"); // add to all_assertions
									totalAssertions++;
								}
							predictedAssertions++;
						}
					}

					// ElementTagAttMatch
					if (edge.getSourceStateVertex().getAssertions().size()>0)
						for (int i=0;i<edge.getSourceStateVertex().getAssertedElementRegions().size();i++){
							String assertion = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertion();
							if (assertion.length()>4000)
								continue;
							String assertionOringin = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
							// Adding generated assertion to the method
							if (addGeneratedAssertions){
								if (assertionOringin.contains("generated assertion")){
									//testMethod.addStatement("\n");
									if (assertionOringin.contains("ElementTagAttMatch")){
										if (checkMethod3.getStatements().size() < 5)
											checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
										if (checkMethod5.getStatements().size() < 5)
											if (addAllAssertions){
												checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
												totalAssertions++;
											}
										generatedAssertions++;
										if (assertionOringin.contains("ElementTagAttMatch"))
											RegionTagMatch++;
									}
								}
							}
						}

					// RegionTagMatch
					if (edge.getSourceStateVertex().getAssertions().size()>0)
						for (int i=0;i<edge.getSourceStateVertex().getAssertedElementRegions().size();i++){
							String assertion = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertion();
							if (assertion.length()>4000)
								continue;
							String assertionOringin = edge.getSourceStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
							// Adding generated assertion to the method
							if (addGeneratedAssertions){
								if (assertionOringin.contains("generated assertion")){
									//testMethod.addStatement("\n");
									if (assertionOringin.contains("RegionTagMatch")){
										if (checkMethod3.getStatements().size() < 5)
											checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
										if (checkMethod5.getStatements().size() < 5)
											if (addAllAssertions){
												checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
												totalAssertions++;
											}
										generatedAssertions++;
										if (assertionOringin.contains("RegionTagMatch"))
											RegionTagMatch++;
									}
								}
							}
						}




					// applying the click
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

									if (sendValue.startsWith("RND") && sendValue.contains("@example.com")){ // only for wolfCMS case of unique email address
										testMethod.addStatement("String RandValue = \"RND\" + new RandomInputValueGenerator().getRandomString(4) + \"@example.com\";");
										testMethod.addStatement("driver.findElement(By." + how + "(\"" + howValue + "\")).sendKeys(RandValue);");
									}
									if (sendValue.startsWith("RND")){
										testMethod.addStatement("String RandValue = \"RND\" + new RandomInputValueGenerator().getRandomString(4);");
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


					// adding the check state method to the file
					checkMethods.add(checkMethod1);
					checkMethods.add(checkMethod2);
					checkMethods.add(checkMethod3);
					checkMethods.add(checkMethod4);
					checkMethods.add(checkMethod5);
					checkMethods.add(checkMethod6);
					checkMethods.add(checkMethod7);
					checkMethods.add(checkMethod8);
					checkMethods.add(checkMethod9);
					checkMethods.add(checkMethod10);

					/*
					 * Now for the sink nodes, add mutations and assertions:
					 */
					if (pathCount==pathLength){
						testMethod.addStatement("//Sink node at state " + Integer.toString(edge.getTargetStateVertex().getId()));

						writeStatesForTestCasesToFile(testSuiteNameToGenerate + "[" + counter + "][" + Integer.toString(edge.getTargetStateVertex().getId()) + "] = 1;");

						numberOfStatesInTestSuite++;

						// adding DOM-mutator to be used for mutation testing of generated assertions, it stores DOM states before mutating it
						testMethod.addStatement("mutateDOMTree(" + edge.getTargetStateVertex().getId() + ");");

						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_OriginalAssertions();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_ReusedAssertions();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_GeneratedAssertions();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_LearnedAssertions();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_AllAssertions();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions1();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions2();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions3();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions4();");
						testMethod.addStatement("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions5();");

						checkMethod1 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_OriginalAssertions");
						checkMethod2 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_ReusedAssertions");
						checkMethod3 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_GeneratedAssertions");
						checkMethod4 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_LearnedAssertions");
						checkMethod5 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_AllAssertions");

						checkMethod6 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions1");
						checkMethod7 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions2");
						checkMethod8 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions3");
						checkMethod9 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions4");
						checkMethod10 = new TestMethod("checkState" + edge.getTargetStateVertex().getId() + "_RandAssertions5");


						// adding random assertions
						/* Adding 5 random assertions */
						if (addRandomAssertions==true){

							// applying randomized assertion generation
							randomAssertionsPool.clear();
							// shuffling the assertions to select a random subset
							for (String assertion: edge.getTargetStateVertex().getElementTagAttAssertions())
								if (assertion.length()<4000)
									randomAssertionsPool.add(assertion);
							for (String assertion: edge.getTargetStateVertex().getRegionTagAssertions())
								if (assertion.length()<4000)
									randomAssertionsPool.add(assertion);
							for (String assertion: edge.getTargetStateVertex().getRegionTagAttAssertions())
								if (assertion.length()<4000)
									randomAssertionsPool.add(assertion);
							for (String assertion: edge.getTargetStateVertex().getRegionFullAssertions())
								if (assertion.length()<4000)
									randomAssertionsPool.add(assertion);


							int maxToSelelect = 5;
							if (randomAssertionsPool.size() < maxToSelelect)
								maxToSelelect = randomAssertionsPool.size();

							Collections.shuffle(randomAssertionsPool);
							for (int i=0; i<maxToSelelect; i++)
								checkMethod6.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
							Collections.shuffle(randomAssertionsPool);
							for (int i=0; i<maxToSelelect; i++)
								checkMethod7.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
							Collections.shuffle(randomAssertionsPool);
							for (int i=0; i<maxToSelelect; i++)
								checkMethod8.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
							Collections.shuffle(randomAssertionsPool);
							for (int i=0; i<maxToSelelect; i++)
								checkMethod9.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
							Collections.shuffle(randomAssertionsPool);
							for (int i=0; i<maxToSelelect; i++)
								checkMethod10.addStatement(randomAssertionsPool.get(i) + "; // Random element assertion");
						}


						// Adding original assertion to the method
						if (addOriginalAssertions){
							if (edge.getTargetStateVertex().getAssertions().size()>0){
								for (int i=0;i<edge.getTargetStateVertex().getAssertedElementRegions().size();i++){
									String assertion = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertion();
									String assertionOringin = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
									if (assertionOringin.contains("original assertion")){
										checkMethod1.addStatement(assertion + "; // " + assertionOringin+"\n");
										if (addAllAssertions){
											checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
											totalAssertions++;
										}
										origAndReusedAssertions++;
									}
								}

							}
						}

						// Adding reused/generated assertions
						if (edge.getTargetStateVertex().getAssertions().size()>0)
							for (int i=0;i<edge.getTargetStateVertex().getAssertedElementRegions().size();i++){
								String assertion = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertion();
								String assertionOringin = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 

								// problem with Claroline app
								if (edge.getTargetStateVertex().getId()==0 && assertion.equals("assertTrue(isElementPresent(By.linkText(\"Logout\")))"))
									continue;

								// Adding reused assertion to the method
								if (addReusedAssertions){
									if (assertionOringin.contains("reused assertion")){
										checkMethod2.addStatement(assertion + "; // " + assertionOringin);
										if (addAllAssertions){
											checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
											totalAssertions++;
										}
										reusedAssertions++;
										ElementFullMatch++;
									}
								}
							}


						// prioritizing assertions: add RegionFullMatch as much as possible 
						if (edge.getTargetStateVertex().getAssertions().size()>0)
							for (int i=0;i<edge.getTargetStateVertex().getAssertedElementRegions().size();i++){
								String assertion = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertion();
								if (assertion.length()>4000)
									continue;
								String assertionOringin = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
								// Adding generated assertion to the method
								if (addGeneratedAssertions){
									if (assertionOringin.contains("generated assertion")){
										//testMethod.addStatement("\n");
										if (assertionOringin.contains("RegionFullMatch") || assertionOringin.contains("AEP for Original")){
											if (checkMethod3.getStatements().size() < 5)
												checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
											if (checkMethod5.getStatements().size() < 5)
												if (addAllAssertions){
													checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
													totalAssertions++;
												}
											generatedAssertions++;
											if (assertionOringin.contains("RegionFullMatch"))
												RegionFullMatch++;
											if (assertionOringin.contains("AEP for Original"))
												AEPforOriginalAssertions++;	
										}
									}
								}
							}
						// RegionTagAttMatch
						if (edge.getTargetStateVertex().getAssertions().size()>0)
							for (int i=0;i<edge.getTargetStateVertex().getAssertedElementRegions().size();i++){
								String assertion = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertion();
								if (assertion.length()>4000)
									continue;
								String assertionOringin = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
								// Adding generated assertion to the method
								if (addGeneratedAssertions){
									if (assertionOringin.contains("generated assertion")){
										//testMethod.addStatement("\n");
										if (assertionOringin.contains("RegionTagAttMatch")){
											if (checkMethod3.getStatements().size() < 5)
												checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
											if (checkMethod5.getStatements().size() < 5)
												if (addAllAssertions){
													checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
													totalAssertions++;
												}
											generatedAssertions++;
											if (assertionOringin.contains("RegionTagAttMatch"))
												RegionTagAttMatch++;
										}
									}
								}
							}

						// Similar Region
						// Adding learned assertions (SVM predicted for a feature vector)
						if (addLearnedAssertions){
							// Adding SP assertion
							HashSet<String> uniqueAssertions = new HashSet<String>();

							if (edge.getTargetStateVertex().getId()==0){
								for (ElementFeatures ef: indexPageElementsFeatures)
									edge.getTargetStateVertex().addElementFeatures(ef);
							}

							for (ElementFeatures ef: edge.getTargetStateVertex().getElementFeatures()){
								if (svmPredict(ef)==true){
									String elementRegionAssertion = ef.getElementRegionAssertion();
									if (elementRegionAssertion!=null && elementRegionAssertion.length()<4000)
										uniqueAssertions.add(elementRegionAssertion);
								}
							}
							for (String assertion: uniqueAssertions){
								if (checkMethod4.getStatements().size() < 5)
									checkMethod4.addStatement(assertion + "; // predicted region assertion\n");
								if (checkMethod5.getStatements().size() < 5)
									if (addAllAssertions){
										checkMethod5.addStatement(assertion + "; // predicted region assertion\n"); // add to all_assertions
										totalAssertions++;
									}
								predictedAssertions++;
							}
						}

						// ElementTagAttMatch
						if (edge.getTargetStateVertex().getAssertions().size()>0)
							for (int i=0;i<edge.getTargetStateVertex().getAssertedElementRegions().size();i++){
								String assertion = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertion();
								if (assertion.length()>4000)
									continue;
								String assertionOringin = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
								// Adding generated assertion to the method
								if (addGeneratedAssertions){
									if (assertionOringin.contains("generated assertion")){
										//testMethod.addStatement("\n");
										if (assertionOringin.contains("ElementTagAttMatch")){
											if (checkMethod3.getStatements().size() < 5)
												checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
											if (checkMethod5.getStatements().size() < 5)
												if (addAllAssertions){
													checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
													totalAssertions++;
												}
											generatedAssertions++;
											if (assertionOringin.contains("ElementTagAttMatch"))
												RegionTagMatch++;
										}
									}
								}
							}

						// RegionTagMatch
						if (edge.getTargetStateVertex().getAssertions().size()>0)
							for (int i=0;i<edge.getTargetStateVertex().getAssertedElementRegions().size();i++){
								String assertion = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertion();
								if (assertion.length()>4000)
									continue;
								String assertionOringin = edge.getTargetStateVertex().getAssertedElementRegions().get(i).getAssertionOrigin(); 
								// Adding generated assertion to the method
								if (addGeneratedAssertions){
									if (assertionOringin.contains("generated assertion")){
										//testMethod.addStatement("\n");
										if (assertionOringin.contains("RegionTagMatch")){
											if (checkMethod3.getStatements().size() < 5)
												checkMethod3.addStatement(assertion + "; // " + assertionOringin+"\n");
											if (checkMethod5.getStatements().size() < 5)
												if (addAllAssertions){
													checkMethod5.addStatement(assertion + "; // " + assertionOringin+"\n"); // add to all_assertions
													totalAssertions++;
												}
											generatedAssertions++;
											if (assertionOringin.contains("RegionTagMatch"))
												RegionTagMatch++;
										}
									}
								}
							}




						// adding the check state method to the file
						checkMethods.add(checkMethod1);
						checkMethods.add(checkMethod2);
						checkMethods.add(checkMethod3);
						checkMethods.add(checkMethod4);
						checkMethods.add(checkMethod5);
						checkMethods.add(checkMethod6);
						checkMethods.add(checkMethod7);
						checkMethods.add(checkMethod8);
						checkMethods.add(checkMethod9);
						checkMethods.add(checkMethod10);
					}


				}				


				// adding the test method to the file
				testMethods.add(testMethod);

				String packagename = "com.crawljax.plugins.testsuiteextension.generated." + testSuiteNameToGenerate;

				String TEST_SUITE_PATH = "src/test/java/com/crawljax/plugins/testsuiteextension/generated/" + testSuiteNameToGenerate;

				String CLASS_NAME = "GeneratedTestCase"+ Integer.toString(counter);
				String FILE_NAME_TEMPLATE = "TestCase.vm";

				try {
					DomUtils.directoryCheck(TEST_SUITE_PATH);
					String fileName = null;

					JavaTestGenerator generator =
							new JavaTestGenerator(packagename, CLASS_NAME, sfg.getInitialState().getUrl(), testMethods, checkMethods);

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
				checkMethods.clear();
			}
		}

		System.out.print(finalReport);

		System.out.println("Total #sink nodes:" + results.size());




		System.out.println("Total #assertions in the original test suite:" + originalAssertedElementRegions.size());

		System.out.println("Total #assertions in the test suite from happy paths (origAndReusedAssertions): " + origAndReusedAssertions);

		System.out.println("Total #assertions in the final extended test suite:" + totalAssertions);

		//System.out.println("Total #reused (cloned in other states) assertions in the extended test suite: " + reusedAssertions);

		int reusedOrigAssertions = origAndReusedAssertions-originalAssertedElementRegions.size(); // original assertions that are reused in extended paths		
		reusedAssertions += reusedOrigAssertions;

		System.out.println("Total #reused assertions (cloned + added) in the extended test suite: " + reusedAssertions);
		System.out.println("Total #generated assertions in the extended test suite: " + generatedAssertions);


		System.out.println("Total #ElementFullMatch: " + ElementFullMatch);
		System.out.println("Total #ElementTagAttMatch: " + ElementTagAttMatch);
		//System.out.println("Total #ElementTagMatch: " + ElementTagMatch);
		System.out.println("Total #RegionFullMatch: " + RegionFullMatch);
		System.out.println("Total #RegionTagAttMatch: " + RegionTagAttMatch);
		System.out.println("Total #RegionTagMatch: " + RegionTagMatch);
		System.out.println("Total #predictedAssertions: " + predictedAssertions);
		System.out.println("Total #AEPforOriginalAssertions: " + AEPforOriginalAssertions);

		System.out.println("numberOfStatesInTestSuite: " + numberOfStatesInTestSuite);

		System.out.println("Average number of assertions per state: " + totalAssertions / numberOfStatesInTestSuite);


		System.out.println("Total #random assertions: " + (randElementTagAttAssertions + randRegionTagAssertions + randRegionFullAssertions));
		System.out.println("Total #random element assertions: " + randElementTagAttAssertions);
		System.out.println("Total #random region assertions: " + (randRegionTagAssertions + randRegionFullAssertions));

		System.out.println("randElementTagAttAssertions: " + randElementTagAttAssertions);
		System.out.println("randRegionTagAssertions: " + randRegionTagAssertions);
		System.out.println("randRegionFullAssertions: " + randRegionFullAssertions);

		System.out.println("randElementTagAttAssertionsOnStates: " + randElementTagAttAssertionsOnStates);
		System.out.println("randRegionTagAssertionsOnStates:" + randRegionTagAssertionsOnStates);
		System.out.println("randRegionFullAssertionsOnStates: " + randRegionFullAssertionsOnStates);

		int sum = randElementTagAttAssertionsOnStates + randRegionTagAssertionsOnStates + randRegionFullAssertionsOnStates;
		System.out.println("Average number of random assertions per state: " + sum / 66);
	}



	@Override
	public String toString() {
		return "TestSuiteExtension plugin";
	}

	@Override
	public void onUrlLoad(CrawlerContext context) {
		// TODO Reset for crawling from states in the happy paths
	}

	/*
	 * After a successful event firing, calculate the code coverage
	 */
	@Override
	public void onFireEvent(CrawlerContext context, StateVertex stateBefore, Eventable eventable, StateVertex stateAfter) {

		// New idea for storing all elements so that later can be used for random assertion generation method
		// Note that "stateAfter" is not added yet to the SFG and might be a clone state.

		//This code was browser-memory-inefficient and replaced with a slower but more memory-efficient one
		/*String jscript = "function getElementXPath(elt) " +   
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

				"var all = document.getElementsByTagName(\"*\");" +
				"var allXPaths = new Array();" +
				"for (var i=0, max=all.length; i < max; i++) {" +
				"allXPaths[i] = getElementXPath(all[i]);" +
				"}" +

				"return allXPaths;";
		 */


		// This is to be used if resulted in a clone state (clone abstract state)
		try {

			Object webelementObjs = this.browser.executeJavaScript("return document.getElementsByTagName(\"*\");");
			ArrayList webelements = (ArrayList) webelementObjs;
			WebElement webelement = null;
			for (int i=0;i<webelements.size();i++){
				webelement  = (WebElement) webelements.get(i);
				String xpath = getXPath(webelement);
				Element element = getElementFromXpath(xpath, browser);
				if (element.getTagName().toUpperCase().equals("HTML") || 
						element.getTagName().toUpperCase().equals("HEAD") ||
						element.getTagName().toUpperCase().equals("META") ||
						element.getTagName().toUpperCase().equals("BODY"))
					continue;
				//System.out.println("The DOM element is: " + element);

				NamedNodeMap elementAttList = element.getAttributes();
				ArrayList<String> atts = new ArrayList<String>();
				for (int j = 0; j < elementAttList.getLength(); j++)
					atts.add(elementAttList.item(j).getNodeName() + "=\"" + elementAttList.item(j).getNodeValue() + "\"");

				String newLocator ="";

				// To combine the xpath locators for multiple attributes:
				// => //E[@foo="bar" and @foo1="bar1"]

				if (xpath.endsWith("]")){	// xpath finished with ]
					xpath = xpath.substring(0, xpath.length()-2);  // removing ] form the xpath
					newLocator += xpath.replace("]", "");
					if (atts.size()>0){ // avoiding null
						for (int j=0; j < atts.size()-1; j++){
							newLocator += " @" + atts.get(j).replace("\"", "\\\"") + " and ";
						}
						newLocator += atts.get(atts.size()-1).replace("\"", "\\\"");
					}
					newLocator += "]"; // adding back the removed ]
				}else{  // xpath finished withouth ]
					newLocator += xpath.replace("\")", "") + "[";
					if (atts.size()>0){ // avoiding null
						for (int j=0; j < atts.size()-1; j++){
							newLocator += "@" + atts.get(j).replace("\"", "\\\"") + " and @";
						}
						newLocator += atts.get(atts.size()-1).replace("\"", "\\\"");
					}
					newLocator += "]";

				}

				AssertedElementRegion aep = new AssertedElementRegion(element, "", "By.xpath(\"" + xpath + "\")"); // creating an AssertedElementRegion without any assertion text
				// generate element/region assertions in the form of strings for each DOM element
				String elementTagAttAssertion =  generateElementAssertion(aep, "ElementTagAttMatch").getAssertion();
				String regionTagAssertion =  generateRegionAssertion(aep, "RegionTagMatch").getAssertion();
				String regionTagAttAssertion =  generateRegionAssertion(aep, "RegionTagAttMatch").getAssertion();
				String regionFullAssertion =  generateRegionAssertion(aep, "RegionFullMatch").getAssertion();

				if (elementTagAttAssertion.length()<4000)
					tempListOfelementTagAttAssertions.add(elementTagAttAssertion);
				if (regionTagAssertion.length()<4000)
					tempListOfregionTagAssertions.add(regionTagAssertion); 
				if (regionTagAttAssertion.length()<4000)
					tempListOfregionTagAttAssertions.add(regionTagAttAssertion); 
				if (regionFullAssertion.length()<4000)
					tempListOfregionFullAssertions.add(regionFullAssertion);				
			}



			/*Object xpathObjs = this.browser.executeJavaScript(jscript);
			ArrayList xPaths = (ArrayList) xpathObjs;
			String xpath = "";
			for (int i=0;i<xPaths.size();i++){
				xpath  = (xPaths.get(i).toString());
				Element element = getElementFromXpath(xpath, browser);
				if (element.getTagName().toUpperCase().equals("HTML") || 
						element.getTagName().toUpperCase().equals("HEAD") ||
						element.getTagName().toUpperCase().equals("META") ||
						element.getTagName().toUpperCase().equals("BODY"))
					continue;
				//System.out.println("The DOM element is: " + element);
				tempListOfDOMElement.add(element);
			}
			 */

		} catch (XPathExpressionException xe) {
			LOG.info("XPathExpressionException!");
			xe.printStackTrace();
		} catch(Exception e){
			LOG.info("Could not execute script");
			e.printStackTrace();
		}


		// now add the tempListOfDOMElement to the current DOM state in the SFG



		/*
		 * This was previously added for FeedEx part but replaced with the new exploration idea
		 */
		// Calculating JS statement code coverage for feedback-directed exploration
		/*for (String modifiedJS : JSModifyProxyPlugin.getModifiedJSList()){
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
		 */
	}

	@Override
	public void onCloneState(CrawlerContext context, StateVertex cloneState) {
		// Adding DOM elements observed in the browser to the clone state
		for (String assertion: tempListOfelementTagAttAssertions)
			cloneState.addElementTagAttAssertion(assertion);
		for (String assertion: tempListOfregionTagAssertions)
			cloneState.addRegionTagAssertion(assertion);
		for (String assertion: tempListOfregionTagAttAssertions)
			cloneState.addRegionTagAttAssertion(assertion);
		for (String assertion: tempListOfregionFullAssertions)
			cloneState.addRegionFullAssertion(assertion);

		// Emptying the temp lists
		tempListOfelementTagAttAssertions.clear();
		tempListOfregionTagAssertions.clear();
		tempListOfregionTagAttAssertions.clear();
		tempListOfregionFullAssertions.clear();	
	}

	@Override
	public void onNewState(CrawlerContext context, StateVertex newState) {

		//if (loadInitialSFGFromFile == false && loadExtendedSFGFromFile == false)
		//	return;

		if (newState.getId()==0){

			//if (true) return;

			System.out.println("Adding index page features and random ");

			browser = context.getBrowser();
			try {
				Object webelementObjs = context.getBrowser().executeJavaScript("return document.getElementsByTagName(\"*\");");
				ArrayList webelements = (ArrayList) webelementObjs;
				WebElement webelement = null;
				for (int i=0;i<webelements.size();i++){
					webelement  = (WebElement) webelements.get(i);
					String xpath = getXPath(webelement);
					Element element = getElementFromXpath(xpath, context.getBrowser());
					if (element.getTagName().toUpperCase().equals("HTML") || 
							element.getTagName().toUpperCase().equals("HEAD") ||
							element.getTagName().toUpperCase().equals("META") ||
							element.getTagName().toUpperCase().equals("BODY"))
						continue;
					//System.out.println("The DOM element is: " + element);	


					NamedNodeMap elementAttList = element.getAttributes();
					ArrayList<String> atts = new ArrayList<String>();
					for (int j = 0; j < elementAttList.getLength(); j++)
						atts.add(elementAttList.item(j).getNodeName() + "=\"" + elementAttList.item(j).getNodeValue() + "\"");

					String newLocator ="";

					// To combine the xpath locators for multiple attributes:
					// => //E[@foo="bar" and @foo1="bar1"]

					if (xpath.endsWith("]")){	// xpath finished with ]
						xpath = xpath.substring(0, xpath.length()-2);  // removing ] form the xpath
						newLocator += xpath.replace("]", "");
						if (atts.size()>0){ // avoiding null
							for (int j=0; j < atts.size()-1; j++){
								newLocator += " @" + atts.get(j).replace("\"", "\\\"") + " and ";
							}
							newLocator += atts.get(atts.size()-1).replace("\"", "\\\"");
						}
						newLocator += "]"; // adding back the removed ]
					}else{  // xpath finished withouth ]
						newLocator += xpath.replace("\")", "") + "[";
						if (atts.size()>0){ // avoiding null
							for (int j=0; j < atts.size()-1; j++){
								newLocator += "@" + atts.get(j).replace("\"", "\\\"") + " and @";
							}
							newLocator += atts.get(atts.size()-1).replace("\"", "\\\"");
						}
						newLocator += "]";

					}


					AssertedElementRegion aep = new AssertedElementRegion(element, "", "By.xpath(\"" + xpath + "\")"); // creating an AssertedElementRegion without any assertion text
					// generate element/region assertions in the form of strings for each DOM element
					String elementTagAttAssertion =  generateElementAssertion(aep, "ElementTagAttMatch").getAssertion();
					String regionTagAssertion =  generateRegionAssertion(aep, "RegionTagMatch").getAssertion();
					String regionTagAttAssertion =  generateRegionAssertion(aep, "RegionTagAttMatch").getAssertion();
					String regionFullAssertion =  generateRegionAssertion(aep, "RegionFullMatch").getAssertion();

					if (elementTagAttAssertion.length()<4000)
						tempListOfelementTagAttAssertions.add(elementTagAttAssertion);
					if (regionTagAssertion.length()<4000)
						tempListOfregionTagAssertions.add(regionTagAssertion); 
					if (regionTagAttAssertion.length()<4000)
						tempListOfregionTagAttAssertions.add(regionTagAttAssertion); 
					if (regionFullAssertion.length()<4000)
						tempListOfregionFullAssertions.add(regionFullAssertion);	
				}
				
				
				// Adding DOM elements observed in the browser to the new state
				for (String assertion: tempListOfelementTagAttAssertions)
					newState.addElementTagAttAssertion(assertion);
				for (String assertion: tempListOfregionTagAssertions)
					newState.addRegionTagAssertion(assertion);
				for (String assertion: tempListOfregionTagAttAssertions)
					newState.addRegionTagAttAssertion(assertion);
				for (String assertion: tempListOfregionFullAssertions)
					newState.addRegionFullAssertion(assertion);

				// Emptying the temp lists
				tempListOfelementTagAttAssertions.clear();
				tempListOfregionTagAssertions.clear();
				tempListOfregionTagAttAssertions.clear();
				tempListOfregionFullAssertions.clear();	
			
				
				
			} catch (XPathExpressionException xe) {
				LOG.info("XPathExpressionException!");
				xe.printStackTrace();
			} catch(Exception e){
				LOG.info("Could not execute script");
				e.printStackTrace();
			}


			// Getting feature vector of block DOM elements [label=-1 (if manualTestPath is not creatred) and 0 otherwise], to be predicted by the trained SVM.
			indexPageElementsFeatures = getDOMElementsFeatures();

			return;
		}


		if (browser.getCurrentUrl().equals("http://localhost:8888/claroline-1.11.7/index.php?logout=true"))
			return;

		// Adding DOM elements observed in the browser to the new state
		for (String assertion: tempListOfelementTagAttAssertions)
			newState.addElementTagAttAssertion(assertion);
		for (String assertion: tempListOfregionTagAssertions)
			newState.addRegionTagAssertion(assertion);
		for (String assertion: tempListOfregionTagAttAssertions)
			newState.addRegionTagAttAssertion(assertion);
		for (String assertion: tempListOfregionFullAssertions)
			newState.addRegionFullAssertion(assertion);

		// Emptying the temp lists
		tempListOfelementTagAttAssertions.clear();
		tempListOfregionTagAssertions.clear();
		tempListOfregionTagAttAssertions.clear();
		tempListOfregionFullAssertions.clear();	



		// Getting feature vector of block DOM elements [label=-1 (if manualTestPath is not creatred) and 0 otherwise], to be predicted by the trained SVM.
		ArrayList<ElementFeatures> newElementsFeatures = getDOMElementsFeatures();
		// Adding feature vectors to the state
		for (ElementFeatures ef: newElementsFeatures){
			newState.addElementFeatures(ef);

			// Adding feature vector of block DOM elements with label -1 (from manual test states) to be used for training the SVM.
			boolean elementExist = false;
			if (manualTestPathsCreated==false){
				for (ElementFeatures currentEF : trainingNegetiveSetElementFeatures){
					if (currentEF.equals(ef)){
						currentEF.increaseCount();
						elementExist = true;
						break;
					}
					//if (currentEF.cosineSimilarity(ef) > 0.5){
					//	elementExist = true;
					//	break;
					//}					
				}
				if (elementExist == false)
					trainingNegetiveSetElementFeatures.add(ef);
			}
		}



		/*
		 * This was previously added for FeedEx part but replaced with the new exploration idea
		 */
		/*
		// bypass or select random if error occured or coverage impact is negative

		// Calculate initial code coverage for the index page to be used by Feedex
		if (vertex.getId() == vertex.INDEX_ID){
			for (String modifiedJS : JSModifyProxyPlugin.getModifiedJSList()){
				// LOGGER.info("** MODIFIEDS ARE: " + modifiedJS);
				try{
					Object counter =  this.browser.executeJavaScript("return " + modifiedJS + "_exec_counter;");
					setCountList(modifiedJS, counter);
				}catch (Exception e) {
					LOG.info("Could not execute script: return " + modifiedJS + "_exec_counter;");
				}
			}
			double coverage = getCoverage();
			context.getSession().getStateFlowGraph().setInitialCoverage(vertex, coverage);
		}
		 */

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


	@Override
	public boolean isDomChanged(CrawlerContext context, StateVertex stateBefore, Eventable e, StateVertex stateAfter) {

		// The idea of using freshness was not very effective thus removed from features and addElementFeatures for state moved to onNewState


		// Detecting which elements are fresh (are in the current DOM state but not in the previous DOM state) 
		// extract all block elements, generate features vectors, determine new ones, add those with freshnes = 1 to set of freshElement

		/*
		HashSet<ElementFeatures> oldElementsFeatures = stateBefore.getElementFeatures();
		// Getting feature vector of block DOM elements [label=-1 (if manualTestPath is not creatred) and 0 otherwise], to be predicted by the trained SVM.
		ArrayList<ElementFeatures> newElementsFeatures = getDOMElementsFeatures();
		// Adding feature vectors to the state
		for (ElementFeatures ef: newElementsFeatures){
			if (!oldElementsFeatures.contains(ef)) // the classLable and freshness does not play role in comparison
				ef.setFreshness(1);  // Setting freshness to 1 if element is a new element on page. Default is 0.
			stateAfter.addElementFeatures(ef);

			// Adding feature vector of block DOM elements with label -1 (from manual test states) to be used for training the SVM.
			boolean elementExist = false;
			if (manualTestPathsCreated==false){
				for (ElementFeatures currentEF : trainingSetElementFeatures){
					if (currentEF.equals(ef)){
						currentEF.increaseCount();
						if (ef.getFreshness()==0)  // Change fresh to non-fresh
							currentEF.setFreshness(0);
						elementExist = true;
						break;
					}
					if (currentEF.cosineSimilarity(ef) > 0.9){
						elementExist = true;
						break;
					}					
				}
				if (elementExist == false)
					trainingSetElementFeatures.add(ef);
			}
		}*/

		// default DOM comparison behavior
		return !stateAfter.equals(stateBefore);
	}	

	class ElementFeatureComp implements Comparator<ElementFeatures>{
		@Override
		public int compare(ElementFeatures ef1, ElementFeatures ef2) {
			return (ef2.getCount() - ef1.getCount());
		}
	}


	/*
	 * The helpers method for the generated JUnit files for calculating JS code coverage for the experiments in the paper
	 */
	public static boolean getCoverageReport() {
		return getCoverageReport;
	}

	// To be used for DOM mutation testing for the experiments in the paper
	public static String mutateDOMTreeCode(int stateID) {
		if (mutateDOM == false)
			return null;

		if (stateID!=StateToBeMutated)
			return null;

		if (MutationOperatorCode==-1)  // the -1 operator code performs no javascript mutation but running the mutant application
			return null;

		/* DOM-based mutation operator
			0) remove subtree
			1) move subtree
			2) remove subtree att
			3) remove subtree text -> remove innerHTML
		 */

		// generate code for DOM mutation, first choose a random DOM element

		String jsCode = "function applyMutation(){ ";

		//jsCode += "randomElementID = Math.round(Math.random() * document.getElementsByTagName(\'*\').length); ";
		//jsCode += "randomElementID = 281; ";

		// if this operator is done for the first time on this state
		if (SelectedRandomElementInDOM[MutationOperatorCode][StateToBeMutated]==-1)
			jsCode += "randomElementID = Math.round(Math.random() * document.getElementsByTagName(\'*\').length); ";
		else
			jsCode += "randomElementID = " + SelectedRandomElementInDOM[MutationOperatorCode][StateToBeMutated] + "; ";


		jsCode += "randomElement = document.getElementsByTagName(\'*\')[randomElementID]; ";

		// if this operator is done for the first time on this state
		//if (SelectedRandomElementInDOM[MutationOperatorCode][StateToBeMutated]==-1){
		//jsCode += "randomElementID = Math.round(Math.random() * document.getElementsByTagName(\'*\').length); ";
		jsCode += "randomElement = document.getElementsByTagName(\'*\')[randomElementID]; ";
		/*}
		else{
			jsCode += "randomElement = " + SelectedRandomElementInDOM[MutationOperatorCode][StateToBeMutated] + "; ";
			jsCode += "randomElement = document.getElementsByTagName(\'*\')[" + SelectedRandomElementInDOM[MutationOperatorCode][StateToBeMutated] + "]; ";
		}*/

		switch(MutationOperatorCode){
		case 0:
			// remove parent and all its children
			jsCode += "randomElement.parentNode.removeChild(randomElement);";
			break;
		case 1:
			/* OLD VESION
				// move subtree of a DOM node to a random node
				jsCode += "anotherRandomElement = document.getElementsByTagName(\'*\')[Math.round(Math.random() * document.getElementsByTagName(\'*\').length)];";
				jsCode += "anotherRandomElement.appendChild(randomElement);";
			 */
			/* NEW VESION: seems to have bug! */
			// move subtree of a DOM node to a its grandparent node
			jsCode += "randomElement.parentNode.parentNode.appendChild(randomElement);";
			/* NEW VESION */
			// move subtree of a DOM node to a its neighbor node
			//jsCode += "document.getElementsByTagName(\'*\')[10].appendChild(randomElement);";


			break;
		case 2:
			// remove attributes of of subtree of a DOM element
			jsCode += "if ( randomElement.hasChildNodes() ) {  var children = randomElement.childNodes;  for (var i = 0; i < children.length; i++) {  if (children[i].hasAttributes()) { for (var j= children[i].attributes.length; j-->0;){ children[i].removeAttributeNode(children[i].attributes[j]); } } } }";
			jsCode += "if ( randomElement.hasAttributes() ) { for (var i= randomElement.attributes.length; i-->0;){ randomElement.removeAttributeNode(randomElement.attributes[i]); }}";
			break;
		case 3:
			// remove text of subtree of a DOM element
			jsCode += "if ( randomElement.hasChildNodes() ) {  var children = randomElement.childNodes;  for (var i = 0; i < children.length; i++) {  children[i].innerHTML = \'\'; } }";
			break;
		}

		//System.out.println("MutationOperatorCode " + MutationOperatorCode + " applied!");

		jsCode += " return randomElementID;} return applyMutation();";

		//System.out.println("jsCode: " + jsCode);

		return jsCode;
	}


	public void writeMPStateIDToFile(String string) {
		try {
			FileWriter fw = new FileWriter("MPStateIDs.txt", true);
			fw.write(string + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException: " + e.getMessage());
		}
	}


	public void writeEPStateIDToFile(String string) {
		try {
			FileWriter fw = new FileWriter("EPStateIDs.txt", true);
			fw.write(string + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException: " + e.getMessage());
		}
	}


	public void writeStatesForTestCasesToFile(String string) {
		try {
			FileWriter fw = new FileWriter("StatesForTestCases.txt", true);
			fw.write(string + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException: " + e.getMessage());
		}
	}

	public static void setSelectedRandomElements(
			int[][] selectedRandomElementInDOM2) {
		SelectedRandomElementInDOM = selectedRandomElementInDOM2;

		for (int i=0; i<4; i++)
			for (int j=0;j<100;j++)
				System.out.println(SelectedRandomElementInDOM[i][j]);

	}



}
