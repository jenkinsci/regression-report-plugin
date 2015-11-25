package jp.skypencil.jenkins.regression;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import jp.skypencil.jenkins.regression.RegressionReportHelper;

import hudson.tasks.junit.CaseResult;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

public class RegressionReportHelperTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();
	
	@SuppressWarnings("rawtypes")
	private AbstractProject project;
	
	@Before
	public void init() {
		project = j.jenkins.getItemByFullName("project1", AbstractProject.class);
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testGetAllCaseResultsForBuild1() {
		AbstractBuild build = project.getBuildByNumber(1);
        List<CaseResult> case_results = RegressionReportHelper.getAllCaseResultsForBuild(build);
        assertEquals(3, case_results.size());

        List<String> fullTestNames = new ArrayList<String>();
        for (CaseResult caseResult : case_results) {
        	fullTestNames.add(caseResult.getFullName());
        }
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp1"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp2"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp3"));
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testGetAllCaseResultsForBuild2() {
		AbstractBuild build = project.getBuildByNumber(5);
        List<CaseResult> case_results = RegressionReportHelper.getAllCaseResultsForBuild(build);
        assertEquals(4, case_results.size());
        
        List<String> fullTestNames = new ArrayList<String>();
        for (CaseResult caseResult : case_results) {
        	fullTestNames.add(caseResult.getFullName());
        }
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp1"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp2"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp3"));
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp5"));
	}
	
	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds1() {
		AbstractBuild b1 = project.getBuildByNumber(1);
		AbstractBuild b2 = project.getBuildByNumber(2);
		
		ArrayList<Tuple<CaseResult, CaseResult>> myTuples = RegressionReportHelper.matchTestsBetweenBuilds(b1, b2);
		assertEquals(3, myTuples.size());
	}
	
	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds2() {
		AbstractBuild b2 = project.getBuildByNumber(2);
		AbstractBuild b3 = project.getBuildByNumber(3);
		
		ArrayList<Tuple<CaseResult, CaseResult>> myTuples = RegressionReportHelper.matchTestsBetweenBuilds(b2, b3);
		assertEquals(4, myTuples.size());
		Tuple<CaseResult, CaseResult> lastTuple = myTuples.get(3);
		assertEquals(null, lastTuple.first);
	}
	
	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds3() {
		AbstractBuild b3 = project.getBuildByNumber(3);
		AbstractBuild b4 = project.getBuildByNumber(4);
		
		ArrayList<Tuple<CaseResult, CaseResult>> myTuples = RegressionReportHelper.matchTestsBetweenBuilds(b3, b4);
		assertEquals(5, myTuples.size());
		Tuple<CaseResult, CaseResult> lastTuple = myTuples.get(4);
		assertEquals(null, lastTuple.first);
		assertEquals(true, lastTuple.second.isFailed());
	}
	
	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds4() {
		AbstractBuild b4 = project.getBuildByNumber(4);
		AbstractBuild b5 = project.getBuildByNumber(5);
		
		ArrayList<Tuple<CaseResult, CaseResult>> myTuples = RegressionReportHelper.matchTestsBetweenBuilds(b4, b5);
		assertEquals(5, myTuples.size());
		Tuple<CaseResult, CaseResult> fourthTuple = myTuples.get(3);
		assertEquals(true, fourthTuple.first.isPassed());
		assertEquals(null, fourthTuple.second);
		
		Tuple<CaseResult, CaseResult> lastTuple = myTuples.get(4);
		assertEquals(false, lastTuple.first.isPassed());
		assertEquals(true, lastTuple.second.isPassed());
	}
}
