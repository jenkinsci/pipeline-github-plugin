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
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
/**
 * An PullRequestApprovalTrigger, to be used from pipeline scripts only.
 *
 * This trigger will not show up on a jobs configuration page.
 *
 * @author Aaron Walker
 * @see org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty
 */
public class PullRequestReviewTrigger  extends Trigger<WorkflowJob> {
  private static final Logger LOG = LoggerFactory.getLogger(PullRequestReviewTrigger.class);

  private String[] reviewStates = null;

  @DataBoundConstructor
  public PullRequestReviewTrigger() {}

  public String[] getReviewStates() {
      return Arrays.copyOf(reviewStates, reviewStates.length);
  }

  @DataBoundSetter
  public void setReviewStates(@Nonnull final String [] reviewStates) {
      this.reviewStates = Arrays.copyOf(reviewStates, reviewStates.length);
  }

  @Override
  public void start(final WorkflowJob project, final boolean newInstance) {
      super.start(project, newInstance);
      // we only care about pull requests
      if (SCMHead.HeadByItem.findHead(project) instanceof PullRequestSCMHead) {
          DescriptorImpl.jobs
                  .computeIfAbsent(getKey(project), key -> new HashSet<>())
                  .add(project);
      }
  }

  @Override
  public void stop() {
      if (SCMHead.HeadByItem.findHead(job) instanceof PullRequestSCMHead) {
          DescriptorImpl.jobs.getOrDefault(getKey(job), Collections.emptySet())
                  .remove(job);
      }
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private String getKey(final WorkflowJob project) {
      final GitHubSCMSource scmSource = (GitHubSCMSource) SCMSource.SourceByItem.findSource(project);
      final PullRequestSCMHead scmHead = (PullRequestSCMHead) SCMHead.HeadByItem.findHead(project);

      return String.format("%s/%s/%d",
              scmSource.getRepoOwner(),
              scmSource.getRepository(),
              scmHead.getNumber()).toLowerCase();
  }

  boolean matches(final String reviewState) {
    if(reviewStates == null) {
        return true; //defaults to trigger on any review state
    } else {
        List<String> list = Arrays.asList(reviewStates);
        return list.contains(reviewState);
    }
  }

  @Symbol("pullRequestReview")
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
