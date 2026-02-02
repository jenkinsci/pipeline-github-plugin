package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

public class MergedPullRequestGlobalVariable extends GlobalVariable {

    private final PullRequestGroovyObject pr;

    public MergedPullRequestGlobalVariable(PullRequestGroovyObject pr) {
        this.pr = pr;
    }

    @NonNull
    @Override
    public String getName() {
        return "mergedPullRequest";
    }

    @NonNull
    @Override
    public Object getValue(@NonNull final CpsScript script) throws Exception {
        final Run<?, ?> build = script.$build();
        if (build == null) {
            throw new IllegalStateException("No associated build");
        }
        return pr;
    }

}
