package org.jenkinsci.plugins.pipeline.github.client;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.service.MilestoneService;

/**
 * @author Aaron Whiteside
 */
public class ExtendedMilestoneService extends MilestoneService {

    public ExtendedMilestoneService(final ExtendedGitHubClient client) {
        super(client);
    }

    @Override
    public ExtendedGitHubClient getClient() {
        return (ExtendedGitHubClient) super.getClient();
    }

    @Override
    public ExtendedMilestone getMilestone(final IRepositoryIdProvider repository, final int number) {
        return getMilestone(repository, Integer.toString(number));
    }

    @Override
    public ExtendedMilestone getMilestone(final IRepositoryIdProvider repository, final String number) {
        String repoId = getId(repository);
        return getMilestone(repoId, number);
    }

    private ExtendedMilestone getMilestone(final String id, final String number) {
        if (number == null) {
            throw new IllegalArgumentException("Milestone cannot be null");
        } else if (number.length() == 0) {
            throw new IllegalArgumentException("Milestone cannot be empty");
        } else {
            StringBuilder uri = new StringBuilder("/repos");
            uri.append('/').append(id);
            uri.append("/milestones");
            uri.append('/').append(number);
            GitHubRequest request = createRequest();
            request.setUri(uri);
            request.setType(ExtendedMilestone.class);
            return (ExtendedMilestone)getClient().getUnchecked(request).getBody();
        }
    }
}
