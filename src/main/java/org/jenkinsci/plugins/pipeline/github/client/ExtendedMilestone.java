package org.jenkinsci.plugins.pipeline.github.client;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.util.DateUtils;

import java.util.Date;

/**
 * @author Aaron Whiteside
 */
public class ExtendedMilestone extends Milestone {
    private static final long serialVersionUID = 8017385076255266092L;

    private Date updatedAt;
    private Date closedAt;

    public Date getUpdatedAt() {
        return DateUtils.clone(this.updatedAt);
    }

    public void setUpdatedAt(final Date updatedAt) {
        this.updatedAt = DateUtils.clone(updatedAt);
    }

    public Date getClosedAt() {
        return DateUtils.clone(this.closedAt);
    }

    public void setClosedAt(final Date closedAt) {
        this.closedAt = DateUtils.clone(closedAt);
    }
}
