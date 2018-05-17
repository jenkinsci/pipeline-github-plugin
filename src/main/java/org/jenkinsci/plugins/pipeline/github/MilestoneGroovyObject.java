package org.jenkinsci.plugins.pipeline.github;

import groovy.lang.GroovyObjectSupport;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Date;

/**
 * Groovy object that represents a GitHub Milestone.
 *
 * @author jamespullar
 */

public class MilestoneGroovyObject extends GroovyObjectSupport implements Serializable {
    private Milestone milestone;

    public MilestoneGroovyObject(final Milestone milestone) {
        this.milestone = milestone;
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
    public User getCreator() {
        return milestone.getCreator();
    }

}
