package org.jenkinsci.plugins.pipeline.github;

import groovy.lang.GroovyObjectSupport;
import org.eclipse.egit.github.core.CommitStatus;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.util.Date;

/**
 * Groovy wrapper over a {@link CommitStatus}
 *
 * @author Aaron Whiteside
 * @see CommitStatus
 */
public class CommitStatusGroovyObject extends GroovyObjectSupport {
    private final CommitStatus commitStatus;

    CommitStatusGroovyObject(final CommitStatus commitStatus) {
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
