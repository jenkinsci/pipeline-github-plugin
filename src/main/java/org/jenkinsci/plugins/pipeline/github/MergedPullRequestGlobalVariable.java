package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.eclipse.egit.github.core.RepositoryId;
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequest;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequestService;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergedPullRequestGlobalVariable extends GlobalVariable {
    private static final Logger LOG = LoggerFactory.getLogger(MergedPullRequestGlobalVariable.class);

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
        return new MergedPullRequestGroovyObject(build.getParent(), getMergedPullRequest(build));
    }

    private ExtendedPullRequest getMergedPullRequest(final Run<?, ?> run) {
        SCMSource scmSource = SCMSource.SourceByItem.findSource(run.getParent());
        if (null != scmSource && scmSource instanceof GitHubSCMSource) {
            GitHubSCMSource gitHubSource = (GitHubSCMSource) scmSource;
            SCMRevision revision = SCMRevisionAction.getRevision(scmSource, run);
            if (null != revision && revision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
                AbstractGitSCMSource.SCMRevisionImpl gitRevision = (AbstractGitSCMSource.SCMRevisionImpl) revision;
                LOG.debug("revision hash for run {}: {}", run.getFullDisplayName(), gitRevision.getHash());
                return getPullRequest(run, gitHubSource, gitRevision.getHash());
            }
        }
        return null;
    }

    private ExtendedPullRequest getPullRequest(Run<?, ?> run, GitHubSCMSource gitHubSource, String commitHash) {
        try {
            RepositoryId repoId = GitHubHelper.getRepositoryId(run.getParent());
            ExtendedPullRequestService prService = new ExtendedPullRequestService(GitHubHelper.getGitHubClient(run.getParent()));

            // Get the target branch name, if available
            String targetBranch = null;
            SCMHead scmHead = SCMHead.HeadByItem.findHead(run.getParent());
            if (scmHead instanceof BranchSCMHead) {
                targetBranch = ((BranchSCMHead) scmHead).getName();
            } else if (scmHead instanceof PullRequestSCMHead) {
                targetBranch = ((PullRequestSCMHead) scmHead).getTarget().getName();
            }

            return prService.getMergedPullRequest(repoId, commitHash, targetBranch);
        } catch (Exception e) {
            LOG.warn("Failed to query GitHub API for pull requests with merge commit {}; job = {}", commitHash, run.getFullDisplayName(), e);
        }
        return null;
    }

}
