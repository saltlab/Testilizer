package com.crawljax.plugins.testsuiteextension.mutationtestrunner;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.crawljax.plugins.testsuiteextension.TestSuiteExtension;

public class JUnitRunner {

	public OutputStream outputStream;

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args){
		JUnitRunner runner = new JUnitRunner();
		ArrayList<String> testClasses_MP_Orig = new ArrayList<String>();
		ArrayList<String> testClasses_MP_Reused = new ArrayList<String>();
		ArrayList<String> testClasses_MP_Generated = new ArrayList<String>();
		ArrayList<String> testClasses_MP_Learned = new ArrayList<String>();
		ArrayList<String> testClasses_MP_All = new ArrayList<String>();
		ArrayList<String> testClasses_EP_Orig = new ArrayList<String>();
		ArrayList<String> testClasses_EP_Reused = new ArrayList<String>();
		ArrayList<String> testClasses_EP_Generated = new ArrayList<String>();
		ArrayList<String> testClasses_EP_Learned = new ArrayList<String>();
		ArrayList<String> testClasses_EP_All = new ArrayList<String>();

		/*
		 * Settings for mutation testing
		 */
		int numberOfStatesInInitialSFG = 1;
		int numberOfStatesInExtendedSFG = 1;
		// Test classes of the original test suite (MP_orig)
		testClasses_MP_Orig.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the MP_reused test suite
		testClasses_MP_Reused.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the MP_generated test suite
		testClasses_MP_Generated.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the MP_learned test suite
		testClasses_MP_Learned.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the MP_all test suite
		testClasses_MP_All.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	

		// Test classes of the EP_orig test suite
		testClasses_EP_Orig.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the EP_reused test suite
		testClasses_EP_Reused.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the EP_generated test suite
		testClasses_EP_Generated.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the EP_learned test suite
		testClasses_EP_Learned.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	
		// Test classes of the EP_all test suite
		testClasses_EP_All.add("com.crawljax.plugins.testsuiteextension.generated.GeneratedTestCase0");	

		boolean passed = false;
		int totalTestRuns = 0;

		int failures_MP_Orig = 0, failures_MP_Reused = 0, failures_MP_Generated = 0, failures_MP_Learned = 0, failures_MP_All = 0;
		int failures_EP_Orig = 0, failures_EP_Reused = 0, failures_EP_Generated = 0, failures_EP_Learned = 0, failures_EP_All = 0;

		int runs_MP_Orig = 0, runs_MP_Reused = 0, runs_MP_Generated = 0, runs_MP_Learned = 0, runs_MP_All = 0;
		int runs_EP_Orig = 0, runs_EP_Reused = 0, runs_EP_Generated = 0, runs_EP_Learned = 0, runs_EP_All = 0;

		FileOutputStream fout;
		try {
			fout = new FileOutputStream("testResult.txt");
			runner.setOutpuStream(fout);

			/*
			// TODO: sample 50% of states and apply mutation on them
			Random randomGenerator = new Random();
			double randVal = randomGenerator.nextDouble();
			if (randVal < 0.5)
				return null;
			 */

			TestSuiteExtension.initializeSelectedRandomElements();
			
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < numberOfStatesInInitialSFG; j++){
					TestSuiteExtension.MutationOperatorCode = i;
					TestSuiteExtension.StateToBeMutated = j;

					System.out.println("MP: i=" + i + ", j=" + j);

					runs_MP_Orig++;
					for (String tc: testClasses_MP_Orig){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_MP_Orig++;
							break;  // if test failed discard running other test classes.
						}
					}		
					runs_MP_Reused++;
					for (String tc: testClasses_MP_Reused){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_MP_Reused++;
							break;  // if test failed discard running other test classes.
						}
					}
					runs_MP_Generated++;
					for (String tc: testClasses_MP_Generated){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_MP_Generated++;
							break;  // if test failed discard running other test classes.
						}
					}
					runs_MP_Learned++;
					for (String tc: testClasses_MP_Learned){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_MP_Learned++;
							break;  // if test failed discard running other test classes.
						}
					}
					runs_MP_All++;
					for (String tc: testClasses_MP_All){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_MP_All++;
							break;  // if test failed discard running other test classes.
						}
					}
				}
			
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < numberOfStatesInExtendedSFG; j++){
					TestSuiteExtension.MutationOperatorCode = i;
					TestSuiteExtension.StateToBeMutated = j;

					System.out.println("EP: i=" + i + ", j=" + j);

					runs_EP_Orig++;
					for (String tc: testClasses_EP_Orig){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_EP_Orig++;
							break;  // if test failed discard running other test classes.
						}
					}
					runs_EP_Reused++;
					for (String tc: testClasses_EP_Reused){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_EP_Reused++;
							break;  // if test failed discard running other test classes.
						}
					}
					runs_EP_Generated++;
					for (String tc: testClasses_EP_Generated){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_EP_Generated++;
							break;  // if test failed discard running other test classes.
						}
					}
					runs_EP_Learned++;
					for (String tc: testClasses_EP_Learned){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_EP_Learned++;
							break;  // if test failed discard running other test classes.
						}
					}
					runs_EP_All++;
					for (String tc: testClasses_EP_All){
						passed = runner.runTestClass(tc);
						if (!passed){
							System.out.println("Failed at: " + tc + ", applying operator: " + i);
							failures_EP_All++;
							break;  // if test failed discard running other test classes.
						}
					}


					//runner.runClass("generated.GeneratedTestCase0");
					//runner.runClass("com.crawljax.plugins.testsuiteextension.casestudies.wolfcms.originaltests.testCreateUser");

				}

			
			totalTestRuns =  runs_MP_Orig + runs_MP_Reused + runs_MP_Generated + runs_MP_Learned + runs_MP_All 
					+ runs_EP_Orig + runs_EP_Reused + runs_EP_Generated + runs_EP_Learned + runs_EP_All;

			
			writeResultToFile("Total mutation runs: " + totalTestRuns);
			
			writeResultToFile("failures_MP_Orig: " + failures_MP_Orig + ", runs_MP_Orig: " + runs_MP_Orig + ", failure rate for MP_Orig: " + (double)failures_MP_Orig/(double)runs_MP_Orig);
			writeResultToFile("failures_MP_Reused: " + failures_MP_Reused + ", runs_MP_Reused: " + runs_MP_Reused + ", failure rate for MP_Reused: " + (double)failures_MP_Reused/(double)runs_MP_Reused);
			writeResultToFile("failures_MP_Generated: " + failures_MP_Generated + ", runs_MP_Generated: " + runs_MP_Generated + ", failure rate for MP_Generated: " + (double)failures_MP_Generated/(double)runs_MP_Generated);
			writeResultToFile("failures_MP_Learned: " + failures_MP_Learned + ", runs_MP_Learned: " + runs_MP_Learned + ", failure rate for MP_Learned: " + (double)failures_MP_Learned/(double)runs_MP_Learned);
			writeResultToFile("failures_MP_All: " + failures_MP_All + ", runs_MP_All: " + runs_MP_All + ", failure rate for MP_All: " + (double)failures_MP_All/(double)runs_MP_All);
			writeResultToFile("failures_EP_Orig: " + failures_EP_Orig + ", runs_EP_Orig: " + runs_EP_Orig + ", failure rate for EP_Orig: " + (double)failures_EP_Orig/(double)runs_EP_Orig);
			writeResultToFile("failures_EP_Reused: " + failures_EP_Reused + ", runs_EP_Reused: " + runs_EP_Reused + ", failure rate for EP_Reused: " + (double)failures_EP_Reused/(double)runs_EP_Reused);
			writeResultToFile("failures_EP_Generated: " + failures_EP_Generated + ", runs_EP_Generated: " + runs_EP_Generated + ", failure rate for EP_Generated: " + (double)failures_EP_Generated/(double)runs_EP_Generated);
			writeResultToFile("failures_EP_Learned: " + failures_EP_Learned + ", runs_EP_Learned: " + runs_EP_Learned + ", failure rate for EP_Learned: " + (double)failures_EP_Learned/(double)runs_EP_Learned);
			writeResultToFile("failures_EP_All: " + failures_EP_All + ", runs_EP_All: " + runs_EP_All + ", failure rate for EP_All: " + (double)failures_EP_All/(double)runs_EP_All);



		} catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public void setOutpuStream(OutputStream out) {
		this.outputStream = out;

	}

	// Testing mutants
	private boolean runTestClass(String testClassName) throws ClassNotFoundException {
		Class testClass = Class.forName(testClassName);
		JUnitCore core = new JUnitCore();
		PassingTestsRunListener listener = new PassingTestsRunListener();
		core.addListener(listener);
		Result result = core.run(testClass);
		return result.wasSuccessful();
	}


	public void runClass(String testClassName) throws ClassNotFoundException {
		Class testClass = Class.forName(testClassName);
		JUnitCore core = new JUnitCore();
		PassingTestsRunListener listener = new PassingTestsRunListener();
		core.addListener(listener);
		Result result = core.run(testClass);
		String resultsTemplate = "{ \"fail\": %d, \"pass\": %d, \"suites\": [ %s ] }";
		int fail = result.getFailureCount();
		int pass = result.getRunCount() - fail;
		String suitesTemplate = "{ \"name\": \"%s\", \"tests\": [ %s ] }";
		String testsTemplate = "{ \"name\": \"%s\", \"status\": \"%s\", \"info\": \"%s\"}";

		// First, print passing tests collected by the custom Listener		
		Iterator<String> itPassing =  listener.getPassingTestsNames();
		String testsJson = "";
		String sep = "";
		while (itPassing.hasNext()) {
			String testName = (String) itPassing.next();
			testsJson += sep + String.format(testsTemplate, testName, "pass", "");
			sep = ", ";
		}

		// Then, print failing tests. JUnit does not collect information for passing tests in Result object (too bad).
		List<Failure> failures = result.getFailures();
		Iterator<Failure> it = failures.iterator();
		while (it.hasNext()) {
			Failure failure = (Failure) it.next();
			testsJson += sep + String.format(testsTemplate, failure.getDescription().getMethodName(), "fail", failure.getMessage());
		}
		String suiteName = testClassName.substring(testClassName.lastIndexOf('.')+1);
		String suitesJson = String.format(suitesTemplate, suiteName, testsJson);
		String resultsJson = String.format(resultsTemplate, fail, pass, suitesJson)+ "\n";
		try {
			this.outputStream.write(resultsJson.getBytes());
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
	}

	public static void writeResultToFile(String string) {
		try {
			FileWriter fw = new FileWriter("MutationTestingResult.txt", true); //appending new data
			fw.write(string + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException: " + e.getMessage());
		}
	}

}
