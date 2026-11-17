package org.jenkinsci.plugins.pipeline.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.scm.NullSCM;
import jenkins.branch.Branch;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.scm.impl.NullSCMSource;
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GitHubPipelineGlobalVariablesTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

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

    @Test
    public void pullRequestClosedDuringBuild() throws Exception {
        GitHubSCMSource source = gitHubSource(true);
        source.setApiUri(wireMockRule.baseUrl());
        WorkflowJob job = createPullRequestJob(source);
        WorkflowMultiBranchProject project = (WorkflowMultiBranchProject) job.getParent();
        GitHubSCMSource unrelatedSource = gitHubSource(false);
        unrelatedSource.setId("unrelated");
        project.setSourcesList(Arrays.asList(new BranchSource(unrelatedSource), new BranchSource(source)));
        job.setDefinition(new CpsFlowDefinition(
                "semaphore 'close'; assert pullRequest.baseSha == 'abc'; assert repository.name == 'repo'", true));
        wireMockRule.stubFor(get(urlEqualTo("/api/v3/repos/owner/repo/pulls/42"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"number\":42,\"state\":\"closed\",\"base\":{\"sha\":\"abc\"}}")));
        wireMockRule.stubFor(get(urlEqualTo("/api/v3/repos/owner/repo"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"repo\",\"owner\":{\"login\":\"owner\"}}")));

        WorkflowRun build = job.scheduleBuild2(0).waitForStart();
        SemaphoreStep.waitForStart("close/1", build);
        build.addAction(new SCMRevisionAction(source,
                new PullRequestSCMRevision(pullRequestHead(), "base", "pull")));
        assertThat(variableNames(variables.forRun(build)),
                containsInAnyOrder("repository", "pullRequest", "mergedPullRequest"));

        project.getProjectFactory().setBranch(job, new Branch.Dead(project.getProjectFactory().getBranch(job)));
        assertTrue(SCMSource.SourceByItem.findSource(job) instanceof NullSCMSource);
        assertThat(variableNames(variables.forRun(build)),
                containsInAnyOrder("repository", "pullRequest", "mergedPullRequest"));
        assertEquals(source, GitHubHelper.getScmSource(job));

        SemaphoreStep.success("close/1", null);
        r.assertBuildStatusSuccess(r.waitForCompletion(build));
    }

    @Test
    public void orphanedJobDoesNotGuessSource() throws Exception {
        GitHubSCMSource source = gitHubSource(false);
        WorkflowJob job = createPullRequestJob(source);
        job.setDefinition(new CpsFlowDefinition("", true));
        WorkflowRun build = r.buildAndAssertSuccess(job);
        WorkflowMultiBranchProject project = (WorkflowMultiBranchProject) job.getParent();
        project.getProjectFactory().setBranch(job, new Branch.Dead(project.getProjectFactory().getBranch(job)));

        assertTrue(variables.forRun(build).isEmpty());

        build.addAction(new SCMRevisionAction(source,
                new PullRequestSCMRevision(pullRequestHead(), "base", "pull")));
        GitHubSCMSource unrelatedSource = gitHubSource(false);
        unrelatedSource.setId("unrelated");
        project.setSourcesList(Collections.singletonList(new BranchSource(unrelatedSource)));

        assertTrue(variables.forRun(build).isEmpty());
        assertTrue(GitHubHelper.getScmSource(job) instanceof NullSCMSource);
    }

    private WorkflowJob createPullRequestJob(GitHubSCMSource source) throws Exception {
        WorkflowMultiBranchProject project = r.createProject(WorkflowMultiBranchProject.class);
        project.getSourcesList().add(new BranchSource(source));
        WorkflowJob job = project.getProjectFactory().newInstance(
                new Branch(source.getId(), pullRequestHead(), new NullSCM(), Collections.emptyList()));
        job.onCreatedFromScratch();
        project.addLoadedChild(job, job.getName());
        return job;
    }

    private GitHubSCMSource gitHubSource(boolean detectMergedPullRequests) {
        GitHubSCMSource source = new GitHubSCMSource("owner", "repo", null, false);
        if (detectMergedPullRequests) {
            source.setTraits(Collections.singletonList(new DetectMergedPullRequestTrait()));
        }
        return source;
    }

    private PullRequestSCMHead pullRequestHead() {
        return new PullRequestSCMHead("PR-42", "owner", "repo", null, 42, new BranchSCMHead("main"), null, null);
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
