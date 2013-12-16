package com.crawljax.plugins.testcasegenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.state.Element;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;
import com.crawljax.forms.FormInput;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XMLObject;

public class TestSuiteGeneratorHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSuiteGeneratorHelper.class);


	private final CrawlSession session;

	public TestSuiteGeneratorHelper(CrawlSession session) {
		this.session = session;
		//updateIdsForEventables();
		//updateIdsForStates();
	}

	private void updateIdsForEventables() {
		long id = 1;
		for (Eventable eventable : session.getStateFlowGraph().getAllEdges()) {
			eventable.setId(id);
			id++;
		}
		for (StateVertex StateVertex : session.getStateFlowGraph().getAllStates()) {
			StateVertex.setId(id);
			id++;
		}
	}

	private void updateIdsForStates() {
		long id = 1;
		for (StateVertex StateVertex : session.getStateFlowGraph().getAllStates()) {
			StateVertex.setId(id);
			id++;
		}
	}

	public void writeStateVertexTestDataToXML(String fname) throws IOException {
		Set<StateVertex> states = session.getStateFlowGraph().getAllStates();

		DomUtils.checkFolderForFile(fname);
		LOGGER.info("Writing StateVertices test data to " + fname);
		Map<Long, StateVertex> map = new HashMap<Long, StateVertex>();
		//int id = 1;
		for (StateVertex stateVertex : states) {
			//stateVertex.setId(id);
			// Original StateVertex saveSateVertix = stateVertex.clone();
			StateVertex saveSateVertix = stateVertex;
			map.put(saveSateVertix.getId(), saveSateVertix);
			//id++;
		}
		XMLObject.objectToXML(map, fname);
	}

	public void writeEventableTestDataToXML(String fname) throws IOException {
		Set<Eventable> eventables = session.getStateFlowGraph().getAllEdges();
		DomUtils.checkFolderForFile(fname);
		LOGGER.info("Writing Eventables test data to " + fname);
		Map<Long, Eventable> map = new HashMap<Long, Eventable>();
		//long id = 1;
		for (Eventable eventable : eventables) {
			//eventable.setId(id);
			Eventable newEventable =
			        new Eventable(eventable.getIdentification(), eventable.getEventType());
			newEventable.setId(eventable.getId());
			Element element = new Element(eventable.getElement().getNode());
			//newEventable.setId(id);
			newEventable.setElement(element);

			map.put(newEventable.getId(), newEventable);
			//id++;
		}
		XMLObject.objectToXML(map, fname);
	}

	public List<TestMethod> getTestMethods() {
		Collection<List<Eventable>> crawlPaths = session.getCrawlPaths();
		List<TestMethod> testMethods = new ArrayList<TestMethod>();

		TestMethod testMethod;
		TestMethodEvent methodEvent;
		Map<String, String> properties;

		// initial state testing
		properties = new HashMap<String, String>();
		properties.put("index", "0");
		properties.put("targetid", "1");
		properties.put("how", "index");
		methodEvent = new TestMethodEvent();
		methodEvent.setProperties(properties);

		testMethod = new TestMethod();
		testMethod.setMethodName("0");
		testMethod.addMethodEvent(methodEvent);
		testMethods.add(testMethod);

		// add the paths
		for (List<Eventable> crawlPath : crawlPaths) {
			testMethod = new TestMethod();

			String methodId = "";

			for (Eventable clickable : crawlPath) {

				// set properties of methodEvent
				properties = new HashMap<String, String>();
				properties.put("id", "" + clickable.getId());
				properties.put("info", clickable.toString());
				properties.put("how", clickable.getIdentification().getHow().toString());
				properties.put("text", clickable.getElement().getText()
				        .replaceAll("\"", "\\\\\"").trim());
				try {
					properties.put("targetid", "" + clickable.getTargetStateVertex().getId());
				} catch (CrawljaxException e) {
					LOGGER.error("Catched CrawljaxException while getting TragerStateVertex", e);
				}
				String id = "" + clickable.getId();
				properties.put("index", id);
				methodEvent = new TestMethodEvent();
				methodEvent.setProperties(properties);

				// set formInputs
				List<Map<String, String>> mapFormInputs = new ArrayList<Map<String, String>>();
				for (FormInput formInput : clickable.getRelatedFormInputs()) {
					if (formInput.getInputValues().iterator().hasNext()) {
						properties = new HashMap<String, String>();
						// TODO Changed to make compile. Is this correct?
						properties.put("how", formInput.getIdentification().getHow().toString());

						properties.put("name", formInput.getIdentification().getValue());
						properties.put("type", formInput.getType());
						properties.put("value", formInput.getInputValues().iterator().next().getValue());
						
						if (!mapFormInputs.contains(properties))
							mapFormInputs.add(properties);
					}
				}
				methodEvent.setFormInputs(mapFormInputs);

				// append to method name
				if (!methodId.equals("")) {
					methodId += "_";
				}
				methodId += id;

				// add event
				testMethod.addMethodEvent(methodEvent);
			}

			// set the name and add the test method
			testMethod.setMethodName(methodId);
			testMethods.add(testMethod);

		}

		return testMethods;
	}
}
