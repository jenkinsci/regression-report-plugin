package jp.skypencil.jenkins.regression;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.TestResult;

import com.google.common.base.Predicate;

class PassedPredicate implements Predicate<CaseResult> {
    @Override
    public boolean apply(CaseResult input) {
        return input.isPassed();
    }
}
