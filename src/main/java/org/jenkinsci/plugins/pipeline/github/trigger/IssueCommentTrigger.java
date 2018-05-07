package org.jenkinsci.plugins.pipeline.github.trigger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * An IssueCommentTrigger, to be used from pipeline scripts only.
 *
 * This trigger will not show up on a jobs configuration page.
 *
 * @author Aaron Whiteside
 * @see org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty
 */
public class IssueCommentTrigger extends Trigger<WorkflowJob> {
    private static final Logger LOG = LoggerFactory.getLogger(IssueCommentTrigger.class);

    private final String commentPattern;

    @DataBoundConstructor
    public IssueCommentTrigger(@Nonnull final String commentPattern) {
        this.commentPattern = commentPattern;
    }

    @Override
    public void start(final WorkflowJob project, final boolean newInstance) {
        super.start(project, newInstance);
        // we only care about pull requests
        if (SCMHead.HeadByItem.findHead(project) instanceof PullRequestSCMHead) {
            DescriptorImpl.jobs.put(getKey(project), project);
        }
    }

    @Override
    public void stop() {
        if (SCMHead.HeadByItem.findHead(job) instanceof PullRequestSCMHead) {
            DescriptorImpl.jobs.put(getKey(job), job);
        }
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private String getKey(final WorkflowJob project) {
        GitHubSCMSource scmSource = (GitHubSCMSource) SCMSource.SourceByItem.findSource(project);
        PullRequestSCMHead scmHead = (PullRequestSCMHead) SCMHead.HeadByItem.findHead(project);

        return String.format("%s/%s/%d",
                scmSource.getRepoOwner(),
                scmSource.getRepository(),
                scmHead.getNumber());
    }

    public String getCommentPattern() {
        return commentPattern;
    }

    boolean matchesComment(final String comment) {
        return Pattern.compile(commentPattern)
                .matcher(comment)
                .matches();
    }

    @Symbol("issueCommentTrigger")
    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private transient static final Map<String, WorkflowJob> jobs = new ConcurrentHashMap<>();

        @Override
        public boolean isApplicable(final Item item) {
            return false; // this is not configurable from the ui.
        }

        public WorkflowJob getJob(final String key) {
            return jobs.get(key);
        }
    }

}
