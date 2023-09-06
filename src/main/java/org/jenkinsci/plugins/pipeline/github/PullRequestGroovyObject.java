package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.GroovyObjectSupport;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Team;
import org.eclipse.egit.github.core.User;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitComment;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitService;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedGitHubClient;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedIssueService;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedMergeStatus;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedMilestoneService;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequest;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequestService;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
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
import java.util.Optional;
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

    private static final long serialVersionUID = 1L;

    private final String jobId;
    private final PullRequestSCMHead pullRequestHead;
    private final RepositoryId base;
    private final RepositoryId head;

    private ExtendedPullRequest pullRequest;

    private transient Job job;
    private transient ExtendedGitHubClient gitHubClient;
    private transient ExtendedPullRequestService pullRequestService;
    private transient ExtendedIssueService issueService;
    private transient ExtendedCommitService commitService;
    private transient ExtendedMilestoneService milestoneService;

    PullRequestGroovyObject(@Nonnull final Job job) throws Exception {
        this.job = job;

        this.jobId = job.getFullName();

        this.pullRequestHead = GitHubHelper.getPullRequest(job);
        this.base = GitHubHelper.getRepositoryId(job);
        this.head = RepositoryId.create(pullRequestHead.getSourceOwner(), pullRequestHead.getSourceRepo());

        // fetch and cache the pull request
        this.pullRequest = getPullRequestService().getPullRequest(base, pullRequestHead.getNumber());
    }

    private Job getJob() {
        if (job == null) {
            job = Jenkins.get().getItemByFullName(jobId, Job.class);
            if (job == null) {
                throw new IllegalStateException("Unable to find Job: " + jobId);
            }
        }
        return job;
    }

    private ExtendedGitHubClient getGitHubClient() {
        if (gitHubClient == null) {
            gitHubClient = GitHubHelper.getGitHubClient(getJob());
        }
        return gitHubClient;
    }

    private ExtendedPullRequestService getPullRequestService() {
        if (pullRequestService == null) {
            pullRequestService = new ExtendedPullRequestService(getGitHubClient());
        }
        return pullRequestService;
    }

    private ExtendedIssueService getIssueService() {
        if (issueService == null) {
            issueService = new ExtendedIssueService(getGitHubClient());
        }
        return issueService;
    }

    private ExtendedCommitService getCommitService() {
        if (commitService == null) {
            commitService = new ExtendedCommitService(getGitHubClient());
        }
        return commitService;
    }

    private ExtendedMilestoneService getMilestoneService() {
        if (milestoneService == null) {
            milestoneService = new ExtendedMilestoneService(getGitHubClient());
        }
        return milestoneService;
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
    public boolean isDraft() {
        return pullRequest.isDraft();
    }

    @Whitelisted
    public MilestoneGroovyObject getMilestone() {
        return Optional.ofNullable(pullRequest.getMilestone())
                .map(Milestone::getNumber)
                .map(m -> getMilestoneService().getMilestone(base, m))
                .map(milestone -> new MilestoneGroovyObject(jobId, milestone))
                .orElse(null);
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
    public String getMergeableState() {
        return pullRequest.getMergeableState();
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
                .stream(getPullRequestService().pageRequestedReviewers(base, pullRequest.getNumber())
                        .spliterator(), false)
                .flatMap(Collection::stream)
                .map(User::getLogin);

        return stream::iterator;
    }

    @Whitelisted
    public Iterable<String> getRequestedTeamReviewers() {
        Stream<String> stream = StreamSupport
                .stream(getPullRequestService().pageRequestedTeamReviewers(base, pullRequest.getNumber())
                        .spliterator(), false)
                .flatMap(Collection::stream)
                .map(Team::getName);

        return stream::iterator;
    }

    @Whitelisted
    public Iterable<ReviewGroovyObject> getReviews() {
        Stream<ReviewGroovyObject> stream = StreamSupport
            .stream(getPullRequestService().pageReviews(base, pullRequest.getNumber())
                    .spliterator(), false)
            .flatMap(Collection::stream)
            .map(ReviewGroovyObject::new);

        return stream::iterator;
    }

    @Whitelisted
    public Iterable<CommitStatusGroovyObject> getStatuses() {
        try {
            return getCommitService().getStatuses(base, pullRequest.getHead().getSha())
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
                .stream(getIssueService().getLabels(base, pullRequest.getNumber())
                        .spliterator(), false)
                .flatMap(Collection::stream)
                .map(Label::getName);

        return stream::iterator;
    }

    @Whitelisted
    public Iterable<String> getAssignees() {
        return pullRequest.getAssignees()
                .stream()
                .map(User::getLogin)
                .collect(toList());
    }

    @Whitelisted
    public Iterable<CommitGroovyObject> getCommits() {
        try {
            Stream<CommitGroovyObject> steam = getPullRequestService()
                    .getCommits(base, pullRequestHead.getNumber())
                    .stream()
                    .map(c -> new CommitGroovyObject(jobId, c, getCommitService(), base));

            return steam::iterator;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public Iterable<IssueCommentGroovyObject> getComments() {
        try {
            Stream<IssueCommentGroovyObject> stream = getIssueService()
                    .getComments(base, pullRequestHead.getNumber())
                    .stream()
                    .map(c -> new IssueCommentGroovyObject(jobId, c, base, getIssueService()));

            return stream::iterator;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public Iterable<ReviewCommentGroovyObject> getReviewComments() {
        Stream<ReviewCommentGroovyObject> stream = StreamSupport
                .stream(getPullRequestService().pageComments2(base,
                        pullRequestHead.getNumber()).spliterator(), false)
                .flatMap(Collection::stream)
                .map(c -> new ReviewCommentGroovyObject(jobId, base, c, getCommitService()));
        return stream::iterator;
    }

    @Whitelisted
    public Iterable<CommitFileGroovyObject> getFiles() {
        try {
            return getPullRequestService().getFiles(base, pullRequestHead.getNumber())
                    .stream()
                    .map(CommitFileGroovyObject::new)
                    .collect(toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setMilestone(final int milestoneNumber) {
        pullRequest.setMilestone(
                getIssueService().setMilestone(base, pullRequest.getNumber(), milestoneNumber)
                        .getMilestone());
    }

    @Whitelisted
    public void setMilestone(final MilestoneGroovyObject milestone) {
        if (milestone == null) {
            // call setMilestone because the caller might not have the right permissions to remove
            // the milestone and it'll return the current milestone.
            pullRequest.setMilestone(
                    getIssueService().setMilestone(base, pullRequest.getNumber(), null)
                        .getMilestone());
        } else {
            setMilestone(milestone.getNumber());
        }
    }

    @Whitelisted
    public void setLocked(final boolean locked) {
        try {
            if (locked) {
                getIssueService().lockIssue(base, pullRequest.getNumber());
            } else {
                getIssueService().unlockIssue(base, pullRequest.getNumber());
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
        pullRequest = getPullRequestService().editPullRequest(base, edit);
    }

    @Whitelisted
    public void setBody(final String body) {
        Objects.requireNonNull(body, "body cannot be null");

        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setBody(body);
        pullRequest = getPullRequestService().editPullRequest(base, edit);
    }

    @Whitelisted
    public void setState(final String state) {
        Objects.requireNonNull(state, "state cannot be null");

        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setState(state);
        pullRequest = getPullRequestService().editPullRequest(base, edit);
    }

    @Whitelisted
    public void setBase(final String newBase) {
        Objects.requireNonNull(newBase, "base cannot be null");

        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setBase(new PullRequestMarker().setRef(newBase));
        pullRequest = getPullRequestService().editPullRequest(base, edit);
    }

    @Whitelisted
    public void setMaintainerCanModify(final boolean value) {
        ExtendedPullRequest edit = new ExtendedPullRequest();
        edit.setNumber(pullRequest.getNumber());
        edit.setMaintainerCanModify(value);
        pullRequest = getPullRequestService().editPullRequest(base, edit);
    }

    @Whitelisted
    public void setLabels(final List<String> labels) {
        try {
            getIssueService().setLabels(base, pullRequest.getNumber(),
                    Optional.ofNullable(labels).orElseGet(Collections::emptyList));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void createReviewRequest(final String reviewer) {
        Objects.requireNonNull(reviewer, "reviewer cannot be null");
        createReviewRequests(Collections.singletonList(reviewer));
    }

    @Whitelisted
    public void createReviewRequests(final List<String> reviewers) {
        Objects.requireNonNull(reviewers, "reviewers cannot be null");
        try {
            getPullRequestService().createReviewRequests(base, pullRequest.getNumber(), reviewers, null);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void deleteReviewRequest(final String reviewer) {
        Objects.requireNonNull(reviewer, "reviewer cannot be null");
        deleteReviewRequests(Collections.singletonList(reviewer));
    }

    @Whitelisted
    public void deleteReviewRequests(final List<String> reviewers) {
        Objects.requireNonNull(reviewers, "reviewers cannot be null");
        try {
            getPullRequestService().deleteReviewRequests(base, pullRequest.getNumber(), reviewers, null);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Whitelisted
    public void createTeamReviewRequest(final String team) {
        Objects.requireNonNull(team, "team cannot be null");
        createTeamReviewRequests(Collections.singletonList(team));
    }

    @Whitelisted
    public void createTeamReviewRequests(final List<String> teams) {
        Objects.requireNonNull(teams, "teams cannot be null");
        try {
            getPullRequestService().createReviewRequests(base, pullRequest.getNumber(), null, teams);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void deleteTeamReviewRequest(final String team) {
        Objects.requireNonNull(team, "team cannot be null");
        deleteTeamReviewRequests(Collections.singletonList(team));
    }

    @Whitelisted
    public void deleteTeamReviewRequests(final List<String> teams) {
        Objects.requireNonNull(teams, "teams cannot be null");
        try {
            getPullRequestService().deleteReviewRequests(base, pullRequest.getNumber(), null, teams);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void addLabel(final String label) {
        addLabels(Collections.singletonList(label));
    }

    @Whitelisted
    public void addLabels(final List<String> labels) {
        Objects.requireNonNull(labels, "labels is a required argument");
        try {
            getIssueService().addLabels(base, pullRequest.getNumber(), labels);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void removeLabel(final String label) {
        Objects.requireNonNull(label, "label is a required argument");
        try {
            getIssueService().removeLabel(base, pullRequest.getNumber(), label);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void addAssignees(final List<String> assignees) {
        Objects.requireNonNull(assignees, "assignees is a required argument");
        try {
            getIssueService().addAssignees(base, pullRequest.getNumber(), assignees);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void setAssignees(final List<String> assignees) {
        Objects.requireNonNull(assignees, "assignees is a required argument");
        try {
            getIssueService().setAssignees(base, pullRequest.getNumber(), assignees);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void removeAssignees(final List<String> assignees) {
        Objects.requireNonNull(assignees, "assignees is a required argument");
        try {
            getIssueService().removeAssignees(base, pullRequest.getNumber(), assignees);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void review(final String event) {
        review(null, event, null);
    }

    @Whitelisted
    public void review(final String event, final String body) {
        review(null, event, body);
    }

    @Whitelisted
    public void review(final Map<String, Object> params) {
        review(params.get("commitId") != null ? params.get("commitId").toString() : null,
               params.get("event") != null ? params.get("event").toString() : null,
               params.get("body") != null ? params.get("body").toString() : null);
    }

    @Whitelisted
    public void review(final String commitId, final String event, final String body) {
        if (event != null && (event.equals("REQUEST_CHANGES") || event.equals("COMMENT"))) {
            Objects.requireNonNull(body, "body is a required argument when event equals REQUEST_CHANGES or COMMENT");
        }

        try {
            pullRequestService.createReview(base, pullRequestHead.getNumber(), commitId, event, body);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public CommitStatusGroovyObject createStatus(final Map<String, Object> params) {
        Objects.requireNonNull(params.get("status"), "status is a required argument");

        return createStatus(params.get("status").toString(),
                            params.get("context") != null ? params.get("context").toString() : null,
                            params.get("description") != null ? params.get("description").toString() : null,
                            params.get("targetUrl") != null ? params.get("targetUrl").toString() : null);
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
                    getCommitService().createStatus(base, pullRequest.getHead().getSha(), commitStatus));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public ReviewCommentGroovyObject reviewComment(final String commitId,
                                                   final String path,
                                                   final int line,
                                                   final String body) {
        Objects.requireNonNull(commitId, "commitId is a required argument");
        Objects.requireNonNull(path, "path is a required argument");
        Objects.requireNonNull(body, "body is a required argument");

        ExtendedCommitComment comment = new ExtendedCommitComment();
        comment.setCommitId(commitId);
        comment.setPath(path);
        comment.setLine(line);
        comment.setBody(body);
        try {
            return new ReviewCommentGroovyObject(
                    jobId,
                    base,
                    getPullRequestService().createComment2(base, pullRequestHead.getNumber(), comment),
                    getCommitService());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public ReviewCommentGroovyObject replyToReviewComment(final long commentId, final String body) {
        Objects.requireNonNull(body, "body is a required argument");
        try {
            return new ReviewCommentGroovyObject(
                    jobId,
                    base,
                    getPullRequestService().replyToComment2(base, pullRequestHead.getNumber(), (int) commentId, body),
                    getCommitService());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void deleteReviewComment(final long commentId) {
        try {
            getPullRequestService().deleteComment(base, commentId);
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
            return new ReviewCommentGroovyObject(jobId, base, getPullRequestService().editComment2(base, comment), getCommitService());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public IssueCommentGroovyObject comment(final String body) {
        Objects.requireNonNull(body, "body is a required argument");

        try {
            return new IssueCommentGroovyObject(
                    jobId,
                    getIssueService().createComment(base, pullRequestHead.getNumber(), body),
                    base,
                    getIssueService());
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
                    jobId,
                    getIssueService().editComment(base, comment),
                    base,
                    getIssueService());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public void deleteComment(final long commentId) {
        try {
            getIssueService().deleteComment(base, commentId);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public String merge(final Map<String, Object> params) {
        return merge(Optional.ofNullable(params.get("commitTitle")).map(Object::toString).orElse(null),
                     Optional.ofNullable(params.get("commitMessage")).map(Object::toString).orElse(null),
                     Optional.ofNullable(params.get("sha")).map(Object::toString).orElse(null),
                     Optional.ofNullable(params.get("mergeMethod")).map(Object::toString).orElse(null));
    }

    @Whitelisted
    public String merge(final String commitMessage) {
        return merge(null, commitMessage, null, null);
    }

    @Whitelisted
    public String merge(final String commitTitle,
                        final String commitMessage,
                        final String sha,
                        final String mergeMethod) {
        try {
            ExtendedMergeStatus status = getPullRequestService().merge(base,
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
        pullRequest = getPullRequestService().getPullRequest(base, pullRequest.getNumber());
    }

    @Whitelisted
    public void setCredentials(final String userName, final String password) {
        getGitHubClient().setCredentials(userName, password);
    }

    @Whitelisted
    public void deleteBranch(){
        try {
            getPullRequestService().deleteBranch(base,pullRequest.getBranchReference());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
