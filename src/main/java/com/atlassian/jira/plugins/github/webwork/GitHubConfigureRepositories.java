package com.atlassian.jira.plugins.github.webwork;


import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.plugins.github.activeobjects.v1.DefaultGitHubMapper;
import com.atlassian.jira.plugins.github.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.scm.SourceControlRepository;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;


public class GitHubConfigureRepositories extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;
    final Logger logger = LoggerFactory.getLogger(GitHubConfigureRepositories.class);

    JiraWebActionSupport jwas = new JiraWebActionSupport();
	private final DefaultGitHubMapper gitHubMapper;
	private final ApplicationProperties ap;

    public GitHubConfigureRepositories(PluginSettingsFactory pluginSettingsFactory, ActiveObjects activeObjects, ApplicationProperties applicationProperties){
        this.pluginSettingsFactory = pluginSettingsFactory;
		ap = applicationProperties;
		this.gitHubMapper = new DefaultGitHubMapper(activeObjects);
    }

    protected void doValidation() {
        //logger.debug("GitHubConfigureRepositories - doValidation()");
        for (Enumeration e =  request.getParameterNames(); e.hasMoreElements() ;) {
            String n = (String)e.nextElement();
            String[] vals = request.getParameterValues(n);
            //validations = validations + "name " + n + ": " + vals[0];
        }

        // GitHub URL Validation
        if (!url.equals("")){
            logger.debug("URL for Evaluation: " + url + " - NA: " + nextAction);
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory")){
                // Valid URL and URL starts with github.com domain
                Pattern p = Pattern.compile("^(https|http)://github.com/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
                Matcher m = p.matcher(url);
                if (!m.matches()){
                    validations = "Please supply a valid GitHub repository URL.";
                }
            }
        }else{
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory")){
                validations = "Please supply a valid GitHub repository URL.";
            }
        }

    }


    public String doDefault(){

        return "input";
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception {
        logger.debug("NextAction: " + nextAction);

        // Remove trailing slashes from URL
        if (url.endsWith("/")){
            url = url.substring(0, url.length() - 1);
        }

        // Set all URLs to HTTPS
        if (url.startsWith("http:")){
            url = url.replaceFirst("http:","https:");
        }

        // Add default branch of 'master' to URL if missing
        String[] urlArray = url.split("/");

        if(urlArray.length == 5){
            url += "/master";
            urlArray = url.split("/");
        }

        if (validations.equals("")){
            if (nextAction.equals("AddRepository")){

                if (repoVisibility.equals("private")){
                    logger.debug("Private Add Repository");
                    String clientID = "";
                    clientID = (String)pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientID");

                    String contextPath = request.getContextPath();
					if (StringUtils.isBlank(clientID))
					{
						// logger.debug("No Client ID");
						validations = "You will need to setup a <a href='" + contextPath + "/secure/admin/ConfigureGlobalSettings!default.jspa'>GitHub OAuth Application</a> before you can add private repositories";
					} else
					{
						int repoId = gitHubMapper.addRepository1(new SourceControlRepository(projectKey, url));
						String encodedUrl = URLEncoder.encode(ap.getBaseUrl() + "/secure/admin/GitHubOAuth2.jspa?&repositoryId="+repoId, "UTF-8"); 
						String redirectURI = "https://github.com/login/oauth/authorize?scope=repo&client_id=" + clientID + "&redirect_uri="+encodedUrl;
						redirectURL = redirectURI;

						return "redirect";
					}
                }else{
                    logger.debug("PUBLIC Add Repository");
                    gitHubMapper.addRepository(new SourceControlRepository(projectKey, url));
                    nextAction = "ForceSync";

                }

                postCommitURL = "GitHubPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length-1];

                logger.debug(postCommitURL);

            }

            if (nextAction.equals("ShowPostCommitURL")){
                postCommitURL = "GitHubPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length-1];
            }

            if (nextAction.equals("DeleteRepository")){
                gitHubMapper.removeRepository(repositoryId);
            }

            if (nextAction.equals("CurrentSyncStatus")){

                try{
                	ProjectMapping projectMapping = gitHubMapper.getProjectMapping(projectKey, url);
                	if (projectMapping!=null)
                	{
                		currentSyncPage = projectMapping.getCurrentSync(); 
                		nonJIRACommitTotal = projectMapping.getNonJiraCommitTotal();
                		JIRACommitTotal = projectMapping.getJiraCommitTotal();
                	} else
                	{
                		logger.debug("GitHubConfigureRepositories.doExecute().CurrentSyncStatus - ProjectMapping for project [] and repo [] not found",projectKey, url);
                	}

                }catch (Exception e){
                    logger.debug("GitHubConfigureRepositories.doExecute().CurrentSyncStatus - Exception reading plugin values.");
                }


                logger.debug("GitHubConfigureRepositories.doExecute().CurrentSyncStatus - currentSyncPage" + currentSyncPage);

                return "syncstatus";
            }

            if (nextAction.equals("SyncRepository")){
                syncRepository();
                return "syncmessage";

            }
        }

        return INPUT;
    }

    private void syncRepository(){

        logger.debug("GitHubConfigureRepositories.syncRepository() - Starting Repository Sync");

        SourceControlRepository scr = gitHubMapper.getSourceControlRepository(projectKey, url);
        GitHubCommits repositoryCommits = new GitHubCommits(gitHubMapper, scr);

        // Reset Commit count
        gitHubMapper.resetCommitTotals(projectKey, url);

        // Starts actual search of commits via GitAPI, "1" is the first
        // page of commits to be returned via the API
        messages = repositoryCommits.syncCommits(1);

    }

    // JIRA Project Listing
    private ComponentManager cm = ComponentManager.getInstance();
    private List<Project> projects = cm.getProjectManager().getProjectObjects();

    public List getProjects(){
        return projects;
    }

    public String getProjectName(){
        return cm.getProjectManager().getProjectObjByKey(projectKey).getName();
    }

    // Used to provide URLs on the repository management screen that go to actual pages
    // as the service does not support repo urls with branches
    public String getRepositoryURLWithoutBranch(String repoURL){

        Integer lastSlash = repoURL.lastIndexOf("/");
        return repoURL.substring(0,lastSlash);
    }

    public String escape(String unescapedHTML){
        return jwas.htmlEncode(unescapedHTML);
    }
    
    public List<ProjectMapping> getProjectRepositories(String projectKey)
    {
    	return gitHubMapper.getProjectRepositories(projectKey);
    }

    // Mode setting to 'single' indicates that this is administration of a single JIRA project
    // Bulk setting indicates multiple projects
    private String mode = "";
    public void setMode(String value){this.mode = value;}
    public String getMode(){return mode;}

    private String repositoryId = "";
    public void setRepositoryId(String value){this.repositoryId = value;}
    public String getRepositoryId(){return repositoryId;}

    
    // GitHub Repository URL
    private String url = "";
    public void setUrl(String value){this.url = value;}
    public String getURL(){return url;}

    // GitHub Post Commit URL for a specific project and repository
    private String postCommitURL = "";
    public void setPostCommitURL(String value){this.postCommitURL = value;}
    public String getPostCommitURL(){return postCommitURL;}

    // GitHub Repository Visibility
    private String repoVisibility = "";
    public void setRepoVisibility(String value){this.repoVisibility = value;}
    public String getRepoVisibility(){return repoVisibility;}

    // Project Key
    private String projectKey = "";
    public void setProjectKey(String value){this.projectKey = value;}
    public String getProjectKey(){return projectKey;}

    // Form Directive
    private String nextAction = "";
    public void setNextAction(String value){this.nextAction = value;}
    public String getNextAction(){return this.nextAction;}

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

    // Confirmation Messages
    private String messages = "";
    public String getMessages(){return this.messages;}

    // Redirect URL
    private String redirectURL = "";
    public String getRedirectURL(){return this.redirectURL;}

    // Current page of commits that is being processed
    private String currentSyncPage = "";
    public String getCurrentSyncPage(){return this.currentSyncPage;}


    private String nonJIRACommitTotal = "";
    public String getNonJIRACommitTotal(){return this.nonJIRACommitTotal;}

    // Current page of commits that is being processed
    private String JIRACommitTotal = "";
    public String getJIRACommitTotal(){return this.JIRACommitTotal;}

}
