package org.jenkinsci.plugins.pipeline.github.client;

import com.google.gson.reflect.TypeToken;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aaron Whiteside
 */
public class ExtendedIssueService extends IssueService {

    public ExtendedIssueService(final ExtendedGitHubClient client) {
        super(client);
    }

    @Override
    public ExtendedGitHubClient getClient() {
        return (ExtendedGitHubClient) super.getClient();
    }

    public void lockIssue(final IRepositoryIdProvider repository, final int issueNumber) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/lock");
        getClient().put(uri.toString());
    }

    public void unlockIssue(final IRepositoryIdProvider repository, final int issueNumber) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/lock");
        getClient().delete(uri.toString());
    }

    public void addAssignees(final IRepositoryIdProvider repository,
                             final int issueNumber,
                             final List<String> assignees) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/assignees");
        Map<Object, Object> params = new HashMap<>(1, 1.0F);
        params.put("assignees", assignees);
        getClient().post(uri.toString(), params, Issue.class);
    }

    public void removeAssignees(final IRepositoryIdProvider repository,
                                final int issueNumber,
                                final List<String> assignees) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/assignees");
        Map<Object, Object> params = new HashMap<>(1, 1.0F);
        params.put("assignees", assignees);
        getClient().delete(uri.toString(), params);
    }

    public void setAssignees(final IRepositoryIdProvider repository,
                             final int issueNumber,
                             final List<String> assignees) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/assignees");
        Map<Object, Object> params = new HashMap<>(1, 1.0F);
        params.put("assignees", assignees);
        getClient().put(uri.toString(), params, Issue.class);
    }

    public PageIterator<Label> getLabels(final IRepositoryIdProvider repository, final int issueNumber) {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/labels");

        PagedRequest<Label> request = this.createPagedRequest();
        request.setUri(uri);
        request.setType((new TypeToken<List<Label>>() {}).getType());
        return this.createPageIterator(request);
    }

    public List<Label> setLabels(final IRepositoryIdProvider repository,
                                 final int issueNumber,
                                 final List<String> labels) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/labels");
        return getClient().put(uri.toString(), labels, (new TypeToken<List<Label>>(){}).getType());
    }

    public List<Label> addLabels(final IRepositoryIdProvider repository,
                                 final int issueNumber,
                                 final List<String> labels) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/labels");
        return getClient().post(uri.toString(), labels, (new TypeToken<List<Label>>() {}).getType());
    }

    public void removeLabel(final IRepositoryIdProvider repository,
                            final int issueNumber,
                            final String label) throws IOException {
        String repoId = this.getId(repository);
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repoId);
        uri.append("/issues");
        uri.append('/').append(issueNumber);
        uri.append("/labels");
        uri.append('/');
        uri.append(label);
        getClient().delete(uri.toString());
    }

}
