package org.jenkinsci.plugins.pipeline.github.client;

import java.io.Serializable;
import java.text.MessageFormat;

import org.eclipse.egit.github.core.User;

public class Review implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATE_APPROVED = "APPROVED";
    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_CHANGES_REQUESTED = "CHANGES_REQUESTED";
    public static final String STATE_DISMISSED = "DISMISSED";
    public static final String STATE_COMMENTED = "COMMENTED";

    private String body;
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
        if (STATE_APPROVED.equals(state) || STATE_PENDING.equals(state) || STATE_CHANGES_REQUESTED.equals(state)
                || STATE_DISMISSED.equals(state) || STATE_COMMENTED.equals(state)) {
            this.state = state;
            return this;
        }
        throw new IllegalArgumentException(MessageFormat.format("Invalid state {0}", state));
    }
}
