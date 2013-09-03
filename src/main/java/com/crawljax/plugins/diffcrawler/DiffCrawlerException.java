package com.crawljax.plugins.diffcrawler;

import com.crawljax.core.CrawljaxException;

/**
 * Gets thrown when something unexpected goes wrong inside the {@link DiffCrawler} plugin.
 */
@SuppressWarnings("serial")
public class DiffCrawlerException extends CrawljaxException {

	public DiffCrawlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public DiffCrawlerException(String message) {
		super(message);
	}

}
