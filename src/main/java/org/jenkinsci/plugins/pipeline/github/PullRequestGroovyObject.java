package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.GroovyObjectSupport;
import hudson.model.Job;
import hudson.model.Run;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitComment;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitService;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedGitHubClient;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedIssueService;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedMergeStatus;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequest;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequestService;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Groovy object that represents a GitHub PullRequest.
 *
 * TODO: better javadoc
 *
 * @author Aaron Whiteside
 * @see ExtendedPullRequest
 */
@PersistIn(PersistenceContext.NONE)
@SuppressFBWarnings("SE_BAD_FIELD")
public class PullRequestGroovyObject extends GroovyObjectSupport implements Serializable {

    private final CpsScript script;

    private final PullRequestSCMHead pullRequestHead;
    private final RepositoryId base;
    private final RepositoryId head;
    private final ExtendedGitHubClient gitHubClient;

    private final ExtendedPullRequestService pullRequestService;
    private final ExtendedIssueService issueService;
    private final ExtendedCommitService commitService;
    private ExtendedPullRequest pullRequest;

    public PullRequestGroovyObject(@Nonnull final CpsScript script) throws Exception {
        this.script = script;
        Run<?, ?> build = script.$build();
        if (build == null) {
            throw new IllegalStateException("No associated build");
        }
        Job job = build.getParent();

        this.pullRequestHead = GitHubHelper.getPullRequest(job);

        this.base = GitHubHelper.getRepositoryId(job);
        this.head = RepositoryId.create(pullRequestHead.getSourceOwner(), pullRequestHead.getSourceRepo());

        this.gitHubClient = GitHubHelper.getGitHubClient(job);
        this.pullRequestService = new ExtendedPullRequestService(gitHubClient);
        this.issueService = new ExtendedIssueService(gitHubClient);
        this.commitService = new ExtendedCommitService(gitHubClient);
        this.pullRequest = pullRequestService.getPullRequest(base, pullRequestHead.getNumber());
    }

    @Whitelisted
    public long getId() {
        return pullRequest.getId();
    }

    @Whitelisted
    public int getNumber() {
        return pullRequest.getNumber();
    }

    @Whitelisted
    public String getDiffUrl() {
        return pullRequest.getDiffUrl();
    }

    @Whitelisted
    public String getUrl() {
        return pullRequest.getHtmlUrl();
    }

    @Whitelisted
    public String getPatchUrl() {
        return pullRequest.getPatchUrl();
    }

    @Whitelisted
    public String getState() {
        return pullRequest.getState();
    }

    @Whitelisted
    public String getIssueUrl() {
        return pullRequest.getIssueUrl();
    }

    @Whitelisted
    public String getTitle() {
        return pullRequest.getTitle();
    }

    @Whitelisted
    public String getBody() {
        return pullRequest.getBody();
    }

    @Whitelisted
    public boolean isLocked() {
        return pullRequest.isLocked();
    }

    @Whitelisted
    public int getMilestone() {
        return pullRequest.getMilestone().getNumber();
    }

    @Whitelisted
    public String getHead() {
        return pullRequest.getHead().getSha();
    }

    @Whitelisted
    public String getHeadRef() {
        return pullRequest.getHead().getRef();
    }

    @Whitelisted
    public String getBase() {
        return pullRequest.getBase().getRef();
    }

    @Whitelisted
    public Date getUpdatedAt() {
        return pullRequest.getUpdatedAt();
    }

    @Whitelisted
    public Date getCreatedAt() {
        return pullRequest.getCreatedAt();
    }

    @Whitelisted
    public String getCreatedBy() {
        return GitHubHelper.userToLogin(pullRequest.getUser());
    }

    @Whitelisted
    public Date getClosedAt() {
        return pullRequest.getCreatedAt();
    }

    @Whitelisted
    public String getClosedBy() {
        return GitHubHelper.userToLogin(pullRequest.getClosedBy());
    }

    @Whitelisted
    public Date getMergedAt() {
        return pullRequest.getMergedAt();
    }

    @Whitelisted
    public String getMergedBy() {
        return GitHubHelper.userToLogin(pullRequest.getMergedBy());
    }

    @Whitelisted
    public int getCommitCount() {
        return pullRequest.getCommits();
    }

    @Whitelisted
    public int getCommentCount() {
        return pullRequest.getComments();
    }

    @Whitelisted
    public int getDeletions() {
        return pullRequest.getDeletions();
    }

    @Whitelisted
    public String getMergeCommitSha() {
        return pullRequest.getMergeCommitSha();
    }

    @Whitelisted
    public boolean isMaintainerCanModify() {
        return pullRequest.isMaintainerCanModify();
    }

    @Whitelisted
    public int getAdditions() {
        return pullRequest.getAdditions();
    }

    @Whitelisted
    public int getChangedFiles() {
        return pullRequest.getChangedFiles();
    }

    @Whitelisted
    public boolean isMergeable() {
        return pullRequest.isMergeable();
    }

    @Whitelisted
    public boolean isMerged() {
        return pullRequest.isMerged();
    }

    @Whitelisted
    public Iterable<String> getRequestedReviewers() {
        Stream<String> stream = StreamSupport
                .stream(pullRequestService.pageRequestedReviewers(base, pullRequest.getNumber())
                        .spliterator(), false)
                .flatMap(Collection::stream)
                .map(User::getLogin);

        return stream::iterator;
    }

    @Whitelisted
    public List<CommitStatusGroovyObject> getStatuses() {
        try {
            return commitService.getStatuses(base, pullRequest.getHead().getSha())
                    .stream()
                    .map(CommitStatusGroovyObject::new)
                    .collect(toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public Iterable<String> getLabels() {
        Stream<String> stream = StreamSupport
                .stream(issueService.getLabels(base, pullRequest.getNumber())
                        .spliterator(), false)
                .flatMap(Collection::stream)
                .map(Label::getName);

        return stream::iterator;
    }

    @Whitelisted
    public List<String> getAssignees() {
        return pullRequest.getAssignees()
                .stream()
                .map(User::getLogin)
                .collect(toList());
    }

    @Whitelisted
    public Iterable<CommitGroovyObject> getCommits() {
        try {
            Stream<CommitGroovyObject> steam = pullRequestService
                    .getCommits(base, pullRequestHead.getNumber())
                    .stream()
                    .map(c -> new CommitGroovyObject(c, commitService, base));

            return steam::iterator;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public Iterable<IssueCommentGroovyObject> getComments() {
        try {
            Stream<IssueCommentGroovyObject> stream = issueService
                    .getComments(base, pullRequestHead.getNumber())
                    .stream()
                    .map(c -> new IssueCommentGroovyObject(c, base, issueService));

            return stream::iterator;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public Iterable<ReviewCommentGroovyObject> getReviewComments() {
        Stream<ReviewCommentGroovyObject> stream = StreamSupport
                .stream(pullRequestService.pageComments2(base,
                        pullRequestHead.getNumber()).spliterator(), false)
                .flatMap(Collection::stream)
                .map(c -> new ReviewCommentGroovyObject(c, base, commitService));
        return stream::iterator;
    }

    @Whitelisted
    public List<CommitFileGroovyObject> getFiles() {
        try {
            return pullRequestService.getFiles(base, pullRequestHead.getNumber())
                    .stream()
                    .map(CommitFileGroovyObject::new)
                    .collect(toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void setMilestone(final int milestoneNumber) {
        // todo
    }

    @Whitelisted
    public void setLocked(final boolean locked) {
        try {
            if (locked) {
                issueService.lockIssue(base, pullRequest.getNumber());
            } else {
                issueService.unlockIssue(base, pullRequest.getNumber());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setTitle(final String title) {
        Objects.requireNonNull(title, "title cannot be null");

        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setTitle(title);
        try {
            pullRequest = pullRequestService.editPullRequest(base, edit);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setBody(final String body) {
        Objects.requireNonNull(body, "body cannot be null");

        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setBody(body);
        try {
            pullRequest = pullRequestService.editPullRequest(base, edit);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setState(final String state) {
        Objects.requireNonNull(state, "state cannot be null");

        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setState(state);
        try {
            pullRequest = pullRequestService.editPullRequest(base, edit);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setBase(final String newBase) {
        Objects.requireNonNull(newBase, "base cannot be null");

        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setBase(new PullRequestMarker().setRef(newBase));
        try {
            pullRequest = pullRequestService.editPullRequest(base, edit);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setMaintainerCanModify(final boolean value) {
        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setMaintainerCanModify(value);
        try {
            pullRequest = pullRequestService.editPullRequest(base, edit);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setLabels(List<String> labels) {
        if (labels == null) {
            labels = Collections.emptyList();
        }
        try {
            issueService.setLabels(base, pullRequest.getNumber(), labels);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void createReviewRequests(final List<String> reviewers) {
        Objects.requireNonNull(reviewers, "reviewers cannot be null");
        try {
            pullRequestService.createReviewRequests(base, pullRequest.getNumber(), reviewers);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void deleteReviewRequests(final List<String> reviewers) {
        Objects.requireNonNull(reviewers, "reviewers cannot be null");
        try {
            pullRequestService.deleteReviewRequests(base, pullRequest.getNumber(), reviewers);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void addLabels(final List<String> labels) {
        Objects.requireNonNull(labels, "labels is a required argument");
        try {
            issueService.addLabels(base, pullRequest.getNumber(), labels);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void removeLabel(final String label) {
        Objects.requireNonNull(label, "label is a required argument");
        try {
            issueService.removeLabel(base, pullRequest.getNumber(), label);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void addAssignees(final List<String> assignees) {
        Objects.requireNonNull(assignees, "assignees is a required argument");
        try {
            issueService.addAssignees(base, pullRequest.getNumber(), assignees);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setAssignees(final List<String> assignees) {
        Objects.requireNonNull(assignees, "assignees is a required argument");
        try {
            issueService.setAssignees(base, pullRequest.getNumber(), assignees);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void removeAssignees(final List<String> assignees) {
        Objects.requireNonNull(assignees, "assignees is a required argument");
        try {
            issueService.removeAssignees(base, pullRequest.getNumber(), assignees);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public CommitStatusGroovyObject createStatus(final Map<String, String> params) {
        return createStatus(params.get("status"),
                            params.get("context"),
                            params.get("description"),
                            params.get("targetUrl"));
    }

    @Whitelisted
    public CommitStatusGroovyObject createStatus(final String status,
                                                 final String context,
                                                 final String description,
                                                 final String targetUrl) {
        Objects.requireNonNull(status, "status is a required argument");

        CommitStatus commitStatus = new CommitStatus();
        commitStatus.setState(status);
        commitStatus.setContext(context);
        commitStatus.setDescription(description);
        commitStatus.setTargetUrl(targetUrl);
        try {
            return new CommitStatusGroovyObject(
                    commitService.createStatus(head, pullRequest.getHead().getSha(), commitStatus));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public ReviewCommentGroovyObject reviewComment(final String commitId,
                                                   final String path,
                                                   final int position,
                                                   final String body) {
        Objects.requireNonNull(commitId, "commitId is a required argument");
        Objects.requireNonNull(path, "path is a required argument");
        Objects.requireNonNull(body, "body is a required argument");

        ExtendedCommitComment comment = new ExtendedCommitComment();
        comment.setCommitId(commitId);
        comment.setPath(path);
        comment.setPosition(position);
        comment.setBody(body);
        try {
            return new ReviewCommentGroovyObject(
                    pullRequestService.createComment2(base, pullRequestHead.getNumber(), comment),
                    base,
                    commitService);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public ReviewCommentGroovyObject replyToReviewComment(final long commentId, final String body) {
        Objects.requireNonNull(body, "body is a required argument");
        try {
            return new ReviewCommentGroovyObject(
                    pullRequestService.replyToComment2(base, pullRequestHead.getNumber(), (int) commentId, body),
                    base,
                    commitService);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void deleteReviewComment(final long commentId) {
        try {
            pullRequestService.deleteComment(base, commentId);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public ReviewCommentGroovyObject editReviewComment(final long commentId, final String body) {
        Objects.requireNonNull(body, "body is a required argument");

        ExtendedCommitComment comment = new ExtendedCommitComment();
        comment.setId(commentId);
        comment.setBody(body);
        try {
            return new ReviewCommentGroovyObject(pullRequestService.editComment2(base, comment), base, commitService);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public IssueCommentGroovyObject comment(final String body) {
        Objects.requireNonNull(body, "body is a required argument");

        try {
            return new IssueCommentGroovyObject(
                    issueService.createComment(base, pullRequestHead.getNumber(), body),
                    base,
                    issueService);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public IssueCommentGroovyObject editComment(final long commentId, final String body) {
        Objects.requireNonNull(body, "body is a required argument");

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setBody(body);
        try {
            return new IssueCommentGroovyObject(
                    issueService.editComment(base, comment),
                    base,
                    issueService);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void deleteComment(final long commentId) {
        try {
            issueService.deleteComment(base, commentId);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public String merge(final Map<String, String> params) {
        return merge(params.get("commitTitle"),
                params.get("commitMessage"),
                params.get("sha"),
                params.get("mergeMethod"));
    }

    @Whitelisted
    public String merge(final String commitTitle,
                        final String commitMessage,
                        final String sha,
                        final String mergeMethod) {
        try {
            ExtendedMergeStatus status = pullRequestService.merge(base,
                    pullRequestHead.getNumber(),
                    commitTitle,
                    commitMessage,
                    sha,
                    mergeMethod);
            if (status.isMerged()) {
                return status.getSha();
            } else {
                throw new RuntimeException(status.getMessage());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void refresh() {
        try {
            pullRequest = pullRequestService.getPullRequest(base, pullRequest.getNumber());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setCredentials(final String userName, final String password) {
        gitHubClient.setCredentials(userName, password);
    }
}
