package jp.skypencil.jenkins.regression;

import hudson.tasks.junit.CaseResult;
import com.google.common.base.Function;

class PairToFirst implements Function<Pair<CaseResult, CaseResult>, CaseResult> {
    @Override
    public CaseResult apply(Pair<CaseResult, CaseResult> input) {
        return input.first;
    }
}
