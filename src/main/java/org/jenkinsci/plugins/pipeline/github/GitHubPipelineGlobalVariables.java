package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceTrait;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.cps.GlobalVariableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Factory for our {@link PullRequestGlobalVariable} and {@link MergedPullRequestGlobalVariable} (if applicable) instances.
 *
 * @author Aaron Whiteside
 * @see PullRequestGlobalVariable
 */
@Extension
public class GitHubPipelineGlobalVariables extends GlobalVariableSet {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPipelineGlobalVariables.class);

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
            if (shouldDetectMergedPullRequest(run)) {
                result.add(new MergedPullRequestGlobalVariable());
            }
            return result;
        } else if (scmHead instanceof BranchSCMHead && shouldDetectMergedPullRequest(run)) {
            Collection<GlobalVariable> result = new LinkedList<>();
            result.add(new MergedPullRequestGlobalVariable());
            return result;
        }
        return Collections.emptyList();
    }

    private boolean shouldDetectMergedPullRequest(final Run<?, ?> run) {
        SCMSource scmSource = SCMSource.SourceByItem.findSource(run.getParent());
        if (null != scmSource && scmSource instanceof GitHubSCMSource) {
            GitHubSCMSource gitHubSource = (GitHubSCMSource) scmSource;
            List<SCMSourceTrait> traits = gitHubSource.getTraits();
            for (SCMSourceTrait trait : traits) {
                if (trait instanceof DetectMergedPullRequestTrait) {
                    LOG.debug("{} found for run: {}", trait.getClass(), run.getFullDisplayName());
                    return true;
                }
            }
        }
        return false;
    }
}
