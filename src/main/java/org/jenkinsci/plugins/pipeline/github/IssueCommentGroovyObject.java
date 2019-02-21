package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.GroovyObjectSupport;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Objects;

/**
 * Groovy wrapper over a {@link Comment}
 *
 * Additionally provides one the ability to update the comment body and delete the comment.
 *
 * @author Aaron Whiteside
 * @see Comment
 */
@SuppressFBWarnings("SE_BAD_FIELD")
public class IssueCommentGroovyObject extends GroovyObjectSupport implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String jobId;
    private final RepositoryId base;
    private Comment comment;

    private transient IssueService issueService;

    IssueCommentGroovyObject(final String jobId,
                             final Comment comment,
                             final RepositoryId base,
                             final IssueService issueService) {
        this.jobId = Objects.requireNonNull(jobId, "jobId cannot be null");
        this.comment = Objects.requireNonNull(comment, "comment cannot be null");
        this.base = Objects.requireNonNull(base, "base cannot be null");
        this.issueService = Objects.requireNonNull(issueService, "issueService cannot be null");
    }

    private IssueService getIssueService() {
        if (issueService == null) {
            issueService = new IssueService(GitHubHelper.getGitHubClient(GitHubHelper.getJob(jobId)));
        }
        return issueService;
    }

    @Whitelisted
    public Date getCreatedAt() {
        return comment.getCreatedAt();
    }

    @Whitelisted
    public Date getUpdatedAt() {
        return comment.getUpdatedAt();
    }

    @Whitelisted
    public String getBody() {
        return comment.getBody();
    }

    @Whitelisted
    public String getBodyHtml() {
        return comment.getBodyHtml();
    }

    @Whitelisted
    public String getBodyText() {
        return comment.getBodyText();
    }

    @Whitelisted
    public long getId() {
        return comment.getId();
    }

    @Whitelisted
    public String getUrl() {
        return comment.getUrl();
    }

    @Whitelisted
    public String getUser() {
        return GitHubHelper.userToLogin(comment.getUser());
    }

    @Whitelisted
    public void setBody(final String body) {
        Comment edit = new Comment();
        edit.setId(comment.getId());
        edit.setBody(body);
        try {
            comment = getIssueService().editComment(base, edit);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void delete() {
        try {
            getIssueService().deleteComment(base, comment.getId());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
