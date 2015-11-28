package jp.skypencil.jenkins.regression;

import hudson.tasks.junit.CaseResult;
import com.google.common.base.Predicate;

class ProgressionPredicate implements Predicate<Tuple<CaseResult, CaseResult>> {
    @Override
    public boolean apply(Tuple<CaseResult, CaseResult> input) {
        return (!input.first == null && !input.second == null &&
            input.first.isFailed() && input.second.isPassed());
    }
}
