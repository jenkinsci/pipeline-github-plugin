package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import groovy.lang.GroovyObjectSupport;
import hudson.model.Job;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedGitHubClient;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.Serializable;

/**
 * Groovy object that represents a GitHub Repository.
 */
@PersistIn(PersistenceContext.NONE)
public class RepositoryGroovyObject extends GroovyObjectSupport implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String jobId;
    private Repository repository;

    private transient Job job;
    private transient ExtendedGitHubClient gitHubClient;
    private transient RepositoryService repositoryService;

    RepositoryGroovyObject(@NonNull final Job job) throws Exception {
        this.job = job;
        this.jobId = job.getFullName();

        RepositoryId repositoryId = GitHubHelper.getRepositoryId(job);
        if (repositoryId == null) {
            throw new IllegalStateException("Job is not a GitHub job");
        }

        this.repository = getRepositoryService().getRepository(repositoryId.getOwner(), repositoryId.getName());
    }

    private Job getJob() {
        if (job == null) {
            job = GitHubHelper.getJob(jobId);
        }
        return job;
    }

    private ExtendedGitHubClient getGitHubClient() {
        if (gitHubClient == null) {
            gitHubClient = GitHubHelper.getGitHubClient(getJob());
        }
        return gitHubClient;
    }

    private RepositoryService getRepositoryService() {
        if (repositoryService == null) {
            repositoryService = new RepositoryService(getGitHubClient());
        }
        return repositoryService;
    }

    @Whitelisted
    public String getName() {
        return repository.getName();
    }

    @Whitelisted
    public String getOwner() {
        return repository.getOwner().getLogin();
    }

    @Whitelisted
    public String getDefaultBranch() {
        return repository.getDefaultBranch();
    }

    @Whitelisted
    public String getDescription() {
        return repository.getDescription();
    }

    @Whitelisted
    public String getHtmlUrl() {
        return repository.getHtmlUrl();
    }

    @Whitelisted
    public String getCloneUrl() {
        return repository.getCloneUrl();
    }

    @Whitelisted
    public String getSshUrl() {
        return repository.getSshUrl();
    }

    @Whitelisted
    public String getGitUrl() {
        return repository.getGitUrl();
    }

    @Whitelisted
    public boolean isPrivate() {
        return repository.isPrivate();
    }

    @Whitelisted
    public boolean isFork() {
        return repository.isFork();
    }

    @Whitelisted
    public long getId() {
        return repository.getId();
    }

}
