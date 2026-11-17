package org.jenkinsci.plugins.pipeline.github;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GitHubPipelineGlobalVariablesTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    private final GitHubPipelineGlobalVariables variables = new GitHubPipelineGlobalVariables();

    @Test
    public void branchBuild() throws Exception {
        FreeStyleBuild build = createBuild(gitHubSource(false), new BranchSCMHead("main"));

        Collection<GlobalVariable> result = variables.forRun(build);

        assertThat(variableNames(result), containsInAnyOrder("repository"));
    }

    @Test
    public void pullRequestBuild() throws Exception {
        FreeStyleBuild build = createBuild(gitHubSource(false), pullRequestHead());

        Collection<GlobalVariable> result = variables.forRun(build);

        assertThat(variableNames(result), containsInAnyOrder("repository", "pullRequest"));
    }

    @Test
    public void branchBuildWithMergedPullRequestDetection() throws Exception {
        FreeStyleBuild build = createBuild(gitHubSource(true), new BranchSCMHead("main"));

        Collection<GlobalVariable> result = variables.forRun(build);

        assertThat(variableNames(result), containsInAnyOrder("repository", "mergedPullRequest"));
    }

    @Test
    public void pullRequestBuildWithMergedPullRequestDetection() throws Exception {
        FreeStyleBuild build = createBuild(gitHubSource(true), pullRequestHead());

        Collection<GlobalVariable> result = variables.forRun(build);

        assertThat(variableNames(result), containsInAnyOrder("repository", "pullRequest", "mergedPullRequest"));
    }

    @Test
    public void nonGitHubSource() throws Exception {
        FreeStyleBuild build = createBuild(new GitSCMSource("https://example.com/repo.git"), pullRequestHead());

        Collection<GlobalVariable> result = variables.forRun(build);

        assertTrue(result.isEmpty());
    }

    @Test
    public void unknownHeadDoesNotExposePullRequestVariables() throws Exception {
        FreeStyleBuild build = createBuild(gitHubSource(true), new SCMHead("other"));

        Collection<GlobalVariable> result = variables.forRun(build);

        assertThat(variableNames(result), containsInAnyOrder("repository"));
    }

    @Test
    public void noSource() throws Exception {
        FreeStyleBuild build = createBuild(null, new BranchSCMHead("main"));

        Collection<GlobalVariable> result = variables.forRun(build);

        assertTrue(result.isEmpty());
    }

    @Test
    public void noRun() {
        Collection<GlobalVariable> result = variables.forRun(null);

        assertTrue(result.isEmpty());
    }

    private GitHubSCMSource gitHubSource(boolean detectMergedPullRequests) {
        GitHubSCMSource source = new GitHubSCMSource("owner", "repo", null, false);
        if (detectMergedPullRequests) {
            source.setTraits(Collections.singletonList(new DetectMergedPullRequestTrait()));
        }
        return source;
    }

    private PullRequestSCMHead pullRequestHead() {
        return new PullRequestSCMHead("PR-42", "owner", "repo", null, 42, null, null, null);
    }

    private FreeStyleBuild createBuild(SCMSource source, SCMHead head) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject();
        r.jenkins.getExtensionList(SCMSource.SourceByItem.class).add(new SCMSource.SourceByItem() {
            @Override
            public SCMSource getSource(Item item) {
                return item == project ? source : null;
            }
        });
        r.jenkins.getExtensionList(SCMHead.HeadByItem.class).add(new SCMHead.HeadByItem() {
            @Override
            public SCMHead getHead(Item item) {
                return item == project ? head : null;
            }
        });

        return r.buildAndAssertSuccess(project);
    }

    private List<String> variableNames(Collection<GlobalVariable> result) {
        return result.stream().map(GlobalVariable::getName).collect(Collectors.toList());
    }
}
