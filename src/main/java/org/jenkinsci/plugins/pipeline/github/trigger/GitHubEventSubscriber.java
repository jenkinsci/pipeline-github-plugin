package org.jenkinsci.plugins.pipeline.github.trigger;

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
            default:
                // no-op
        }
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
                    .filter(t -> triggerMatches(t, issueCommentEvent.getComment(), job))
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
        return GitHubHelper.getCollaborators(job)
                .stream()
                .filter(commentAuthor::equals)
                .findAny()
                .map(a -> Boolean.TRUE)
                .orElse(Boolean.FALSE);
    }

    private boolean triggerMatches(final IssueCommentTrigger trigger,
                                   final GHIssueComment issueComment,
                                   final WorkflowJob job) {
        if (trigger.matchesComment(issueComment.getBody())) {
            LOG.debug("Job: {}, IssueComment: {} matched Pattern: {}",
                    job.getFullName(), issueComment, trigger.getCommentPattern());
            return true;
        } else {
            LOG.debug("Job: {}, IssueComment: {}, the comment did not match Pattern: {}",
                    job.getFullName(), issueComment, trigger.getCommentPattern());
        }
        return false;
    }

    @Override
    protected Set<GHEvent> events() {
        final Set<GHEvent> events = new HashSet<>();
//        events.add(GHEvent.PULL_REQUEST_REVIEW_COMMENT);
//        events.add(GHEvent.COMMIT_COMMENT);
        events.add(GHEvent.ISSUE_COMMENT);
        return Collections.unmodifiableSet(events);
    }
}
