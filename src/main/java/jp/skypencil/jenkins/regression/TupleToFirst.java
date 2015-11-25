package jp.skypencil.jenkins.regression;

import hudson.tasks.junit.CaseResult;
import com.google.common.base.Function;

class TupleToFirst implements Function<Tuple<CaseResult, CaseResult>, CaseResult> {
    @Override
    public CaseResult apply(Tuple<CaseResult, CaseResult> input) {
        return (CaseResult) input.first;
    }
}
