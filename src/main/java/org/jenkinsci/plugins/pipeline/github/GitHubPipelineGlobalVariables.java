package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.BooleanParameterValue;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceTrait;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.eclipse.egit.github.core.RepositoryId;
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequest;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequestService;
import org.jenkinsci.plugins.pipeline.github.trigger.GitHubEnvironmentVariablesAction;
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
        PullRequestGroovyObject mergedPr = getMergedPullRequest(run, scmHead);
        if (scmHead instanceof PullRequestSCMHead) {
            Collection<GlobalVariable> result = new LinkedList<>();
            result.add(new PullRequestGlobalVariable());
            if (null != mergedPr) {
                result.add(new MergedPullRequestGlobalVariable(mergedPr));
            }
            return result;
        } else if (scmHead instanceof BranchSCMHead && null != mergedPr) {
            Collection<GlobalVariable> result = new LinkedList<>();
            result.add(new MergedPullRequestGlobalVariable(mergedPr));
            return result;
        }
        return Collections.emptyList();
    }

    private PullRequestGroovyObject getMergedPullRequest(final Run<?, ?> run, SCMHead scmHead) {
        if (detectMergedPullRequest(run)) {
            SCMSource scmSource = SCMSource.SourceByItem.findSource(run.getParent());
            if (null != scmSource && scmSource instanceof GitHubSCMSource) {
                GitHubSCMSource gitHubSource = (GitHubSCMSource) scmSource;
                SCMRevision revision = SCMRevisionAction.getRevision(scmSource, run);
                if (null != revision && revision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
                    AbstractGitSCMSource.SCMRevisionImpl gitRevision = (AbstractGitSCMSource.SCMRevisionImpl) revision;
                    LOG.debug("revision hash for run {}: {}", run.getFullDisplayName(), gitRevision.getHash());
                    ExtendedPullRequest pr = getPullRequest(run, gitHubSource, scmHead, gitRevision.getHash());
                    if (null != pr) {
                        try {
                            run.addOrReplaceAction(new GitHubEnvironmentVariablesAction(new BooleanParameterValue("GITHUB_PR_MERGE_DETECTED", true)));
                            return new PullRequestGroovyObject(run.getParent(), pr);
                        } catch (Exception e) {
                            LOG.error("Failed to create PullRequestGroovyObject for PR #{} for run {}", pr.getNumber(), run.getFullDisplayName(), e);
                        }
                    }
                }
            }
        }
        return null;
    }

    private ExtendedPullRequest getPullRequest(Run<?, ?> run, GitHubSCMSource gitHubSource, SCMHead scmHead, String commitHash) {
        try {
            RepositoryId repoId = GitHubHelper.getRepositoryId(run.getParent());
            ExtendedPullRequestService prService = new ExtendedPullRequestService(GitHubHelper.getGitHubClient(run.getParent()));

            // Get the target branch name if available
            String targetBranch = null;
            if (scmHead instanceof BranchSCMHead) {
                targetBranch = ((BranchSCMHead) scmHead).getName();
            } else if (scmHead instanceof PullRequestSCMHead) {
                targetBranch = ((PullRequestSCMHead) scmHead).getTarget().getName();
            }

            return prService.getMergedPullRequest(repoId, commitHash, targetBranch);
        } catch (Exception e) {
            LOG.warn("Failed to query GitHub API for pull requests with merge commit {}", commitHash, e);
        }
        return null;
    }

    private boolean detectMergedPullRequest(final Run<?, ?> run) {
        SCMSource scmSource = SCMSource.SourceByItem.findSource(run.getParent());
        if (null != scmSource && scmSource instanceof GitHubSCMSource) {
            GitHubSCMSource gitHubSource = (GitHubSCMSource) scmSource;
            List<SCMSourceTrait> traits = gitHubSource.getTraits();
            for (SCMSourceTrait trait : traits) {
                if (trait instanceof DetectMergedPullRequestTrait) {
                    LOG.debug("DetectMergedPullRequestTrait found for run: {}", run.getFullDisplayName());
                    return true;
                }
            }
        }
        return false;
    }
}
