package com.atlassian.jira.plugins.github.activeobjects.v1;

import java.util.List;
import java.util.Map;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.scm.SourceControlChangeset;
import com.atlassian.jira.plugins.scm.SourceControlRepository;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class DefaultGitHubMapper implements GitHubMapper
{

	private final ActiveObjects activeObjects;

	public DefaultGitHubMapper(ActiveObjects activeObjects)
	{
		this.activeObjects = activeObjects;
	}

    public void removeRepository(final int id)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                activeObjects.delete(activeObjects.get(ProjectMapping.class, id));
                activeObjects.delete(activeObjects.find(IssueMapping.class, "repository_id = ?", id));
                return null;
            }
        });
    }

	
    public void addRepository(final SourceControlRepository repository)
    {
//        checkDuplicates(repository);
    	
    	final Map map = new ImmutableMap.Builder<Object, Object>()
    			.put("repository_uri", validUri(repository.getUri()))
    			.put("project_key", repository.getProjectKey())
    			.put("access_token", "")
    			.put("current_sync", "")
    			.put("jira_commit_total", "")
    			.put("non_jira_commit_total", "")
    			.build();
    	
    	activeObjects.executeInTransaction(new TransactionCallback<Object>()
    			{
    		public Object doInTransaction()
    		{
    			ProjectMapping projectMapping = activeObjects.create(ProjectMapping.class, map);
    			return null; 
    		}
    			});
    }
    
	public int addRepository1(final SourceControlRepository repository)
	{
//        checkDuplicates(repository);

		final Map map = new ImmutableMap.Builder<Object, Object>()
				.put("repository_uri", validUri(repository.getUri()))
				.put("project_key", repository.getProjectKey())
				.put("access_token", "")
				.put("current_sync", "")
				.put("jira_commit_total", "")
				.put("non_jira_commit_total", "")
				.build();
		
		return activeObjects.executeInTransaction(new TransactionCallback<Integer>()
        {
            public Integer doInTransaction()
            {
                ProjectMapping projectMapping = activeObjects.create(ProjectMapping.class, map);
                return projectMapping.getID(); 
            }
        });
	}
	
	private SourceControlRepository getSourceControlRepositoryFromProjectMapping(ProjectMapping projectMapping)
	{
		if (projectMapping == null) return null;
		String uri = projectMapping.getRepositoryUri();
		return new SourceControlRepository(projectMapping.getID(), projectMapping.getProjectKey(), uri, uri, uri);
	}

	private Object validUri(String uri)
	{
		// TODO Auto-generated method stub
		return uri;
	}

	public List<IssueMapping> getIssueMappings(final String issueKey)
	{
		return activeObjects.executeInTransaction(new TransactionCallback<List<IssueMapping>>()
		{
			public List<IssueMapping> doInTransaction()
			{
            	IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class,"issue_key = ?", issueKey);
            	return Lists.newArrayList(issueMappings);
			}
		});
	}
	public void addIssueMapping(final String issueKey, final String commitUrl, final String projectKey, final String repositoryUri)
	{
		
		activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
            	ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class,"project_key = ? and repository_uri = ?", projectKey, repositoryUri);
            	if (projectMappings.length==0) throw new GitHubException();
            	int repositoryId = projectMappings[0].getID();
            	final Map map = new ImmutableMap.Builder<Object, Object>()
            			.put("issue_key", issueKey)
            			.put("commit_url", commitUrl)
            			.put("repository_id", repositoryId)
            			.build();
                activeObjects.create(IssueMapping.class, map);
                return null;
            }
        });
	}

	public String getAccessToken(final String projectKey, final String repositoryUri)
	{
        return activeObjects.executeInTransaction(new TransactionCallback<String>()
        {
            public String doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ? and repository_uri = ?", projectKey, repositoryUri);
                if (projectMappings.length>0)
                	return projectMappings[0].getAccessToken();
                return null;
            }
        });
	}

	public void setCurrentSync(final String projectKey, final String repositoryUri, final String currentSync)
	{
		activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ? and repository_uri = ?", projectKey, repositoryUri);
            	if (projectMappings.length==0) throw new GitHubException();
                ProjectMapping first = projectMappings[0];
                first.setCurrentSync(currentSync);
                first.save();
                return null;
            }
        });
	}

	public void incrementJiraCommitTotal(final String projectKey, final String repositoryUri)
	{
		activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ? and repository_uri = ?", projectKey, repositoryUri);
            	if (projectMappings.length==0) throw new GitHubException();
                ProjectMapping first = projectMappings[0];
                first.setJiraCommitTotal(first.getJiraCommitTotal()+1);
                first.save();
                return null;
            }
        });
	}
	
	public void incrementNonJiraCommitTotal(final String projectKey, final String repositoryUri)
	{
		activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ? and repository_uri = ?", projectKey, repositoryUri);
            	if (projectMappings.length==0) throw new GitHubException();
                ProjectMapping first = projectMappings[0];
                first.setNonJiraCommitTotal(first.getNonJiraCommitTotal()+1);
                first.save();
                return null;
            }
        });
	}

	public void removeRepository(final String repositoryId)
	{
		activeObjects.executeInTransaction(new TransactionCallback<Object>()
		{
			public Object doInTransaction()
			{
				ProjectMapping projectMapping = activeObjects.get(ProjectMapping.class, Integer.valueOf(repositoryId));
				activeObjects.delete(projectMapping);
				activeObjects.delete(activeObjects.find(IssueMapping.class, "repository_id = ?", repositoryId));
				return null;
			}
		});
	}

	public ProjectMapping getProjectMapping(final String projectKey, final String repositoryUri)
	{
        return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping>()
        {
            public ProjectMapping doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ? and repository_uri = ?", projectKey, repositoryUri);
                if (projectMappings.length>0)
                	return projectMappings[0];
                return null;
            }
        });
	}

	public SourceControlRepository getSourceControlRepository(final String projectKey, final String repositoryUri)
	{
        return activeObjects.executeInTransaction(new TransactionCallback<SourceControlRepository>()
        {
            public SourceControlRepository doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ? and repository_uri = ?", projectKey, repositoryUri);
                if (projectMappings.length>0)
                	return getSourceControlRepositoryFromProjectMapping(projectMappings[0]);
                return null;
            }
        });
	}

	public void resetCommitTotals(final String projectKey, final String repositoryUri)
	{
		activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ? and repository_uri = ?", projectKey, repositoryUri);
            	if (projectMappings.length==0) throw new GitHubException();
                ProjectMapping first = projectMappings[0];
                first.setNonJiraCommitTotal("0");
                first.setJiraCommitTotal("0");
                first.setCurrentSync("0");
                first.save();
                return null;
            }
        });
	}

	public List<ProjectMapping> getProjectRepositories(final String projectKey)
	{
        return activeObjects.executeInTransaction(new TransactionCallback<List<ProjectMapping>>()
        {
            public List<ProjectMapping> doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "project_key = ?", projectKey);
                return Lists.newArrayList(projectMappings);
            }
        });
	}

	public void setAccessToken(final SourceControlRepository repository, final String accessToken)
	{
		activeObjects.executeInTransaction(new TransactionCallback<Object>()
		{
			public Object doInTransaction()
			{
				ProjectMapping projectMapping = activeObjects.get(ProjectMapping.class, repository.getId());
				projectMapping.setAccessToken(accessToken);
				projectMapping.save();
				return null;
			}
		});
	}

	public Iterable<? extends SourceControlChangeset> getChangesets(String issueId)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public SourceControlRepository getRepository(final int id)
	{
        return activeObjects.executeInTransaction(new TransactionCallback<SourceControlRepository>()
        {
            public SourceControlRepository doInTransaction()
            {
                ProjectMapping projectMapping = activeObjects.get(ProjectMapping.class, id);
                return getSourceControlRepositoryFromProjectMapping(projectMapping);
            }
        });	}

	public void updateRepository(SourceControlRepository repo)
	{
		// TODO Auto-generated method stub
	
	}

	public Iterable<SourceControlRepository> getRepositories(String projectKey)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}
