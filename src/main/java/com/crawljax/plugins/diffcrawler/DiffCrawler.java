package com.crawljax.plugins.diffcrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.SerializationUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.OnFireEventFailedPlugin;
import com.crawljax.core.plugin.OnFireEventSucceededPlugin;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnUrlLoadPlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.plugin.PreCrawlingPlugin;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.jsmodify.AstInstrumenter;
import com.crawljax.plugins.jsmodify.JSModifyProxyPlugin;
import com.crawljax.plugins.utils.EventFunctionRelation;
import com.crawljax.plugins.utils.JSFunctionInfo;
import com.crawljax.plugins.diffcrawler.model.CandidateElementPosition;
import com.crawljax.plugins.diffcrawler.model.OutPutModel;
import com.crawljax.plugins.diffcrawler.model.Serializer;
import com.crawljax.plugins.diffcrawler.model.State;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * The DiffCrawler plug-in is built on top of the crawloverview plug-in that generates a HTML report from the crawling session
 * including screenshots of the visited states, the clicked elements, and the state-flow graph.
 **/
public class DiffCrawler implements OnNewStatePlugin, PreStateCrawlingPlugin,
PostCrawlingPlugin, OnFireEventFailedPlugin, OnUrlLoadPlugin, OnFireEventSucceededPlugin, PreCrawlingPlugin{

	private static final Logger LOG = LoggerFactory
			.getLogger(DiffCrawler.class);

	private final OutputBuilder outputBuilder;
	private final ConcurrentMap<String, StateVertex> visitedStates;
	private final OutPutModelCache outModelCache;
	private boolean warnedForElementsInIframe = false;
	private OutPutModel result;

	
	// The event-function relation table EFT stores which functions were executed as a result of firing an event
	private ArrayList<EventFunctionRelation> newEFT = new ArrayList<EventFunctionRelation>();

	// old version info loaded from file
	private StateFlowGraph oldSFG = null;
	private ArrayList<EventFunctionRelation> oldEFT = new ArrayList<EventFunctionRelation>();
	private ArrayList<JSFunctionInfo> oldJSFunctions = new ArrayList<JSFunctionInfo>();

	private JSCodeChangeAnalyzer analyzer = new JSCodeChangeAnalyzer();

	public DiffCrawler(File outputFolder) {
		Preconditions
		.checkNotNull(outputFolder, "Output folder cannot be null");
		outputBuilder = new OutputBuilder(outputFolder);
		outModelCache = new OutPutModelCache();
		visitedStates = Maps.newConcurrentMap();
		LOG.info("Initialized the DiffCrawler plugin");
	}

	/**
	 * Saves a screenshot of every new state.
	 */
	@Override
	public void onNewState(CrawlerContext context, StateVertex vertex) {
		LOG.debug("onNewState");
		StateBuilder state = outModelCache.addStateIfAbsent(vertex);
		visitedStates.putIfAbsent(state.getName(), vertex);
		saveScreenshot(context.getBrowser(), state.getName(), vertex);
		outputBuilder.persistDom(state.getName(), vertex.getDom());
	}

	private void saveScreenshot(EmbeddedBrowser browser, String name,
			StateVertex vertex) {
		LOG.debug("Saving screenshot for state {}", name);
		File jpg = outputBuilder.newScreenShotFile(name);
		File thumb = outputBuilder.newThumbNail(name);
		try {
			byte[] screenshot = browser.getScreenShot();
			ImageWriter.writeScreenShotAndThumbnail(screenshot, jpg, thumb);
		} catch (CrawljaxException | WebDriverException e) {
			LOG.warn(
					"Screenshots are not supported or not functioning for {}. Exception message: {}",
					browser, e.getMessage());
			LOG.debug("Screenshot not made because {}", e.getMessage(), e);
		}
		LOG.trace("Screenshot saved");
	}

	
	
	/**
	 * Initializing the DiffCrawler with the old SFG and the old EFT
	 */
	@Override
	public void preCrawling(CrawljaxConfiguration config) {
		LOG.info("DiffCrawler plugin started");
		
		// TODO: get jsFunction info from the current version using the proxy. should probably override response for proxy as well...
		
		try {

			LOG.info("Reading the old EFT and the old SFG from file");

			// Read the old EFT (event-function relation table) and the old SFG from file
			FileInputStream fis = null;
			ObjectInputStream in = null;

			// Read the old SFG from file
			String sfgFileName = "sfg.ser";
			try {
				fis = new FileInputStream(sfgFileName);
				in = new ObjectInputStream(fis);
				oldSFG = (StateFlowGraph) in.readObject();
				in.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			//LOG.info(Serializer.toPrettyJson(oldSFG));
			LOG.info("oldSFG: " + oldSFG);

			// Read the old EFT from file
			String eftFileName = "eft.ser";
			try {
				fis = new FileInputStream(eftFileName);
				in = new ObjectInputStream(fis);
				oldEFT = (ArrayList<EventFunctionRelation>) in.readObject();
				in.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			//LOG.info(Serializer.toPrettyJson(oldEFT));
			LOG.info("oldEFT: " + oldEFT);
			
			// Read the old jsFunctions from file
			String jsFuncFileName = "functions.ser";
			try {
				fis = new FileInputStream(jsFuncFileName);
				in = new ObjectInputStream(fis);
				oldJSFunctions = (ArrayList<JSFunctionInfo>) in.readObject();
				in.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			//LOG.info(Serializer.toPrettyJson(oldJSFunctions));
			LOG.info("oldJSFunctions: " + oldJSFunctions);

			
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}

	
	
	/**
	 * Logs all the candidate elements so that the plugin knows which elements were the candidate
	 * elements.
	 */
	@Override
	public void preStateCrawling(CrawlerContext context,
			ImmutableList<CandidateElement> candidateElements, StateVertex state) {
		LOG.debug("preStateCrawling");
		List<CandidateElementPosition> newElements = Lists.newLinkedList();
		LOG.info("Prestate found new state {} with {} candidates",
				state.getName(), candidateElements.size());
		for (CandidateElement element : candidateElements) {
			try {
				WebElement webElement = getWebElement(context.getBrowser(), element);
				if (webElement != null) {
					newElements.add(findElement(webElement, element));
				}
			} catch (WebDriverException e) {
				LOG.info("Could not get position for {}", element, e);
			}
		}

		StateBuilder stateOut = outModelCache.addStateIfAbsent(state);
		stateOut.addCandidates(newElements);
		LOG.trace("preState finished, elements added to state");
	}

	private WebElement getWebElement(EmbeddedBrowser browser,
			CandidateElement element) {
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

	private CandidateElementPosition findElement(WebElement webElement,
			CandidateElement element) {
		Point location = webElement.getLocation();
		Dimension size = webElement.getSize();
		CandidateElementPosition renderedCandidateElement =
				new CandidateElementPosition(element.getIdentification().getValue(),
						location, size);
		if (location.getY() < 0) {
			LOG.warn("Weird positioning {} for {}", webElement.getLocation(),
					renderedCandidateElement.getXpath());
		}
		return renderedCandidateElement;
	}

	/**
	 * Generated the report.
	 */
	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitStatus) {
		LOG.debug("postCrawling");
		StateFlowGraph sfg = session.getStateFlowGraph();
		result = outModelCache.close(session, exitStatus);
		outputBuilder.write(result, session.getConfig());
		StateWriter writer = new StateWriter(outputBuilder, sfg,
				ImmutableMap.copyOf(visitedStates));
		for (State state : result.getStates().values()) {
			writer.writeHtmlForState(state);
		}
		
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
			LOG.info("DiffCrawler successfully wrote SFG to sfg.ser file");
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
			LOG.info("DiffCrawler successfully wrote EFT to eft.ser file");
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

		// Save the AstInstrumenter.jsFunctions to file
		String jsFuncFileName = "functions.ser";
		try {
			fos = new FileOutputStream(jsFuncFileName);
			out = new ObjectOutputStream(fos);
			out.writeObject(AstInstrumenter.jsFunctions);
			out.close();
			LOG.info("DiffCrawler successfully wrote AstInstrumenter.jsFunctions to functions.ser file");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
			
		
		LOG.info("DiffCrawler plugin has finished");
	}

	/**
	 * @return the result of the Crawl or <code>null</code> if it hasn't finished yet.
	 */
	public OutPutModel getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "DiffCrawler plugin";
	}

	@Override
	public void onFireEventFailed(CrawlerContext context, Eventable eventable,
			List<Eventable> pathToFailure) {
		outModelCache.registerFailEvent(context.getCurrentState(), eventable);
	}

	@Override
	public void onUrlLoad(CrawlerContext context) {
		// TODO crawl given a crawlpath to specific states
		// TODO: check for DOM statement changes and affected functions
		// TODO: set Xpaths to states need to be recrawled
		
		ArrayList<JSFunctionInfo> affectedFunctions = analyzer.getAffectedFunctions(oldJSFunctions, AstInstrumenter.jsFunctions);
		LOG.info("DOM accessing statements are changed in these functions:" + affectedFunctions);
	}

	/**
	 * After a successful event firing, create an event-function relation and add it to the EFT table
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

		// get functions executed from instrumented code in the browser
		for (String modifiedJS : JSModifyProxyPlugin.getModifiedJSList()){
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
		}

		//LOG.info(Serializer.toPrettyJson(AstInstrumenter.jsFunctions));

		// Create an event-function relation and add it to the EFT table
		EventFunctionRelation newRelation = new EventFunctionRelation(eventable, stateBefore, stateAfter, executedJSFunctions);
		newEFT.add(newRelation);
	}


}
