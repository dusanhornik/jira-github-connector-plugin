package com.atlassian.jira.plugins.github.activeobjects.v1;

import com.atlassian.jira.plugins.scm.SourceControlMapper;
import com.atlassian.jira.plugins.scm.SourceControlRepository;

public interface GitHubMapper extends SourceControlMapper
{
    /**
     * Return a list of all repository uris for the given project
     * @param projectKey the jira project
     * @return a list of repositories
     */
    Iterable<SourceControlRepository> getRepositories(String projectKey);

    /**
     * Get a repository by id
     * @param id the id of the repository
     * @return a repository or null if not found
     */
    SourceControlRepository getRepository(int id);

    /**
     * Remove the specified repository
     * @param id the id of the repository
     */
    void removeRepository(int id);

    /**
     * Map a repository to the specified jira project
     * @param repository the new repository
     */
    void addRepository(SourceControlRepository repository);

    /**
     * Update a new repository mapping with new information
     * @param repo the new repository details
     */
    void updateRepository(SourceControlRepository repo);

    /**
     * Return a list of all commits mapped to the given issue from the given repository
     * @param issueId the jira issue id
     * @return a list of changesets
     */
//    Iterable<BitbucketChangeset> getChangesets(String issueId);

    /**
     * Map a changeset to an issue id for the given repository
     * @param issueId the jira issue id
     * @param bitbucketChangeset the changeset to map to
     */
//    void addChangeset(String issueId, BitbucketChangeset bitbucketChangeset);

}
