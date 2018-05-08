package org.jenkinsci.plugins.pipeline.github.client;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.egit.github.core.User;

public class Review implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATE_APPROVED = "APPROVED";
    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_CHANGES_REQUESTED = "CHANGES_REQUESTED";
    public static final String STATE_DISMISSED = "DISMISSED";
    public static final String STATE_COMMENTED = "COMMENTED";

    private static final Set<String> reviewStates = Stream
        .of(STATE_APPROVED, STATE_PENDING, STATE_CHANGES_REQUESTED, STATE_DISMISSED, STATE_COMMENTED)
        .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));

    private String body;
    private String commitId;
    private long id;
    private User user;
    private String state;

    public String getBody() {
        return body;
    }

    public Review setBody(final String body) {
        this.body = body;
        return this;
    }

    public String getCommitId() {
        return commitId;
    }

    public Review setCommitId(final String commitId) {
        this.commitId = commitId;
        return this;
    }

    public long getId() {
        return id;
    }

    public Review setId(final long id) {
        this.id = id;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Review setUser(final User user) {
        this.user = user;
        return this;
    }

    public String getState() {
        return state;
    }

    /**
     * @param state one of APPROVED, PENDING, CHANGES_REQUESTED, DISMISSED, COMMENTED
     * @return this review
     * throws {@link IllegalArgumentException} if state is invalid
     */
    public Review setState(final String state) {
        if (reviewStates.contains(state)) {
            this.state = state;
            return this;
        }
        throw new IllegalArgumentException(MessageFormat.format("Invalid state {0}", state));
    }
}
