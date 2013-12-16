package com.crawljax.plugins.testcasegenerator;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.browserwaiter.WaitConditionChecker;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.Identification.How;
import com.crawljax.core.state.StateVertex;
import com.crawljax.forms.FormHandler;
import com.crawljax.forms.FormInput;
import com.crawljax.oraclecomparator.StateComparator;
import com.crawljax.util.ElementResolver;
import com.crawljax.util.XMLObject;

/**
 * Helper for the test suites.
 */
public class TestSuiteHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSuiteHelper.class);

	private final ArrayList<Eventable> eventables = new ArrayList<Eventable>();

	private Map<Long, StateVertex> mapStateVertices;
	private Map<Long, Eventable> mapEventables;

	// private ReportBuilder reportBuilder = new ReportBuilder("TestReport");

	/**
	 * @param xmlStates
	 *            The xml states.
	 * @param xmlEventables
	 *            The xml eventables.
	 * @throws Exception
	 *             On error.
	 */
	@SuppressWarnings("unchecked")
	public TestSuiteHelper(String xmlStates, String xmlEventables) throws Exception {
		LOGGER.info("Loading needed xml files for States and Eventables");
		mapStateVertices = (Map<Long, StateVertex>) XMLObject.xmlToObject(xmlStates);
		mapEventables = (Map<Long, Eventable>) XMLObject.xmlToObject(xmlEventables);
	}

	private Eventable getEventable(Long eventableId) {
		return mapEventables.get(eventableId);
	}

	/**
	 * @param StateVertexId
	 *            The id of the state vertex.
	 * @return the State with id StateVertex Id
	 */
	public StateVertex getStateVertex(Long StateVertexId) {
		return mapStateVertices.get(StateVertexId);
	}

	/**
	 * @param currentTestMethod
	 *            The current method that is used for testing
	 */
	public void newCurrentTestMethod(String currentTestMethod) {
		LOGGER.info("");
		LOGGER.info("New test: " + currentTestMethod);
		eventables.clear();
		// this.currentTestMethod = currentTestMethod;
	}

}
