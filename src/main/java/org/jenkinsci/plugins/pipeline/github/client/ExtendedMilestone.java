package org.jenkinsci.plugins.pipeline.github.client;

import org.eclipse.egit.github.core.Milestone;

import java.util.Date;

/**
 * @author Aaron Whiteside
 */
public class ExtendedMilestone extends Milestone {
    private static final long serialVersionUID = 8017385076255266092L;

    private Date updatedAt;
    private Date closedAt;

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }
}
