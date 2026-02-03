package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Job;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedPullRequest;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext;

import java.io.Serializable;

/**
 * Groovy object that represents a GitHub PullRequest that was merged into a branch/PR.
 */
@PersistIn(PersistenceContext.NONE)
public class MergedPullRequestGroovyObject extends PullRequestGroovyObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean exists = false;

    MergedPullRequestGroovyObject(@NonNull final Job job, final ExtendedPullRequest pr) throws Exception {
    	super(job, pr);
    	this.exists = pr != null;
    }

    @Whitelisted
    public boolean isExists() {
        return exists;
    }

}
