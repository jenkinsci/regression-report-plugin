package jp.skypencil.jenkins.regression;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.ChangeLogSet;

public class ChangeLogSetMock extends ChangeLogSet<ChangeLogSet.Entry> {

    protected ChangeLogSetMock(AbstractBuild<?, ?> build) {
        super(build);
    }

    private final Set<Entry> set = Sets.newHashSet();

    ChangeLogSetMock withChangeBy(final User user) {
        ChangeLogSet.Entry change = new ChangeLogSet.Entry() {
            @Override
            public String getMsg() {
                return "";
            }
            @Override
            public User getAuthor() {
                return user;
            }
            @Override
            public Collection<String> getAffectedPaths() {
                return Collections.emptyList();
            }
        };
        set.add(change);
        return this;
    }

    @Override
    public Iterator<Entry> iterator() {
        return set.iterator();
    }

    @Override
    public boolean isEmptySet() {
        return set.isEmpty();
    }

}
