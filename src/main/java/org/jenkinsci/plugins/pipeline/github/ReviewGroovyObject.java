package org.jenkinsci.plugins.pipeline.github;

import groovy.lang.GroovyObjectSupport;
import org.jenkinsci.plugins.pipeline.github.client.Review;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.Objects;

/**
 * Groovy wrapper for PR reviews
 */
public class ReviewGroovyObject extends GroovyObjectSupport implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Review review;

    ReviewGroovyObject(final Review review) {
        this.review = Objects.requireNonNull(review, "review cannot be null");
    }

    @Whitelisted
    public String getUser() {
        return review.getUser().getLogin();
    }

    @Whitelisted
    public String getBody() {
        return review.getBody();
    }

    @Whitelisted
    public String getCommitId() {
        return review.getCommitId();
    }

    @Whitelisted
    public long getId() {
        return review.getId();
    }

    @Whitelisted
    public String getState() {
        return review.getState();
    }
}
