package jp.skypencil.jenkins.regression;

import hudson.tasks.junit.CaseResult;
import com.google.common.base.Predicate;

class NewTestPredicate implements Predicate<Pair<CaseResult, CaseResult>> {
    @Override
    public boolean apply(Pair<CaseResult, CaseResult> input) {
        return input.second == null;
    }
}
