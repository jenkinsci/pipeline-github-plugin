package org.jenkinsci.plugins.pipeline.github;

import groovy.lang.GroovyObjectSupport;
import org.eclipse.egit.github.core.Milestone;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedMilestone;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Groovy wrapper over {@link Milestone}
 *
 * @author Aaron Whiteside
 * @see Milestone
 */
public class MilestoneGroovyObject extends GroovyObjectSupport implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String jobId;
    private final ExtendedMilestone milestone;

    MilestoneGroovyObject(final String jobId, final ExtendedMilestone milestone) {
        this.jobId = Objects.requireNonNull(jobId, "jobId cannot be null");
        this.milestone = Objects.requireNonNull(milestone, "milestone cannot be null");
    }

    @Whitelisted
    public Date getCreatedAt() {
        return milestone.getCreatedAt();
    }

    @Whitelisted
    public Date getDueOn() {
        return milestone.getDueOn();
    }

    @Whitelisted
    public int getClosedIssues() {
        return milestone.getClosedIssues();
    }

    @Whitelisted
    public int getNumber() {
        return milestone.getNumber();
    }

    @Whitelisted
    public int getOpenIssues() {
        return milestone.getOpenIssues();
    }

    @Whitelisted
    public String getDescription() {
        return milestone.getDescription();
    }

    @Whitelisted
    public String getState() {
        return milestone.getState();
    }

    @Whitelisted
    public String getTitle() {
        return milestone.getTitle();
    }

    @Whitelisted
    public String getUrl() {
        return milestone.getUrl();
    }

    @Whitelisted
    public String getCreator() {
        return milestone.getCreator().getLogin();
    }

    @Whitelisted
    public Date getUpdatedAt() {
        return milestone.getUpdatedAt();
    }

    @Whitelisted
    public Date getClosedAt() {
        return milestone.getClosedAt();
    }

}
