package org.jenkinsci.plugins.pipeline.github;

import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;

/**
 * Factory for our {@link PullRequestGroovyObject} instance.
 *
 * @author Aaron Whiteside
 * @see PullRequestGroovyObject
 */
public class PullRequestGlobalVariable extends GlobalVariable {

    @Nonnull
    @Override
    public String getName() {
        return "pullRequest";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull final CpsScript script) throws Exception {
        final Run<?, ?> build = script.$build();
        if (build == null) {
            throw new IllegalStateException("No associated build");
        }
        return new PullRequestGroovyObject(build.getParent());
    }

}
