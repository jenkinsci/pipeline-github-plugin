package org.jenkinsci.plugins.pipeline.github;

import groovy.lang.GroovyObjectSupport;
import org.eclipse.egit.github.core.CommitStatus;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Groovy wrapper over a {@link CommitStatus}
 *
 * @author Aaron Whiteside
 * @see CommitStatus
 */
public class CommitStatusGroovyObject extends GroovyObjectSupport implements Serializable {
    private static final long serialVersionUID = 1L;

    private final CommitStatus commitStatus;

    CommitStatusGroovyObject(final CommitStatus commitStatus) {
        Objects.requireNonNull(commitStatus, "commitStatus cannot be null");

        this.commitStatus = commitStatus;
    }

    @Whitelisted
    public String getCreator() {
        return commitStatus.getCreator().getLogin();
    }

    @Whitelisted
    public Date getCreatedAt() {
        return commitStatus.getCreatedAt();
    }

    @Whitelisted
    public Date getUpdatedAt() {
        return commitStatus.getUpdatedAt();
    }

    @Whitelisted
    public long getId() {
        return commitStatus.getId();
    }

    @Whitelisted
    public String getContext() {
        return commitStatus.getContext();
    }

    @Whitelisted
    public String getDescription() {
        return commitStatus.getDescription();
    }

    @Whitelisted
    public String getState() {
        return commitStatus.getState();
    }

    @Whitelisted
    public String getTargetUrl() {
        return commitStatus.getTargetUrl();
    }

    @Whitelisted
    public String getUrl() {
        return commitStatus.getUrl();
    }
}
