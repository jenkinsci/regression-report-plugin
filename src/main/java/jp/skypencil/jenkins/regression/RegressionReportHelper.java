package jp.skypencil.jenkins.regression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;

import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.junit.TestResultAction;

public class RegressionReportHelper {

    /** 
     * Returns a list of CaseResults that are contained in a build. Currently this
     * function only handles builds whose getResult return 
     * an object of type TestResultAction or AggregatedTestResultAction
     * @param build an AbstractBuild object from which the caller wants to get
     *      the case results.
     * @return an ArrayList of CaseResult.
     */
    public static ArrayList<CaseResult> getAllCaseResultsForBuild(AbstractBuild build) {
        ArrayList<CaseResult> ret = new ArrayList<CaseResult>();
        List<AbstractTestResultAction> testActions = build.getActions(AbstractTestResultAction.class);

        for (AbstractTestResultAction testAction : testActions) {
            if (testAction instanceof TestResultAction) {
                hudson.tasks.junit.TestResult testResult = (hudson.tasks.junit.TestResult) testAction.getResult();
                ret.addAll(getTestsFromTestResult(testResult));
            }
            else if (testAction instanceof AggregatedTestResultAction){
                List<AggregatedTestResultAction.ChildReport> child_reports = ((AggregatedTestResultAction)testAction).getChildReports();
                for(AggregatedTestResultAction.ChildReport child_report: child_reports){
                    hudson.tasks.junit.TestResult testResult = (hudson.tasks.junit.TestResult) child_report.result;
                    ret.addAll(getTestsFromTestResult(testResult));
                }
            }
            //Else, unsupported project type.
        }
        return ret;
    }

    /**
     * A helper fuction that returns a of CaseResult from a TestReult
     * object.
     * @param testResult a TestResult object that contains PackageResult as its
     *      children
     * @return An ArrayList of CaseResult.
     */
    private static ArrayList<CaseResult> getTestsFromTestResult(TestResult testResult) {
        ArrayList<CaseResult> tests = new ArrayList<CaseResult>();
        Collection<PackageResult> packageResults = testResult.getChildren();
        for (PackageResult packageResult : packageResults) {
            Collection<ClassResult> classResults = packageResult.getChildren();
            for(ClassResult classResult : classResults){
                Collection<CaseResult> caseResults = classResult.getChildren();
                tests.addAll(caseResults);
            }
        }

        return tests;
    }


    /**
     * Given two builds thisBuild and otherBuild, returns the a list of Tuples
     * of matching CaseResult. Each pair is of form 
     * (CaseResultFromThisBuild, CaseResultFromThatBuild)
     *
     * @param thisBuild an AbstractBuild.
     * @param otherBuild another AbstractBuild, which is compared against thisBuild
     * @return an ArrayList of Tuples of CaseResults.Each pair is of form 
     * (CaseResultFromThisBuild, CaseResultFromThatBuild)
     * if a matching case result is not found in the other build, a null is used
     * instead.
     */
    public static ArrayList<Tuple<CaseResult, CaseResult>> matchTestsBetweenBuilds(AbstractBuild thisBuild, AbstractBuild otherBuild) {
        ArrayList<CaseResult> thisResults = getAllCaseResultsForBuild(thisBuild);
        ArrayList<CaseResult> otherResults = getAllCaseResultsForBuild(otherBuild);

        HashMap<String, CaseResult> hmap = new HashMap<String, CaseResult>();
        for (CaseResult otherCaseResult : otherResults) {
            hmap.put(otherCaseResult.getFullName(), otherCaseResult); // add (test_name, CaseResult) to hmap
        }

        ArrayList<Tuple<CaseResult, CaseResult>> returnValue = new ArrayList<Tuple<CaseResult, CaseResult>>();
        for (CaseResult thisCaseResult : thisResults) {
            String currTestName = thisCaseResult.getFullName();
            CaseResult otherCaseResult = null;
            if (hmap.containsKey(currTestName)) {
                otherCaseResult = hmap.get(currTestName);
                hmap.remove(currTestName);
            }
            Tuple tuple = new Tuple<CaseResult, CaseResult>(thisCaseResult, otherCaseResult);
            returnValue.add(tuple);
        }

        for (CaseResult otherCaseResultInMap : hmap.values()) {
            Tuple tuple = new Tuple<CaseResult, CaseResult>(null, otherCaseResultInMap);
            returnValue.add(tuple);
        }

        return returnValue;
    }
    
}
