package com.atlassian.jira.plugins.github.webwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.github.activeobjects.v1.DefaultGitHubMapper;
import com.atlassian.jira.plugins.scm.SourceControlRepository;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class GitHubCommits {

    private String repositoryURL;
    private String projectKey;

    final Logger logger = LoggerFactory.getLogger(GitHubCommits.class);
	private final DefaultGitHubMapper gitHubMapper;

    public GitHubCommits(DefaultGitHubMapper gitHubMapper, SourceControlRepository repository)
	{
		this.gitHubMapper = gitHubMapper;
		this.repositoryURL = repository.getUrl();
		this.projectKey = repository.getProjectKey();
	}

	// Generates a URL for pulling commit messages based upon the base Repository URL
    private String inferCommitsURL(){
        String[] path = repositoryURL.split("/");
        return "https://github.com/api/v2/json/commits/list/" + path[3] + "/" + path[4] + "/" + path[5];
    }

    // Generate a URL for pulling a single commits details (diff and author)
    private String inferCommitDetailsURL(){
        String[] path = repositoryURL.split("/");
        return "https://github.com/api/v2/json/commits/show/" + path[3] + "/" + path[4] +"/";
    }

    private String getBranchFromURL(){
        String[] path = repositoryURL.split("/");
        return path[5];
    }

    // Only used for Private Github Repositories
    private String getAccessTokenParameter()
    {

        String accessToken = gitHubMapper.getAccessToken(projectKey, repositoryURL);
        
        if (accessToken == null){
            return "";
        }else{
            return "&access_token=" + accessToken;
        }

    }

    private String getCommitsList(Integer pageNumber){
        logger.debug("GitHubCommits.getCommitsList()");
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {

            logger.debug("GitHubCommits - Commits URL - " + this.inferCommitsURL() + "?page=" + Integer.toString(pageNumber) + this.getAccessTokenParameter() );
            url = new URL(this.inferCommitsURL() + "?page=" + Integer.toString(pageNumber) + this.getAccessTokenParameter());
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();

            // Sets current page status for UI feedback
            gitHubMapper.setCurrentSync(projectKey, repositoryURL, Integer.toString(pageNumber));

        }catch (MalformedURLException e){
            logger.debug("GitHubCommits.MalformedException()");
            //e.printStackTrace();
            if(pageNumber.equals(1)){
                result = "GitHub Repository can't be found or incorrect credentials.";
            }

            gitHubMapper.setCurrentSync(projectKey, repositoryURL, "complete");

        } catch (Exception e) {
            logger.debug("GitHubCommits.exception()");
            //e.printStackTrace();
            if(pageNumber.equals(1)){
                result = "GitHub Repository can't be found or incorrect credentials.";
            }

            gitHubMapper.setCurrentSync(projectKey, repositoryURL, "complete");

        }

        return result;
    }

    // Commit list returns id (hashed) and Message
    // you have to call each individual commit to get diff details
    public String getCommitDetails(String commit_id_url){
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(commit_id_url + this.getAccessTokenParameter());
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        }catch (MalformedURLException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private ArrayList extractProjectKey(String message){
        Pattern projectKeyPattern = Pattern.compile("(" + this.projectKey + "-\\d*)", Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        ArrayList<String> matches = new ArrayList<String>();

        while(match.find()){
            // Get all groups for this match
            for (int i=0; i<=match.groupCount(); i++) {
                logger.debug("GitHubCommits.extractProjectKey() - index: " + i + " projectKey: " + match.group(i));
                matches.add(match.group(i));
            }
        }

        return matches;
    }

    public String syncCommits(Integer pageNumber){
        logger.debug("GitHubCommits.syncCommits()");
        Date date = new Date();

        logger.debug("searchCommits()");
        String commitsAsJSON = getCommitsList(pageNumber);

        String messages = "";

        if (commitsAsJSON != ""){

            try{
                JSONObject jsonCommits = new JSONObject(commitsAsJSON);
                JSONArray commits = jsonCommits.getJSONArray("commits");

                for (int i = 0; i < commits.length(); ++i) {
                    String message = commits.getJSONObject(i).getString("message").toLowerCase();
                    String commit_id = commits.getJSONObject(i).getString("id");
                    logger.debug("GitHubCommits.syncCommits() - commit_id:" + commit_id);

                    // Detect presence of JIRA Issue Key
                    if (message.indexOf(this.projectKey.toLowerCase()) > -1){

                        ArrayList extractedIssues = extractProjectKey(message);

                        // Remove duplicate IssueIDs
                        HashSet h = new HashSet(extractedIssues);
                        extractedIssues.clear();
                        extractedIssues.addAll(h);

                        for (int j=0; j < extractedIssues.size(); ++j){
                            String issueId = (String)extractedIssues.get(j).toString().toUpperCase();
                            logger.debug("GitHubCommits.syncCommits() - Found issueId: " + issueId + " in commit " + commit_id);

                            addCommitID(issueId, commit_id, getBranchFromURL(), repositoryURL);
                            gitHubMapper.incrementJiraCommitTotal(projectKey, repositoryURL); 
                        }

                    }else{
                    	gitHubMapper.incrementNonJiraCommitTotal(projectKey, repositoryURL); 
                    }
                }

                Integer nextCommitPage = pageNumber + 1;
                messages += this.syncCommits(nextCommitPage);

            }catch (JSONException e){
                //e.printStackTrace();
                gitHubMapper.setCurrentSync(projectKey, repositoryURL, "complete");
                messages = "GitHub Repository can't be found or incorrect credentials.";
            }

        }

        return messages;

    }



    public String postReceiveHook(String payload){

        logger.debug("postBack()");
        String messages = "";

        try{
            JSONObject jsonCommits = new JSONObject(payload);
            JSONArray commits = jsonCommits.getJSONArray("commits");

            for (int i = 0; i < commits.length(); ++i) {
                String message = commits.getJSONObject(i).getString("message").toLowerCase();
                String commit_id = commits.getJSONObject(i).getString("id");

                // Detect presence of JIRA Issue Key
                if (message.indexOf(this.projectKey.toLowerCase()) > -1){

                        ArrayList extractedIssues = extractProjectKey(message);

                        for (int j=0; j < extractedIssues.size(); ++j){
                            String issueId = (String)extractedIssues.get(j).toString().toUpperCase();
                            addCommitID(issueId, commit_id, getBranchFromURL(), repositoryURL);
                            gitHubMapper.incrementJiraCommitTotal(projectKey, repositoryURL);
                        }

                }else{
                	gitHubMapper.incrementNonJiraCommitTotal(projectKey, repositoryURL);
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
            return "exception";
        }

        return messages;


    }

    // Manages the entry of multiple Github commit id hash ids associated with an issue
    // urls look like - https://github.com/api/v2/json/commits/show/mojombo/grit/5071bf9fbfb81778c456d62e111440fdc776f76c?branch=master
    private void addCommitID(String issueKey, String commitId, String branch, String repositoryUrl)
    {
    	logger.debug("GitHubCommits.addCommitID()");

    	String commitUrl = inferCommitDetailsURL() + commitId + "?branch=" + branch;
    	gitHubMapper.addIssueMapping(issueKey, commitUrl, projectKey, repositoryUrl);

    }
}
