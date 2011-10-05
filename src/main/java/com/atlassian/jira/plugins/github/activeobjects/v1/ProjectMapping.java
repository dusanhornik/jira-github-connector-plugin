package com.atlassian.jira.plugins.github.activeobjects.v1;

import net.java.ao.Entity;

/**
 * Active objects storage for the mapping between a bitbucket repository and a jira project.
 */
public interface ProjectMapping extends Entity
{
    String getRepositoryUri();
    String getProjectKey();
    String getAccessToken();
    String getCurrentSync();
    String getJiraCommitTotal();
    String getNonJiraCommitTotal();
    
    void setJiraCommitTotal(String jiraCommitTotal);
    void setNonJiraCommitTotal(String nonJiraCommitTotal);
    void setCurrentSync(String currentSync);
    void setAccessToken(String accessToken);
    void setRepositoryUri(String uri);
    void setProjectKey(String projectKey);
}
