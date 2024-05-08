package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

/**
 * Factory for our {@link PullRequestGroovyObject} instance.
 *
 * @author Aaron Whiteside
 * @see PullRequestGroovyObject
 */
public class PullRequestGlobalVariable extends GlobalVariable {

    @NonNull
    @Override
    public String getName() {
        return "pullRequest";
    }

    @NonNull
    @Override
    public Object getValue(@NonNull final CpsScript script) throws Exception {
        final Run<?, ?> build = script.$build();
        if (build == null) {
            throw new IllegalStateException("No associated build");
        }
        return new PullRequestGroovyObject(build.getParent());
    }

}
