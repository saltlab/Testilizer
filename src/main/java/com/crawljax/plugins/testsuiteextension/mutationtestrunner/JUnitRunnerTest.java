package com.crawljax.plugins.testsuiteextension.mutationtestrunner;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.junit.Test;

public class JUnitRunnerTest {

	@Test
	public void outputsJSONResultsToStdOut() throws ClassNotFoundException {
		JUnitRunner runner = new JUnitRunner();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileOutputStream fout = new FileOutputStream("testResult.txt");
			//com.crawljax.plugins.testsuiteextension.casestudies.wolfcms.originaltests.testCreatePage
			runner.setOutpuStream(fout);
			runner.runClass("com.crawljax.plugins.testsuiteextension.casestudies.wolfcms.originaltests.testCreatePage");
			runner.runClass("com.crawljax.plugins.testsuiteextension.casestudies.wolfcms.originaltests.testCreateUser");
			String expected = "{ \"fail\": 1, \"pass\": 1, \"suites\": [ { \"name\": \"Test\", \"tests\": [ { \"name\": \"passingTest\", \"status\": \"pass\", \"info\": \"\"}, { \"name\": \"failingTest\", \"status\": \"fail\", \"info\": \"expected:<1> but was:<2>\"} ] } ] }";
			assertEquals(expected, out.toString());		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
