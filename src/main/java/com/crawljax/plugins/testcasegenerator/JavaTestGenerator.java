package com.crawljax.plugins.testcasegenerator;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.crawljax.core.configuration.CrawlRules;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.Plugin;
import com.crawljax.util.DomUtils;

/**
 * @author mesbah
 * @version $Id: JavaTestGenerator.java 6234 2009-12-18 13:46:37Z mesbah $
 */
public class JavaTestGenerator {

	private final VelocityEngine engine;
	private final VelocityContext context;
	private final String className;

	/**
	 * @param className
	 * @param url
	 * @throws Exception
	 */
	public JavaTestGenerator(String className, String url, List<TestMethod> testMethods,
	        CrawljaxConfiguration configuration) throws Exception {
		CrawlRules crawlRules = configuration.getCrawlRules();

		engine = new VelocityEngine();
		/* disable logging */
		engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");

		engine.init();
		context = new VelocityContext();
		this.className = className;
		context.put("date", new Date().toString());
		context.put("classname", className);
		context.put("url", url);

		context.put("waitAfterEvent", crawlRules.getWaitAfterEvent());
		context.put("waitAfterReloadUrl", crawlRules.getWaitAfterReloadUrl());

		context.put("methodList", testMethods);
		context.put("database", true);
	}

	public void useXmlInSteadOfDB(String xmlStates, String xmlEventables) {
		context.put("xmlstates", xmlStates);
		context.put("xmleventables", xmlEventables);
		context.put("database", false);
	}

	/**
	 * @param outputFolder
	 * @param fileNameTemplate
	 * @return filename of generates class
	 * @throws Exception
	 */
	public String generate(String outputFolder, String fileNameTemplate) throws Exception {

		Template template = engine.getTemplate(fileNameTemplate);
		DomUtils.directoryCheck(outputFolder);
		File f = new File(outputFolder + className + ".java");
		FileWriter writer = new FileWriter(f);
		template.merge(context, writer);
		writer.flush();
		writer.close();
		return f.getAbsolutePath();

	}
}
