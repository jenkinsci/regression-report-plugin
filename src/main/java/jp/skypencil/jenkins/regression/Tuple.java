package jp.skypencil.jenkins.regression;

public class Tuple<X, Y> {
    public final X first;
    public final Y second;
    public Tuple(X x, Y y) {
        first = x;
        second = y;
    }
}
