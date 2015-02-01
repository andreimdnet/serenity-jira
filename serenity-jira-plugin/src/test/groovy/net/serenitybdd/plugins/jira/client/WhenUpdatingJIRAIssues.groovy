package net.serenitybdd.plugins.jira.client

import com.google.common.base.Optional
import net.serenitybdd.plugins.jira.domain.IssueComment
import net.serenitybdd.plugins.jira.domain.IssueSummary
import spock.lang.Specification

class WhenUpdatingJIRAIssues extends Specification {

    def "should be able to add a comment to an issue"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO")
            IssueSummary issue = jiraClient.findByKey("DEMO-2").get()
            int commentCount = issue.comments.size()
        when:
            jiraClient.addComment("DEMO-2", "Integration test comment");
        then:
            Optional<IssueSummary> reloadedIssue = jiraClient.findByKey("DEMO-2")
            reloadedIssue.get().comments.size() == commentCount + 1
    }

    def "should be able to update a comment"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO")
            IssueSummary issue = jiraClient.findByKey("DEMO-2").get()
        when:
            IssueComment updatedComment = issue.comments[0].withText("Updated integration test comment")
            jiraClient.updateComment("DEMO-2", updatedComment);
        then:
            Optional<IssueSummary> reloadedIssue = jiraClient.findByKey("DEMO-2")
            reloadedIssue.get().comments[0].text == "Updated integration test comment"
    }



}