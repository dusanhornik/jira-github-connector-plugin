package com.atlassian.jira.plugins.github.activeobjects.v1;

import net.java.ao.Entity;

public interface IssueMapping extends Entity
{
	String getIssueKey();
	String getCommitUrl();
    Integer getRepositoryId();

	void setIssueKey(String issueKey);
	void setCommitUrl(String commitUrl);
    void setRepositoryId(Integer id);

}
