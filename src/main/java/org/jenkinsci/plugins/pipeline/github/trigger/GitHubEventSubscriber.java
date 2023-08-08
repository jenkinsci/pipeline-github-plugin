package org.jenkinsci.plugins.pipeline.github.trigger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.pipeline.github.GitHubHelper;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listens for GitHub events.
 *
 * Currently only handles IssueComment events.
 *
 * @author Aaron Whiteside
 */
@Extension
public class GitHubEventSubscriber extends GHEventsSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(GHEventsSubscriber.class);

    @Override
    protected boolean isApplicable(@Nullable final Item project) {
        if (project != null) {
            if (project instanceof SCMSourceOwner) {
                SCMSourceOwner owner = (SCMSourceOwner) project;
                for (final SCMSource source : owner.getSCMSources()) {
                    if (source instanceof GitHubSCMSource) {
                        return true;
                    }
                }
            }
            if (project.getParent() instanceof SCMSourceOwner) {
                SCMSourceOwner owner = (SCMSourceOwner) project.getParent();
                for (final SCMSource source : owner.getSCMSources()) {
                    if (source instanceof GitHubSCMSource) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void onEvent(final GHSubscriberEvent event) {
        LOG.debug("Received event: {}", event.getGHEvent());

        switch (event.getGHEvent()) {
            case ISSUE_COMMENT:
                handleIssueComment(event);
                break;
            case PULL_REQUEST:
                handleLabelEvent(event);
                break;
            case PULL_REQUEST_REVIEW:
                handlePullRequestReview(event);
                break;
            default:
                // no-op
        }
    }

    private void handleLabelEvent(final GHSubscriberEvent event) {
        switch (event.getType()){
            case CREATED:
            case UPDATED:
                break;
            default:
                return;
        }
        final GHEventPayload.PullRequest prEvent;
        try {
            prEvent = GitHub.offline()
                    .parseEventPayload(new StringReader(event.getPayload()), GHEventPayload.PullRequest.class);
        } catch (final IOException e) {
            LOG.error("Unable to parse the payload of GHSubscriberEvent: {}", event, e);
            return;
        }
        switch (prEvent.getAction()) {
            case "labeled":
                break;
            default:
                LOG.debug("Ignoring PR: {} event with Action: {}",
                        prEvent.getNumber(), prEvent.getAction());
                return;
        }
        // create key for this comment's PR
        final String key = String.format("%s/%s/%d",
                prEvent.getRepository().getOwnerName(),
                prEvent.getRepository().getName(),
                prEvent.getNumber());
                // lookup trigger
        final LabelAddedTrigger.DescriptorImpl triggerDescriptor = (LabelAddedTrigger.DescriptorImpl) Jenkins.get()
                .getDescriptor(LabelAddedTrigger.class);
        if (triggerDescriptor == null) {
            LOG.error("Unable to find the LabelAddedTrigger Trigger, this shouldn't happen.");
            return;
        }
        // create values for the action if a new job is triggered afterward
        ArrayList<ParameterValue> values = new ArrayList<ParameterValue>();
        String labelName = prEvent.getLabel().getName();
        LOG.info("Added label {} to repo {}", labelName, key);
        values.add(new StringParameterValue("GITHUB_LABEL_ADDED", String.valueOf(labelName)));
        // lookup jobs
        for (final WorkflowJob job : triggerDescriptor.getJobs(key)) {
            // find triggers
            final List<LabelAddedTrigger> matchingTriggers = job.getTriggersJobProperty()
                    .getTriggers()
                    .stream()
                    .filter(LabelAddedTrigger.class::isInstance)
                    .map(LabelAddedTrigger.class::cast)
                    .filter(labelTrigger -> labelAddedMatches(labelTrigger, labelName, job))
                    .collect(Collectors.toList());

            if (matchingTriggers.size() == 0) {
                LOG.debug("No labels match the ones attached to the trigger");
                break;
            }
            job.scheduleBuild2(
                Jenkins.getInstance().getQuietPeriod(),
                new CauseAction(
                    new LabelAddedCause(
                        prEvent.getSender().getLogin(),
                        labelName
                    )
                ),
                new GitHubEnvironmentVariablesAction(values)
            );
        }
    }
    private boolean labelAddedMatches(final LabelAddedTrigger trigger,final String labelName,final WorkflowJob job ){
        boolean matches = trigger.matchesLabel(labelName);
        if (matches) {
            LOG.debug("Job: {}, labelName: {}, the label did matched the triggerLabel: {}",
                job.getFullName(), labelName, trigger.getLabelTrigger());
            return true;
        } 
        LOG.debug("Job: {}, labelName: {}, the label did not matched the triggerLabel: {}",
                job.getFullName(), labelName, trigger.getLabelTrigger());
        
        return false;
    }
    private void handleIssueComment(final GHSubscriberEvent event) {
        // we only care about created or updated events
        switch (event.getType()) {
            case CREATED:
            case UPDATED:
                break;
            default:
                return;
        }

        // decode payload
        final GHEventPayload.IssueComment issueCommentEvent;
        try {
            issueCommentEvent = GitHub.offline()
                    .parseEventPayload(new StringReader(event.getPayload()), GHEventPayload.IssueComment.class);
        } catch (final IOException e) {
            LOG.error("Unable to parse the payload of GHSubscriberEvent: {}", event, e);
            return;
        }

        switch (issueCommentEvent.getAction()) {
            case "created":
            case "edited":
                break;
            default:
                LOG.debug("Ignoring IssueComment: {} with Action: {}",
                        issueCommentEvent.getComment(), issueCommentEvent.getAction());
                return;
        }

        // create key for this comment's PR
        final String key = String.format("%s/%s/%d",
                issueCommentEvent.getRepository().getOwnerName(),
                issueCommentEvent.getRepository().getName(),
                issueCommentEvent.getIssue().getNumber());

        // lookup trigger
        final IssueCommentTrigger.DescriptorImpl triggerDescriptor = (IssueCommentTrigger.DescriptorImpl) Jenkins.get()
                .getDescriptor(IssueCommentTrigger.class);

        if (triggerDescriptor == null) {
            LOG.error("Unable to find the IssueComment Trigger, this shouldn't happen.");
            return;
        }

        // create values for the action if a new job is triggered afterward
        ArrayList<ParameterValue> values = new ArrayList<ParameterValue>();
        values.add(new StringParameterValue("GITHUB_COMMENT", String.valueOf(issueCommentEvent.getComment().getBody())));
        values.add(new StringParameterValue("GITHUB_COMMENT_AUTHOR", String.valueOf(issueCommentEvent.getComment().getUserName())));

        // lookup jobs
        for (final WorkflowJob job : triggerDescriptor.getJobs(key)) {
            // find triggers
            final List<IssueCommentTrigger> matchingTriggers = job.getTriggersJobProperty()
                    .getTriggers()
                    .stream()
                    .filter(IssueCommentTrigger.class::isInstance)
                    .map(IssueCommentTrigger.class::cast)
                    .filter(t -> commentTriggerMatches(t, issueCommentEvent.getComment(), job))
                    .collect(Collectors.toList());

            // check if they have authorization
            for (final IssueCommentTrigger matchingTrigger : matchingTriggers) {
                String commentAuthor = issueCommentEvent.getComment().getUserName();
                boolean authorized = isAuthorized(job, commentAuthor);

                if (authorized) {
                    job.scheduleBuild2(
                            Jenkins.getInstance().getQuietPeriod(),
                            new CauseAction(new IssueCommentCause(
                                        issueCommentEvent.getComment().getUserName(),
                                        issueCommentEvent.getComment().getBody(),
                                        matchingTrigger.getCommentPattern())),
                            new GitHubEnvironmentVariablesAction(values));

                    LOG.info("Job: {} triggered by IssueComment: {}",
                            job.getFullName(), issueCommentEvent.getComment());
                } else {
                    LOG.warn("Job: {}, IssueComment: {}, Comment Author: {} is not a collaborator, " +
                                    "and is therefore not authorized to trigger a build.",
                            job.getFullName(),
                            issueCommentEvent.getComment(),
                            commentAuthor);
                }
            }
        }
    }

    private boolean isAuthorized(final WorkflowJob job, final String commentAuthor) {
        return GitHubHelper.isAuthorized(job, commentAuthor);
    }

    private boolean commentTriggerMatches(final IssueCommentTrigger trigger,
                                   final GHIssueComment issueComment,
                                   final WorkflowJob job) {
        if (trigger.matchesComment(issueComment.getBody())) {
            LOG.debug("Job: {}, IssueComment: {} matched Pattern: {}",
                    job.getFullName(), issueComment, trigger.getCommentPattern());
            return true;
        }
        LOG.debug("Job: {}, IssueComment: {}, the comment did not match Pattern: {}",
                job.getFullName(), issueComment, trigger.getCommentPattern());
    
        return false;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private void handlePullRequestReview(final GHSubscriberEvent event) {
        // we only care about created or updated events
        switch (event.getType()) {
            case CREATED:
            case UPDATED:
            // case: SUBMITTED:
                break;
            default:
                return;
        }
        // decode payload
        final GHEventPayload.PullRequestReview pullRequestReview;
        try {
            pullRequestReview = GitHub.offline()
                    .parseEventPayload(new StringReader(event.getPayload()), GHEventPayload.PullRequestReview.class);
        } catch (final IOException e) {
            LOG.error("Unable to parse the payload of GHSubscriberEvent: {}", event, e);
            return;
        }

        switch (pullRequestReview.getAction()) {
            case "submitted":
                break;
            default:
                LOG.debug("Ignoring pullRequestReview: {} with Action: {}",
                pullRequestReview.getReview(), pullRequestReview.getAction());
                return;
        }

        final String key = String.format("%s/%s/%d",
                pullRequestReview.getRepository().getOwnerName(),
                pullRequestReview.getRepository().getName(),
                pullRequestReview.getPullRequest().getNumber());

        // lookup trigger
        final PullRequestReviewTrigger.DescriptorImpl triggerDescriptor = (PullRequestReviewTrigger.DescriptorImpl) Jenkins.get()
                .getDescriptor(PullRequestReviewTrigger.class);

        if (triggerDescriptor == null) {
            LOG.error("Unable to find the PullRequestReview Trigger, this shouldn't happen.");
            return;
        }
        String reviewer = pullRequestReview.getSender().getLogin();

        // create values for the action if a new job is triggered afterward
        ArrayList<ParameterValue> reviewEnvVars = new ArrayList<ParameterValue>();
        reviewEnvVars.add(new StringParameterValue("GITHUB_REVIEW_COMMENT", String.valueOf(pullRequestReview.getReview().getBody())));
        reviewEnvVars.add(new StringParameterValue("GITHUB_REVIEW_AUTHOR", reviewer));
        reviewEnvVars.add(new StringParameterValue("GITHUB_REVIEW_STATE", pullRequestReview.getReview().getState().name()));


        for (final WorkflowJob job : triggerDescriptor.getJobs(key)) {
            // find triggers
            final List<PullRequestReviewTrigger> matchingTriggers = job.getTriggersJobProperty()
                    .getTriggers()
                    .stream()
                    .filter(PullRequestReviewTrigger.class::isInstance)
                    .map(PullRequestReviewTrigger.class::cast)
                    .filter(t -> commentTriggerMatches(t, pullRequestReview.getReview(), job))
                    .collect(Collectors.toList());

            // check if they have authorization
            for (final PullRequestReviewTrigger matchingTrigger : matchingTriggers) {
                boolean authorized = isAuthorized(job, reviewer);

                if (authorized) {
                    job.scheduleBuild2(
                            Jenkins.get().getQuietPeriod(),
                            new CauseAction(new PullRequestReviewCause(
                                        reviewer,
                                        pullRequestReview.getReview().getState().name().toLowerCase(),
                                        pullRequestReview.getReview().getBody(),
                                        matchingTrigger.getReviewStates())),
                            new GitHubEnvironmentVariablesAction(reviewEnvVars));

                    LOG.info("Job: {} triggered by PullRequestReview: {}",
                            job.getFullName(), pullRequestReview.getReview());
                } else {
                    LOG.warn("Job: {}, PullRequestReview: {}, Reviewer: {} is not a collaborator, " +
                                    "and is therefore not authorized to trigger a build.",
                            job.getFullName(),
                            pullRequestReview.getReview(),
                            reviewer);
                }
            }
        }
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private boolean commentTriggerMatches(final PullRequestReviewTrigger trigger,
                                   final GHPullRequestReview review,
                                   final WorkflowJob job) {
        if (trigger.matches(review.getState().name().toLowerCase())) {
            LOG.debug("Job: {}, PullRequestReview: {} matched one of the states: {}",
                    job.getFullName(), review, trigger.getReviewStates());
            return true;
        } else {
            LOG.debug("Job: {}, PullRequestReview: {}, state did not match the states: {}",
                    job.getFullName(), review, trigger.getReviewStates());
        }
        return false;
    }

    @Override
    protected Set<GHEvent> events() {
        final Set<GHEvent> events = new HashSet<>();
//        events.add(GHEvent.PULL_REQUEST_REVIEW_COMMENT);
//        events.add(GHEvent.COMMIT_COMMENT);
        events.add(GHEvent.ISSUE_COMMENT);
        events.add(GHEvent.PULL_REQUEST);
        events.add(GHEvent.PULL_REQUEST_REVIEW);
        return Collections.unmodifiableSet(events);
    }
}
