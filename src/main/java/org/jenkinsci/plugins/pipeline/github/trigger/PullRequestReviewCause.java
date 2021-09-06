package org.jenkinsci.plugins.pipeline.github.trigger;

import hudson.model.Cause;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.util.Arrays;

/**
 * Represents the user who reviewed the PR that triggered the build.
 *
 * @author Aaron Walker
 */
public class PullRequestReviewCause extends Cause {

  private final String userLogin;
  private final String comment;
  private final String state;
  private final String[] reviewStates;

  public PullRequestReviewCause(final String userLogin, final String state, final String comment, final String[] reviewStates) {
    this.userLogin = userLogin;
    this.state = state;
    this.comment = comment;
    this.reviewStates =  Arrays.copyOf(reviewStates, reviewStates.length);
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
  public String getState() {
      return state;
  }

  @Whitelisted
  public String[] getReviewStates() {
       return Arrays.copyOf(reviewStates, reviewStates.length);
  }

  @Override
  public String getShortDescription() {
      return String.format("%s reviewed: %s", userLogin, state);
  }
}
