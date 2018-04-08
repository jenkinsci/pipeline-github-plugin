package org.jenkinsci.plugins.pipeline.github;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Job;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CollaboratorService;
import org.jenkinsci.plugins.github_branch_source.Connector;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedGitHubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Various utility methods to obtain clients, repos and pull request scm heads from Jobs
 *
 * @author Aaron Whiteside
 */
public class GitHubHelper {

    private final static Logger LOG = LoggerFactory.getLogger(GitHubHelper.class);

    private GitHubHelper(){
        // go away
    }

    public static List<String> getCollaborators(@Nonnull final Job<?,?> job) {
        ExtendedGitHubClient client = getGitHubClient(job);
        RepositoryId repository = getRepositoryId(job);
        CollaboratorService collaboratorService = new CollaboratorService(client);

        try {
            return collaboratorService.getCollaborators(repository)
                    .stream()
                    .map(User::getLogin)
                    .collect(Collectors.toList());
        } catch (final IOException e) {
            LOG.debug("Received an exception while trying to retrieve the collaborators for the repository: {}",
                    repository, e);
            return Collections.emptyList();
        }
    }

    public static ExtendedGitHubClient getGitHubClient(@Nonnull final Job<?,?> job) {
        SCMSource scmSource = SCMSource.SourceByItem.findSource(job);
        if (scmSource instanceof GitHubSCMSource) {
            GitHubSCMSource gitHubSource = (GitHubSCMSource) scmSource;

            ExtendedGitHubClient client;
            if (gitHubSource.getApiUri() == null) {
                client = new ExtendedGitHubClient();
            } else {
                URI uri = URI.create(gitHubSource.getApiUri());
                client = new ExtendedGitHubClient(uri.getHost(), uri.getPort(), uri.getScheme());
            }

            // configure credentials
            if (gitHubSource.getCredentialsId() != null) {
                StandardCredentials credentials = Connector.lookupScanCredentials(
                        job, gitHubSource.getApiUri(), gitHubSource.getCredentialsId());

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

    public static RepositoryId getRepositoryId(@Nonnull final Job<?,?> job) {
        SCMSource src = SCMSource.SourceByItem.findSource(job);
        if (src instanceof GitHubSCMSource) {
            GitHubSCMSource source = (GitHubSCMSource) src;
            if (source.getCredentialsId() != null) {
                return RepositoryId.create(source.getRepoOwner(), source.getRepository());
            }
        }
        return null;
    }

    public static PullRequestSCMHead getPullRequest(@Nonnull final Job job) throws Exception {
        PullRequestSCMHead head = (PullRequestSCMHead) SCMHead.HeadByItem.findHead(job);
        if (head == null) {
            throw new IllegalStateException("Build is not a pull request");
        }
        return head;
    }

    public static String userToLogin(final User user) {
        return user == null ? null : user.getLogin();
    }

}
