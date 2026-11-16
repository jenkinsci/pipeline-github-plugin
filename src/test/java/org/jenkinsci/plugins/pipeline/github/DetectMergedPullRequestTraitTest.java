package org.jenkinsci.plugins.pipeline.github;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DetectMergedPullRequestTraitTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void availableInGitHubSourceBehaviors() {
        DetectMergedPullRequestTrait.DescriptorImpl trait =
                r.jenkins.getDescriptorByType(DetectMergedPullRequestTrait.DescriptorImpl.class);
        assertNotNull(trait, "The trait must be registered as a Jenkins extension");

        GitHubSCMSource.DescriptorImpl source =
                r.jenkins.getDescriptorByType(GitHubSCMSource.DescriptorImpl.class);
        assertTrue(source.getTraitsDescriptorLists().stream().anyMatch(group -> group.contains(trait)));

        GitHubSCMNavigator.DescriptorImpl navigator =
                r.jenkins.getDescriptorByType(GitHubSCMNavigator.DescriptorImpl.class);
        assertTrue(navigator.getTraitsDescriptorLists().stream().anyMatch(group -> group.contains(trait)));
    }
}
