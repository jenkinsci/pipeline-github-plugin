package org.jenkinsci.plugins.pipeline.github.trigger;

import hudson.model.Cause;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

/**
 * Represents the user who authored the comment that triggered the build.
 *
 * @author Aaron Whiteside
 */
public class IssueCommentCause extends Cause {
    private final String userLogin;
    private final String comment;
    private final String triggerPattern;

    public IssueCommentCause(final String userLogin, final String comment, final String triggerPattern) {
        this.userLogin = userLogin;
        this.comment = comment;
        this.triggerPattern = triggerPattern;
    }

    @Whitelisted
    public String getUserLogin() {
        return userLogin;
    }

    @Whitelisted
    public String getComment() {
        return comment;
    }

    @Whitelisted
    public String getTriggerPattern() {
        return triggerPattern;
    }

    @Override
    public String getShortDescription() {
        return String.format("%s commented: %s", userLogin, comment);
    }
}
