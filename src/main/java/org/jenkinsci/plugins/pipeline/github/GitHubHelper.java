package org.jenkinsci.plugins.pipeline.github;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.impl.NullSCMSource;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CollaboratorService;
import org.jenkinsci.plugins.github_branch_source.Connector;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedGitHubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Various utility methods to obtain clients, repos and pull request scm heads from Jobs
 *
 * @author Aaron Whiteside
 */
public class GitHubHelper {

    private final static Logger LOG = LoggerFactory.getLogger(GitHubHelper.class);

    private GitHubHelper() {
        // go away
    }

    @CheckForNull
    public static SCMSource getScmSource(@NonNull final Job<?, ?> job) {
        return getScmSource(job, job.getLastBuild());
    }

    @CheckForNull
    public static SCMSource getScmSource(@NonNull final Run<?, ?> run) {
        return getScmSource(run.getParent(), run);
    }

    @CheckForNull
    private static SCMSource getScmSource(final Job<?, ?> job, final Run<?, ?> run) {
        SCMSource source = SCMSource.SourceByItem.findSource(job);
        LOG.trace("Resolved SCM source type {} for job {}", source != null ? source.getClass() : "null", job.getFullName());
        if (source instanceof NullSCMSource && run != null && job.getParent() instanceof SCMSourceOwner) {
            LOG.debug("Attempting SCM source recovery for run {}", run.getFullDisplayName());
            // dead branches lose their source ID, but the build's revision action retains it
            SCMRevisionAction action = run.getAction(SCMRevisionAction.class);
            if (action != null && action.getSourceId() != null) {
                SCMSource original = ((SCMSourceOwner) job.getParent()).getSCMSource(action.getSourceId());
                if (original != null) {
                    LOG.debug("Recovered SCM source {} for orphaned job {}", action.getSourceId(), job.getFullName());
                    return original;
                }
                LOG.warn("Cannot recover SCM source {} for orphaned job {}", action.getSourceId(), job.getFullName());
            } else {
                LOG.info("No SCM revision source ID recorded for run {}; cannot recover SCM source", run.getFullDisplayName());
            }
        }
        return source;
    }

    public static Boolean isAuthorized(@NonNull final Job<?,?> job, @NonNull final String User) {
        ExtendedGitHubClient client = getGitHubClient(job);
        RepositoryId repository = getRepositoryId(job);
        CollaboratorService collaboratorService = new CollaboratorService(client);

        try {
            return collaboratorService.isCollaborator(repository, User);
        } catch (final IOException e) {
            LOG.debug("Received an exception while trying to check if user {} is a collaborator of repository: {}",
                    User, repository, e);
            return false;
        }
    }

    public static ExtendedGitHubClient getGitHubClient(@NonNull final Job<?,?> job) {
        SCMSource scmSource = getScmSource(job);
        if (scmSource instanceof GitHubSCMSource) {
            GitHubSCMSource gitHubSource = (GitHubSCMSource) scmSource;

            URI uri = URI.create(gitHubSource.getApiUri());
            ExtendedGitHubClient client = new ExtendedGitHubClient(uri.getHost(), uri.getPort(), uri.getScheme());

            // configure credentials
            if (gitHubSource.getCredentialsId() != null) {
                StandardCredentials credentials = Connector.lookupScanCredentials(
                        job, gitHubSource.getApiUri(), gitHubSource.getCredentialsId(), gitHubSource.getRepoOwner());

                if (credentials instanceof StandardUsernamePasswordCredentials) {
                    StandardUsernamePasswordCredentials c = (StandardUsernamePasswordCredentials) credentials;
                    String userName = c.getUsername();
                    String password = c.getPassword().getPlainText();
                    client.setCredentials(userName, password);
                }
            }
            return client;
        }
        throw new IllegalArgumentException("Job's SCM is not GitHub.");
    }

    public static RepositoryId getRepositoryId(@NonNull final Job<?,?> job) {
        SCMSource src = getScmSource(job);
        if (src instanceof GitHubSCMSource) {
            GitHubSCMSource source = (GitHubSCMSource) src;
            return RepositoryId.create(source.getRepoOwner(), source.getRepository());
        }
        return null;
    }

    public static PullRequestSCMHead getPullRequest(@NonNull final Job job) throws Exception {
        PullRequestSCMHead head = (PullRequestSCMHead) SCMHead.HeadByItem.findHead(job);
        if (head == null) {
            throw new IllegalStateException("Build is not a Pull Request");
        }
        return head;
    }

    public static String userToLogin(final User user) {
        return user == null ? null : user.getLogin();
    }

    public static Job getJob(final String jobId) {
        final Job job = Jenkins.get().getItemByFullName(jobId, Job.class);
        if (job == null) {
            throw new IllegalStateException("Unable to find Job: " + jobId);
        }
        return job;
    }

}
