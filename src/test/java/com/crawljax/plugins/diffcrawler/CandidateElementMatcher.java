package com.crawljax.plugins.diffcrawler;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Factory;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import com.crawljax.plugins.diffcrawler.model.CandidateElementPosition;

class CandidateElementMatcher extends CustomMatcher<CandidateElementPosition> {

	private CandidateElementPosition actual;

	public CandidateElementMatcher(CandidateElementPosition actual) {
		super("A " + CandidateElementPosition.class.getName() + " with coordinates");
		this.actual = actual;
	}

	@Override
	public boolean matches(Object item) {
		if (item instanceof CandidateElementPosition) {
			CandidateElementPosition element = (CandidateElementPosition) item;
			return element.getLeft() == actual.getLeft() && element.getTop() == actual.getTop()
			        && element.getWidth() == actual.getWidth()
			        && element.getHeight() == actual.getHeight();
		} else {
			return false;
		}
	}

	@Factory
	public static CandidateElementMatcher element(Point point, Dimension size) {
		return new CandidateElementMatcher(new CandidateElementPosition(null, point, size));
	}

}