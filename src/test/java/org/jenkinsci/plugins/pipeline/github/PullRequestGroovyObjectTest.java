package org.jenkinsci.plugins.pipeline.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import hudson.model.Item;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;

/**
 * @author Aaron Whiteside
 */
public class PullRequestGroovyObjectTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    public static class TestHeadByItemImpl extends SCMHead.HeadByItem {
        @Override
        public SCMHead getHead(Item item) {
            return new PullRequestSCMHead("PR-42", "owner", "repo", null, 42, null, null, null);
        }
    }

    public static class TestSourceByItemImpl extends SCMSource.SourceByItem {
        public TestSourceByItemImpl(int port) {
            this.port = port;
        }

        @Override
        public SCMSource getSource(Item item) {
            GitHubSCMSource result = new GitHubSCMSource("owner", "repo", null, false);
            result.setCredentialsId("credentialsId");
            result.setApiUri(String.format("http://localhost:%d", port));
            return result;
        }

        private int port;
    }

    @Test
    public void testReadOnlyProperties() throws Exception {
        WorkflowJob job = r.createProject(WorkflowJob.class, "p");
        r.getInstance().getAllItems().forEach(System.out::println);
    }

    @Test
    public void testPullRequestBaseSha() throws Exception {
        WorkflowJob job = r.createProject(WorkflowJob.class, "p");
        r.jenkins.getExtensionList(SCMHead.HeadByItem.class).add(new TestHeadByItemImpl());
        r.jenkins.getExtensionList(SCMSource.SourceByItem.class).add(new TestSourceByItemImpl(wireMockRule.port()));
        stubFor(get(urlPathMatching("/api/v3/repos/owner/repo/pulls/42"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"base\":{\"sha\":\"abc\"}}")));
        PullRequestGroovyObject prgo = new PullRequestGroovyObject((WorkflowJob)job);
        assertEquals("abc", prgo.getBaseSha());
   }
}