package org.jenkinsci.plugins.pipeline.github;

import groovy.lang.GroovyObjectSupport;
import org.eclipse.egit.github.core.RepositoryId;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitComment;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitService;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Objects;

/**
 * Groovy wrapper over {@link ExtendedCommitComment}
 *
 * Additionally provides one the ability to update the comment body and delete the comment.
 *
 * @author Aaron Whiteside
 * @see ExtendedCommitComment
 */
public class ReviewCommentGroovyObject extends GroovyObjectSupport {
    private final RepositoryId base;
    private final ExtendedCommitService commitService;

    private ExtendedCommitComment commitComment;

    ReviewCommentGroovyObject(final ExtendedCommitComment commitComment,
                              final RepositoryId base,
                              final ExtendedCommitService commitService) {
        this.commitComment = commitComment;
        this.base = base;
        this.commitService = commitService;
    }

    @Whitelisted
    public Integer getLine() {
        return commitComment.getLine();
    }

    @Whitelisted
    public Integer getPosition() {
        return commitComment.getPosition();
    }

    @Whitelisted
    public Integer getOriginalPosition() {
        return commitComment.getOriginalPosition();
    }

    @Whitelisted
    public String getCommitId() {
        return commitComment.getCommitId();
    }

    @Whitelisted
    public String getOriginalCommitId() {
        return commitComment.getOriginalCommitId();
    }

    @Whitelisted
    public String getPath() {
        return commitComment.getPath();
    }

    @Whitelisted
    public String getDiffHunk() {
        return commitComment.getDiffHunk();
    }

    @Whitelisted
    public Date getCreatedAt() {
        return commitComment.getCreatedAt();
    }

    @Whitelisted
    public Date getUpdatedAt() {
        return commitComment.getUpdatedAt();
    }

    @Whitelisted
    public String getBody() {
        return commitComment.getBody();
    }

    @Whitelisted
    public long getId() {
        return commitComment.getId();
    }

    @Whitelisted
    public String getUrl() {
        return commitComment.getUrl();
    }

    @Whitelisted
    public String getUser() {
        return GitHubHelper.userToLogin(commitComment.getUser());
    }

    @Whitelisted
    public void setBody(final String body) {
        Objects.requireNonNull(body, "body cannot be null");

        ExtendedCommitComment edit = new ExtendedCommitComment();
        edit.setId(commitComment.getId());
        edit.setBody(body);
        try {
            commitComment = commitService.editComment2(base, edit);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void delete() {
        try {
            commitService.deleteComment(base, commitComment.getId());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

//    @Whitelisted
    public void replyTo(final String body) {
        Objects.requireNonNull(body, "body is a required argument");
        try {
            commitService.replyToComment(base, commitComment.getCommitId(), (int) commitComment.getId(), body);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
