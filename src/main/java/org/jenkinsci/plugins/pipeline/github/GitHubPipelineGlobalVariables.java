package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import jenkins.scm.api.SCMHead;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.cps.GlobalVariableSet;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Factory for our {@link PullRequestGlobalVariable} instance.
 *
 * @author Aaron Whiteside
 * @see PullRequestGlobalVariable
 */
@Extension
public class GitHubPipelineGlobalVariables extends GlobalVariableSet {
    @NonNull
    @Override
    public Collection<GlobalVariable> forRun(final Run<?, ?> run) {
        if (run == null) {
            return Collections.emptyList();
        }
        SCMHead scmHead = SCMHead.HeadByItem.findHead(run.getParent());
        if (scmHead instanceof PullRequestSCMHead) {
            Collection<GlobalVariable> result = new LinkedList<>();
            result.add(new PullRequestGlobalVariable());
            return result;
        }
        return Collections.emptyList();
    }
}
