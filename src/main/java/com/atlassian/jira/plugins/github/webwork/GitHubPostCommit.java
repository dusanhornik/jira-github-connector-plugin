package com.atlassian.jira.plugins.github.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.github.activeobjects.v1.DefaultGitHubMapper;
import com.atlassian.jira.plugins.scm.SourceControlRepository;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Created by IntelliJ IDEA.
 * User: michaelbuckbee
 * Date: 4/14/11
 * Time: 4:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class GitHubPostCommit extends JiraWebActionSupport {

    final Logger logger = LoggerFactory.getLogger(GitHubPostCommit.class);
	private final DefaultGitHubMapper gitHubMapper;

    public GitHubPostCommit(ActiveObjects activeObjects){
		this.gitHubMapper = new DefaultGitHubMapper(activeObjects);
    }

    protected void doValidation() {

        if (payload.equals("")){
            validations += "Missing Required GitHub 'payload' parameter. <br/>";
        }

    }

    protected String doExecute() throws Exception {

        if (validations.equals("")){
            logger.debug("Staring PostCommitUpdate");

            JSONObject jsonPayload = new JSONObject(payload);
            JSONObject jsonRepository = jsonPayload.getJSONObject("repository");

            String baseRepositoryURL = jsonRepository.getString("url");

            SourceControlRepository repository = gitHubMapper.getRepository(Integer.valueOf(repositoryId));
			GitHubCommits repositoryCommits = new GitHubCommits(gitHubMapper, repository);

            // Starts actual search of commits via GitAPI, "1" is the first
            // page of commits to be returned via the API
            validations = repositoryCommits.postReceiveHook(payload);

        }

        return "postcommit";
    }

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

    // GitHub JSON Payload
    private String payload = "";
    public void setPayload(String value){this.payload = value;}
    public String getPayload(){return payload;}

    // GitHub Repository ID
    private String repositoryId = "";
    public void setRepositoryId(String repositoryId){this.repositoryId = repositoryId;}
    public String getRepositoryId(){return repositoryId;}

}
