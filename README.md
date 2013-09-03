DiffCrawler plugin
==================

DiffCrawler is an incremental Ajax crawler built on Crawljax tool which dynamically builds a `state-flow graph' modeling
the various navigation paths and states within an Ajax application. DiffCrawler applies code change analysis to find modified parts of the javascript code and crawl towrads the modified states given the SFG from the previous run.

Using the plugin
----------------
    public class DiffCrawlerExample {

   	private static final String URL = "http://google.com";
	  
  	public static void main(String[] args) throws IOException {
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(URL);
		builder.crawlRules().insertRandomDataInInputForms(false);
		builder.setMaximumRunTime(100, TimeUnit.SECONDS);

		// click these elements
		builder.crawlRules().clickDefaultElements();
		builder.crawlRules().click("div").withAttribute("class", "clickable");

		// but don't click these
		builder.crawlRules().dontClick("a").withAttribute("class", "ignore");
		builder.crawlRules().dontClick("a").underXPath("//DIV[@id='footer']");

		// Set timeouts
		builder.crawlRules().waitAfterReloadUrl(20, TimeUnit.MILLISECONDS);
		builder.crawlRules().waitAfterEvent(200, TimeUnit.MILLISECONDS);

		// Add a condition that this XPath doesn't exits
		builder.crawlRules().addCrawlCondition("No spans with foo as class",
		        new NotXPathCondition("//*[@class='foo']"));

		// Set some input for fields
		builder.crawlRules().setInputSpec(getInputSpecification());

		// This will generate a nice output in the output directory.
		File outFolder = new File("output");
		if (outFolder.exists()) {
			FileUtils.deleteDirectory(outFolder);
		}
		builder.addPlugin(new DiffCrawler(outFolder));
		
		// Create a Proxy for the purpose of code instrumentation
		WebScarabProxyPlugin proxyPlugin = new WebScarabProxyPlugin();
		JSModifyProxyPlugin modifier = new JSModifyProxyPlugin(new AstInstrumenter());
		modifier.excludeDefaults();
		proxyPlugin.addPlugin(modifier);
		builder.addPlugin(proxyPlugin);

		// Configure the proxy to use the port 8084 (you can change this of course)
		builder.setProxyConfig(ProxyConfiguration.manualProxyOn("127.0.0.1", 8084));

		// For this version of DiffCrawler we use only one browser.
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.firefox, 1));

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();
	}
    }

