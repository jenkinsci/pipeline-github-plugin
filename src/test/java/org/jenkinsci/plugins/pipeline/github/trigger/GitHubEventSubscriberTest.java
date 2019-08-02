package org.jenkinsci.plugins.pipeline.github.trigger;

import hudson.model.FreeStyleProject;
import hudson.model.Label;
import jenkins.branch.BranchSource;
import jenkins.scm.api.SCMEvent;
import jenkins.scm.api.SCMEvents;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMSourceEvent;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.github.GHEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class GitHubEventSubscriberTest {

    private static SCMEvent.Type firedEventType;
    private static GHSubscriberEvent ghEvent;

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        jRule.getInstance().getInjector().injectMembers(this);
    }

    @Before
    public void resetFiredEvent() {
        firedEventType = null;
        ghEvent = null;
    }

    @Test
    public void shouldBeNotApplicableForProjects() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        assertThat(new GitHubEventSubscriber().isApplicable(prj), is(false));
    }

    @Test
    public void given_ghCommentEventUpdated_then_createdHeadEventFired() throws Exception {
        TestSCMEventListener.setReceived(true);

        GitHubEventSubscriber subscriber = new GitHubEventSubscriber();

        firedEventType = SCMEvent.Type.UPDATED;
        ghEvent = callOnEvent(subscriber, "GitHubEventSubscriberTest/payload/comment.json", GHEvent.ISSUE_COMMENT);
        waitAndAssertReceived(true);
    }

    @Test
    public void given_ghCommentEventCreated_then_createdHeadEventFired() throws Exception {
        TestSCMEventListener.setReceived(true);

        GitHubEventSubscriber subscriber = new GitHubEventSubscriber();

        firedEventType = SCMEvent.Type.CREATED;
        ghEvent = callOnEvent(subscriber, "GitHubEventSubscriberTest/payload/comment.json", GHEvent.ISSUE_COMMENT);
        waitAndAssertReceived(true);
    }

    @Test
    public void given_ghCommentEventCreated_and_project_then_env_variables_are_created() throws Exception {
        TestSCMEventListener.setReceived(true);
        WorkflowJob p = jRule.jenkins.createProject(WorkflowJob.class, "demo");
        jRule.createOnlineSlave(Label.get("remote"));
        p.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "  agent { label 'remote' }\n" +
                        "  triggers {\n" +
                        "    issueCommentTrigger('test.*')\n" +
                        "  }\n" +
                        "  stages {\n" +
                        "    stage('Bar') {\n" +
                        "      options { skipDefaultCheckout() }\n" +
                        "      steps {\n" +
                        "        echo 'hi'\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}\n"));

        GitHubEventSubscriber subscriber = new GitHubEventSubscriber();

        firedEventType = SCMEvent.Type.CREATED;
        ghEvent = callOnEvent(subscriber, "GitHubEventSubscriberTest/payload/comment.json", GHEvent.ISSUE_COMMENT);
        waitAndAssertReceived(true);
    }

    @Test
    public void given_ghCommentEventCreated_and_project_then_env_variables_are_created2() throws Exception {
        WorkflowMultiBranchProject job = jRule.createProject(WorkflowMultiBranchProject.class);
        job.setSourcesList(Arrays.asList(new BranchSource(new GitHubSCMSource("super-mario-bros-pipelines", "pipeline"))));
        job.scheduleBuild();
        GitHubEventSubscriber subscriber = new GitHubEventSubscriber();

        firedEventType = SCMEvent.Type.CREATED;
        ghEvent = callOnEvent(subscriber, "GitHubEventSubscriberTest/payload/comment.json", GHEvent.ISSUE_COMMENT);
        waitAndAssertReceived(true);
    }

    @Test
    public void given_ghCommitCommentEventCreated_then_no_createdHeadEventFired() throws Exception {
        TestSCMEventListener.setReceived(false);

        GitHubEventSubscriber subscriber = new GitHubEventSubscriber();

        firedEventType = SCMEvent.Type.CREATED;
        ghEvent = callOnEvent(subscriber, "GitHubEventSubscriberTest/payload/comment.json", GHEvent.COMMIT_COMMENT);
        waitAndAssertReceived(false);
    }

    private GHSubscriberEvent callOnEvent(GitHubEventSubscriber subscriber, String eventPayloadFile, org.kohsuke.github.GHEvent ghevent) throws IOException {
        GHSubscriberEvent event = createEvent(eventPayloadFile, ghevent);
        subscriber.onEvent(event);
        return event;
    }

    private GHSubscriberEvent createEvent(String eventPayloadFile, org.kohsuke.github.GHEvent ghevent) throws IOException {
        String payload = IOUtils.toString(getClass().getResourceAsStream(eventPayloadFile));
        return new GHSubscriberEvent("myOrigin", ghevent, payload);
    }

    private void waitAndAssertReceived(boolean received) throws InterruptedException {
        long watermark = SCMEvents.getWatermark();
        // event will be fired by subscriber at some point
        SCMEvents.awaitOne(watermark, 1200, TimeUnit.MILLISECONDS);

        assertEquals("Event should have " + ((!received) ? "not " : "") + "been received", received, TestSCMEventListener.didReceive());
    }

    @TestExtension
    public static class TestSCMEventListener extends jenkins.scm.api.SCMEventListener {

        private static boolean eventReceived = false;

        public void onSCMHeadEvent(SCMHeadEvent<?> event) {
            receiveEvent(event.getType(), event.getOrigin());
        }

        public void onSCMSourceEvent(SCMSourceEvent<?> event) {
            receiveEvent(event.getType(), event.getOrigin());
        }

        private void receiveEvent(SCMEvent.Type type, String origin) {
            eventReceived = true;

            assertEquals("Event type should be the same", type, firedEventType);
            assertEquals("Event origin should be the same", origin, ghEvent.getOrigin());
        }

        public static boolean didReceive() {
            return eventReceived;
        }

        public static void setReceived(boolean received) {
            eventReceived = received;
        }

    }
}
