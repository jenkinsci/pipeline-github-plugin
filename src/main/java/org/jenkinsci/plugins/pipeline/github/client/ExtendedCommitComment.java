package org.jenkinsci.plugins.pipeline.github.client;

import org.eclipse.egit.github.core.CommitComment;

/**
 * @author Aaron Whiteside
 */
public class ExtendedCommitComment extends CommitComment {
    private static final long serialVersionUID = 4834285683963788350L;

    private String pullRequestUrl;
    private long pullRequestReviewId;
    private long inReplyToId;

    public long getInReplyToId() {
        return inReplyToId;
    }

    public void setInReplyToId(final long inReplyToId) {
        this.inReplyToId = inReplyToId;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public void setPullRequestUrl(final String pullRequestUrl) {
        this.pullRequestUrl = pullRequestUrl;
    }

    public long getPullRequestReviewId() {
        return pullRequestReviewId;
    }

    public void setPullRequestReviewId(long pullRequestReviewId) {
        this.pullRequestReviewId = pullRequestReviewId;
    }
}
