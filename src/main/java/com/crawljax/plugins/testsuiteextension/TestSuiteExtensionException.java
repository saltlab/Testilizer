package com.crawljax.plugins.testsuiteextension;

import com.crawljax.core.CrawljaxException;

/**
 * Gets thrown when something unexpected goes wrong inside the {@link TestSuiteExtension} plugin.
 */
@SuppressWarnings("serial")
public class TestSuiteExtensionException extends CrawljaxException {

	public TestSuiteExtensionException(String message, Throwable cause) {
		super(message, cause);
	}

	public TestSuiteExtensionException(String message) {
		super(message);
	}

}
