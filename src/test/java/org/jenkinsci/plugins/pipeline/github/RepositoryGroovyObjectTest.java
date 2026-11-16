package org.jenkinsci.plugins.pipeline.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import hudson.model.Item;
import jenkins.scm.api.SCMSource;

public class RepositoryGroovyObjectTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    public static class TestSourceByItemImpl extends SCMSource.SourceByItem {
        private final int port;

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
    }

    @Test
    public void testRepositoryProperties() throws Exception {
        WorkflowJob job = r.createProject(WorkflowJob.class, "p");
        r.jenkins.getExtensionList(SCMSource.SourceByItem.class).add(new TestSourceByItemImpl(wireMockRule.port()));

        stubFor(get(urlPathMatching("/api/v3/repos/owner/repo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"name\":\"repo\",\"owner\":{\"login\":\"owner\"},\"default_branch\":\"main\","
                    + "\"description\":\"A test repository\",\"html_url\":\"https://github.com/owner/repo\","
                    + "\"clone_url\":\"https://github.com/owner/repo.git\","
                    + "\"ssh_url\":\"git@github.com:owner/repo.git\","
                    + "\"git_url\":\"git://github.com/owner/repo.git\",\"private\":true,\"fork\":false,"
                    + "\"watchers\":17,\"forks\":3,\"open_issues\":5,\"id\":12345}")));

        RepositoryGroovyObject repository = new RepositoryGroovyObject(job);

        assertEquals("repo", repository.getName());
        assertEquals("owner", repository.getOwner());
        assertEquals("main", repository.getDefaultBranch());
        assertEquals("A test repository", repository.getDescription());
        assertEquals("https://github.com/owner/repo", repository.getUrl());
        assertEquals("https://github.com/owner/repo.git", repository.getCloneUrl());
        assertEquals("git@github.com:owner/repo.git", repository.getSshUrl());
        assertEquals("git://github.com/owner/repo.git", repository.getGitUrl());
        assertTrue(repository.isPrivate());
        assertFalse(repository.isFork());
        assertEquals(17, repository.getStargazersCount());
        assertEquals(3, repository.getForksCount());
        assertEquals(5, repository.getOpenIssuesCount());
        assertEquals(12345L, repository.getId());
    }
}
