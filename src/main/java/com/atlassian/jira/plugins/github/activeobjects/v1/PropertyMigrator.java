package com.atlassian.jira.plugins.github.activeobjects.v1;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class PropertyMigrator implements ActiveObjectsUpgradeTask
{
    private final Logger log = LoggerFactory.getLogger(PropertyMigrator.class);

	private final ProjectManager projectManager;

	private final PluginSettingsFactory pluginSettingsFactory;

	private final ActiveObjects activeObjects;

	public PropertyMigrator(final ProjectManager projectManager,
			final PluginSettingsFactory pluginSettingsFactory,
			final ActiveObjects activeObjects)
	{
		this.projectManager = projectManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.activeObjects = activeObjects;
	}

	public ModelVersion getModelVersion() 
	{
        return ModelVersion.valueOf("2");
	}

	private List<String> getRepositories(String projectKey)
    {
        List<String> repoUrls = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray");
        return repoUrls != null ? repoUrls : Collections.<String>emptyList();
    }

	public void upgrade(ModelVersion currentVersion, ActiveObjects ao) 
	{
	       log.error("upgrade [ " + getModelVersion() + " ]");

	        ao.migrate(ProjectMapping.class, IssueMapping.class);

	       List<Project> projects = projectManager.getProjectObjects();
	        for (Project project : projects)
	        {
	            String projectKey = project.getKey();
	            log.error(" === migrating repositories for project [{}] === ", projectKey);

	            List<String> repositories = getRepositories(projectKey);
	            for (String repository : repositories)
	            {
	            	log.error("migrating repository [{}]", repository);
	            }
	         }
	}

}
