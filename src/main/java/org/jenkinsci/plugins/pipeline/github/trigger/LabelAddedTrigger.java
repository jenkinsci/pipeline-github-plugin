package org.jenkinsci.plugins.pipeline.github.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * An LabelAddedTrigger, to be used from pipeline scripts only.
 *
 * This trigger will not show up on a jobs configuration page.
 *
 * @author Joaquín Fernández Campo
 * @see org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty
 */
public class LabelAddedTrigger extends Trigger<WorkflowJob> {
    private static final Logger LOG = LoggerFactory.getLogger(LabelAddedTrigger.class);

    private final String labelTriggerPattern;

    @DataBoundConstructor
    public LabelAddedTrigger(@NonNull final String labelTriggerPattern) {
        this.labelTriggerPattern = labelTriggerPattern;
    }

    @Override
    public void start(final WorkflowJob project, final boolean newInstance) {
        super.start(project, newInstance);
        // we only care about pull requests
        if (SCMHead.HeadByItem.findHead(project) instanceof PullRequestSCMHead) {
            final String key = getKey(project);
            if (key != null) {
                DescriptorImpl.jobs
                        .computeIfAbsent(key, k -> new HashSet<>())
                        .add(project);
            }
        }
    }

    @Override
    public void stop() {
        if (SCMHead.HeadByItem.findHead(job) instanceof PullRequestSCMHead) {
            final String key = getKey(job);
            if (key != null) {
                DescriptorImpl.jobs.getOrDefault(key, Collections.emptySet())
                        .remove(job);
            }
        }
    }

    private String getKey(final WorkflowJob project) {
        final SCMSource scmSource = SCMSource.SourceByItem.findSource(project);
        if (!(scmSource instanceof GitHubSCMSource)) {
            LOG.warn("Job: {} has a non-GitHub SCM source: {}, skipping trigger registration",
                    project.getFullName(), scmSource != null ? scmSource.getClass().getName() : "null");
            return null;
        }
        final SCMHead scmHead = SCMHead.HeadByItem.findHead(project);
        if (!(scmHead instanceof PullRequestSCMHead)) {
            return null;
        }

        return String.format("%s/%s/%d",
                ((GitHubSCMSource) scmSource).getRepoOwner(),
                ((GitHubSCMSource) scmSource).getRepository(),
                ((PullRequestSCMHead) scmHead).getNumber()).toLowerCase();
    }

    public String getLabelTrigger() {
        return labelTriggerPattern;
    }
    boolean matchesLabel(final String label) {
        return Pattern.compile(labelTriggerPattern)
                .matcher(label)
                .matches();
    }

    @Symbol("labelAddedTrigger")
    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private transient static final Map<String, Set<WorkflowJob>> jobs = new ConcurrentHashMap<>();

        @Override
        public boolean isApplicable(final Item item) {
            return false; // this is not configurable from the ui.
        }

        public Set<WorkflowJob> getJobs(final String key) {
            return jobs.getOrDefault(key.toLowerCase(), Collections.emptySet());
        }
    }

}
