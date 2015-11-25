package jp.skypencil.jenkins.regression;

import hudson.tasks.junit.CaseResult;
import com.google.common.base.Predicate;

class NewTestPredicate implements Predicate<Tuple<CaseResult, CaseResult>> {
    @Override
    public boolean apply(Tuple<CaseResult, CaseResult> input) {
        if (input.first == null || input.second == null) {
            return false;
        } else if (input.first.isFailed && input.second.isPassed()) {
            return true;
        }
        return false;
    }
}
