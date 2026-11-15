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
 * Factory for our {@link RepositoryGlobalVariable}, {@link PullRequestGlobalVariable}, and {@link MergedPullRequestGlobalVariable} (if applicable) instances.
 *
 * @author Aaron Whiteside
 * @see RepositoryGlobalVariable
 * @see PullRequestGlobalVariable
 * @see MergedPullRequestGlobalVariable
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

        LOG.debug("Processing global variables for run: {}", run.getFullDisplayName());

        SCMSource scmSource = SCMSource.SourceByItem.findSource(run.getParent());
        LOG.debug("Found SCM source type: {}", scmSource != null ? scmSource.getClass() : "null");

        if (scmSource instanceof GitHubSCMSource) {
            LOG.debug("SCM source is GitHubSCMSource, creating global variables");
            GitHubSCMSource gitHubSource = (GitHubSCMSource) scmSource;
            Collection<GlobalVariable> result = new LinkedList<>();
            result.add(new RepositoryGlobalVariable());

            SCMHead scmHead = SCMHead.HeadByItem.findHead(run.getParent());
            LOG.debug("Found SCM head type: {}", scmHead != null ? scmHead.getClass() : "null");

            if (scmHead instanceof PullRequestSCMHead) {
                result.add(new PullRequestGlobalVariable());

                if (shouldDetectMergedPullRequest(gitHubSource, run)) {
                    LOG.debug("MergedPullRequestGlobalVariable for PR run: {}", run.getFullDisplayName());
                    result.add(new MergedPullRequestGlobalVariable());
                }
            } else if (scmHead instanceof BranchSCMHead && shouldDetectMergedPullRequest(gitHubSource, run)) {
                LOG.debug("MergedPullRequestGlobalVariable for branch run: {}", run.getFullDisplayName());
                result.add(new MergedPullRequestGlobalVariable());
            }
            return result;
        }

        LOG.debug("SCM source is not GitHubSCMSource, returning empty collection");
        return Collections.emptyList();
    }

    private boolean shouldDetectMergedPullRequest(GitHubSCMSource gitHubSource, final Run<?, ?> run) {
        List<SCMSourceTrait> traits = gitHubSource.getTraits();
        LOG.trace("Checking {} traits for DetectMergedPullRequestTrait", traits.size());

        for (SCMSourceTrait trait : traits) {
            LOG.trace("Evaluating trait: {}", trait.getClass());

            if (trait instanceof DetectMergedPullRequestTrait) {
                LOG.debug("DetectMergedPullRequestTrait found for run: {}", run.getFullDisplayName());
                return true;
            }
        }
        return false;
    }
}
