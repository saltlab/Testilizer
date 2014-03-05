package com.crawljax.plugins.testsuiteextension.mutationtestrunner;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class PassingTestsRunListener extends RunListener {
	
	private Set<String> passingTestNames = new TreeSet<String>(new Comparator<String>() {

		public int compare(String o1, String o2) {
			if (o1 != null && o2 != null)
				return o1.compareTo(o2);
			else if (o2 == null)
				return o1.compareTo(o1);
			else if (o1 == null)
				return -1 * o2.compareTo(o1);

			return 0;
		}
	});
	
	private Set<String> failingTestNames = new TreeSet<String>(new Comparator<String>() {

		public int compare(String o1, String o2) {
			if (o1 != null && o2 != null)
				return o1.compareTo(o2);
			else if (o2 == null)
				return o1.compareTo(o1);
			else if (o1 == null)
				return -1 * o2.compareTo(o1);

			return 0;
		}
	});

	public Iterator<String> getPassingTestsNames() {
		return this.passingTestNames.iterator();
	}
	
	@Override
	public void testFinished(Description description) throws Exception {
		String methodName = description.getMethodName();
		if (!this.failingTestNames.contains(methodName))
			this.passingTestNames.add(methodName);
		super.testFinished(description);
	}
	
	@Override
	public void testFailure(Failure failure) throws Exception {
		String methodName = failure.getDescription().getMethodName();
		this.failingTestNames.add(methodName);
		super.testFailure(failure);
	}

}
