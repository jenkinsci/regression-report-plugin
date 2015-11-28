package jp.skypencil.jenkins.regression;

import hudson.tasks.junit.CaseResult;
import com.google.common.base.Predicate;

class NewTestPredicate implements Predicate<Tuple<CaseResult, CaseResult>> {
    @Override
    public boolean apply(Tuple<CaseResult, CaseResult> input) {
        return input.second == null;
    }
}
