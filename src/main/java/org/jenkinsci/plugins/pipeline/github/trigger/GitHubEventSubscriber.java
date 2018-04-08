package org.jenkinsci.plugins.pipeline.github.trigger;

import hudson.Extension;
import hudson.model.Item;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
        GHEventPayload.IssueComment issueCommentEvent;

        // we only care about created or updated events
        switch (event.getType()) {
            case CREATED:
            case UPDATED:
                break;
            default:
                return;
        }

        // decode payload
        try {
            issueCommentEvent = GitHub.offline()
                    .parseEventPayload(new StringReader(event.getPayload()), GHEventPayload.IssueComment.class);
        } catch (final IOException e) {
            LOG.error("Unable to parse the payload of GHSubscriberEvent: {}", event, e);
            return;
        }

        // create key for this comment's PR
        String key = String.format("%s/%s/%d",
                issueCommentEvent.getRepository().getOwnerName(),
                issueCommentEvent.getRepository().getName(),
                issueCommentEvent.getIssue().getNumber());

        // lookup trigger
        IssueCommentTrigger.DescriptorImpl triggerDescriptor =
                (IssueCommentTrigger.DescriptorImpl) Jenkins.getInstance()
                .getDescriptor(IssueCommentTrigger.class);

        if (triggerDescriptor == null) {
            LOG.error("Unable to find the IssueComment Trigger, this shouldn't happen.");
            return;
        }

        // lookup job
        WorkflowJob job = triggerDescriptor.getJob(key);

        if (job == null) {
            LOG.debug("No job found matching key: {}", key);
        } else {
            Optional<IssueCommentTrigger> matchingTrigger = job.getTriggersJobProperty()
                    .getTriggers()
                    .stream()
                    .filter(t -> t instanceof IssueCommentTrigger)
                    .map(IssueCommentTrigger.class::cast)
                    .filter(t -> triggerMatches(t, issueCommentEvent.getComment(), job))
                    .findAny();

            if (matchingTrigger.isPresent()) {
                String commentAuthor = issueCommentEvent.getComment().getUserName();
                boolean authorized = isAuthorized(job, commentAuthor);

                if (authorized) {
                    job.scheduleBuild(
                            new IssueCommentCause(
                                    issueCommentEvent.getComment().getUserName(),
                                    issueCommentEvent.getComment().getBody(),
                                    matchingTrigger.get().getCommentPattern()));
                    LOG.info("Job: {} triggered by IssueComment: {}",
                            job.getFullName(), issueCommentEvent.getComment());
                } else {
                    LOG.warn("Job: {}, IssueComment: {}, Comment Author: {} is not a collaborator, " +
                             "and is therefore not authorized to trigger a build.",
                            job.getFullName(),
                            issueCommentEvent.getComment(),
                            commentAuthor);
                }
            } else {
                LOG.debug("Job: {}, IssueComment: {}, No matching triggers could be found for this comment.",
                        job.getFullName(), issueCommentEvent.getComment());
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
        Set<GHEvent> events = new HashSet<>();
//        events.add(GHEvent.PULL_REQUEST_REVIEW_COMMENT);
//        events.add(GHEvent.COMMIT_COMMENT);
        events.add(GHEvent.ISSUE_COMMENT);
        return Collections.unmodifiableSet(events);
    }
}
