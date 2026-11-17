package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for our {@link RepositoryGroovyObject} instance.
 *
 * @see RepositoryGroovyObject
 */
public class RepositoryGlobalVariable extends GlobalVariable {
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryGlobalVariable.class);

    @NonNull
    @Override
    public String getName() {
        return "repository";
    }

    @NonNull
    @Override
    public Object getValue(@NonNull final CpsScript script) throws Exception {
        final Run<?, ?> build = script.$build();
        if (build == null) {
            throw new IllegalStateException("No associated build");
        }
        LOG.trace("initializing {}", RepositoryGroovyObject.class);
        return new RepositoryGroovyObject(build.getParent());
    }

}
