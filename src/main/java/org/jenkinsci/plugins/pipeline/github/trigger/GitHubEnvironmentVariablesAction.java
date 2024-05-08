package org.jenkinsci.plugins.pipeline.github.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Restricted(NoExternalUse.class)
public class GitHubEnvironmentVariablesAction extends ParametersAction {

    private List<ParameterValue> parameters;

    public GitHubEnvironmentVariablesAction(List<ParameterValue> parameters) {
        super(parameters);
        this.parameters = parameters;
    }

    public GitHubEnvironmentVariablesAction(ParameterValue... parameters) {
        this(Arrays.asList(parameters));
    }

    @Override
    public List<ParameterValue> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public ParameterValue getParameter(String name) {
        for (ParameterValue parameter : parameters) {
            if (parameter != null && parameter.getName().equals(name)) {
                return parameter;
            }
        }

        return null;
    }

    @Extension
    public static final class GitHubAdditionalParameterEnvironmentContributor extends EnvironmentContributor {

        @Override
        @SuppressWarnings("rawtypes")
        public void buildEnvironmentFor(@NonNull Run run,
                                        @NonNull EnvVars envs,
                                        @NonNull TaskListener listener) throws IOException, InterruptedException {

            GitHubEnvironmentVariablesAction action = run.getAction(GitHubEnvironmentVariablesAction.class);
            if (action != null) {
                for (ParameterValue p : action.getParameters()) {
                    envs.put(p.getName(), String.valueOf(p.getValue()));
                }
            }
            super.buildEnvironmentFor(run, envs, listener);
        }
    }
}
