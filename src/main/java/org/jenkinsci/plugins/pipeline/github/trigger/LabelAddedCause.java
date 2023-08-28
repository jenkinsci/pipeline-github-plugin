package org.jenkinsci.plugins.pipeline.github.trigger;

import hudson.model.Cause;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import java.util.List;

/**
 * Represents the user who authored the comment that triggered the build.
 *
 * @author Aaron Whiteside
 */
public class LabelAddedCause extends Cause {
    private final String userLogin;
    private final String labelAdded;


    public LabelAddedCause(final String userLogin, final String label) {
        this.userLogin = userLogin;
        this.labelAdded = label;
    }

    @Whitelisted
    public String getUserLogin() {
        return userLogin;
    }

    @Whitelisted
    public String getLabelAdded() {
        return labelAdded;
    }

    @Override
    public String getShortDescription() {
        return String.format("%s added label %s", userLogin, labelAdded);
    }
}
