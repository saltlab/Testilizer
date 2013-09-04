package com.crawljax.plugins.instrumentor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import salt.domcoverage.core.utils.ConstantVars;

public class ElementDataPersist {

	public ElementData createElementData(String time, String testName, String by, String domData, ArrayList<String> elements) {

		String buffer = "";
		String domfilename = testName + "_DOM_" + time;
		String elementFile = testName + "_ELEMENT_" + time;

		return new ElementData(testName, time, by, domfilename, elementFile);
	}

	public ElementDataPersist(String time, String testName, String by, String domData, String domfilename, ArrayList<String> elements) {

		try {
			// FileUtils.cleanDirectory(new File("Coverage"));
			String buffer = "";
			String elementFile = testName + "_ELEMENT_" + time;
			if (domfilename == "") {
				domfilename = testName + "_DOM_" + time + ".html";
				writeDOMtoFile(domData, domfilename);
			}
			String allElements = "";
			for (String element : elements) {
				buffer += testName + ConstantVars.SEPARATOR + time + ConstantVars.SEPARATOR + by + ConstantVars.SEPARATOR + elementFile + ConstantVars.SEPARATOR + domfilename + "\r";
				allElements = element + "\n";
			}
			FileUtils.write(new File(ConstantVars.COVERAGE_LOCATION + elementFile + ".txt"), allElements);

			FileUtils.write(new File(ConstantVars.COVERAGE_COVERED_ELEMENTS_CSV), buffer, null, true);

			// return new ElementData(testName, time, by, domfilename,
			// elementFile);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return null;
	}

	public ElementDataPersist() {
		// TODO Auto-generated constructor stub
	}

	private void writeDOMtoFile(String domData, String domfilename) throws IOException {
		// add style to domData after TITLE
		String title = "<title>";
		String[] split = domData.split(title);
		if (split == null || split.length == 0)
			split = domData.split(title.toUpperCase());
		String modifieddomData = domData;
		if (split.length == 2)
			modifieddomData = split[0] + ConstantVars.STYLE + title + split[1];
		// if contains html does not add html extension!!!
		FileUtils.write(new File(ConstantVars.COVERAGE_LOCATION + domfilename), modifieddomData, false);
	}

	public List<ElementData> getElementsFromFile(String file) {
		List<ElementData> elementsData = new ArrayList<ElementData>();
		try {
			List<String> fileContents = FileUtils.readLines(new File(file));
			for (String line : fileContents) {
				String[] split = line.split(ConstantVars.SEPARATOR);
				String testName = split[0];
				String time = split[1];
				String by = split[2];
				String elementFile = split[3];
				String domfilename = split[4];
				// public ElementData(String time, String testName, String by,
				// String domData, ArrayList<String> elements) {
				ElementData elem = new ElementData(testName, time, by, domfilename, elementFile);
				elementsData.add(elem);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return elementsData;

	}

	public ElementData getElementData(String name, List<ElementData> elementsData) {
		// if (name.contains(".html"))
		// name = name.substring(0, name.length() - 5);
		for (ElementData elementData : elementsData) {
			if (elementData.getDomFileName().contains(name))
				return elementData;
		}
		return null;
		// TODO Auto-generated method stub

	}

}
