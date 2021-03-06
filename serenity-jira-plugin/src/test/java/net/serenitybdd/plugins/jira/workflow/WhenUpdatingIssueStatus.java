package net.serenitybdd.plugins.jira.workflow;

import net.thucydides.core.annotations.Feature;
import net.thucydides.core.annotations.Issue;
import net.thucydides.core.annotations.Issues;
import net.thucydides.core.annotations.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.MockEnvironmentVariables;
import net.serenitybdd.plugins.jira.JiraListener;
import net.serenitybdd.plugins.jira.model.IssueTracker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenUpdatingIssueStatus {

    @Feature
    public static final class SampleFeature {
        public class SampleStory {}
        public class SampleStory2 {}
    }

    @Story(SampleFeature.SampleStory.class)
    static class SampleTestCase {

        @Issue("#MYPROJECT-123")
        public void issue_123_should_be_fixed_now() {}

        @Issues({"#MYPROJECT-123","#MYPROJECT-456"})
        public void issue_123_and_456_should_be_fixed_now() {}

        public void anotherTest() {}
    }

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        environmentVariables = new MockEnvironmentVariables();
        environmentVariables.setProperty("jira.url", "http://my.jira.server");
        environmentVariables.setProperty("thucydides.public.url", "http://my.server/myproject/thucydides");

        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
    }

    @Mock
    IssueTracker issueTracker;

    EnvironmentVariables environmentVariables;
    
    ClasspathWorkflowLoader workflowLoader;

    @Test
    public void a_successful_test_should_not_update_status_if_workflow_is_not_activated() {

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Open");

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"false");

        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        verify(issueTracker, never()).doTransition(eq("MYPROJECT-123"),anyString());
    }

    @Test
    public void a_successful_test_should_not_update_status_if_workflow_update_status_is_not_specified() {

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Open");

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"");

        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("MYPROJECT-123"),anyString());
    }

    @Test
    public void a_successful_test_should_resolve_an_open_issue() {


        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Open");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker).doTransition("MYPROJECT-123", "Resolve Issue");
    }


    @Test
    public void a_successful_test_should_resolve_an_in_progress_issue() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("In Progress");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        InOrder inOrder = inOrder(issueTracker);
        inOrder.verify(issueTracker).doTransition("MYPROJECT-123","Stop Progress");
        inOrder.verify(issueTracker).doTransition("MYPROJECT-123","Resolve Issue");
    }

    @Test
    public void a_successful_test_should_resolve_a_reopened_issue() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Reopened");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker).doTransition("MYPROJECT-123", "Resolve Issue");
    }

    @Test
    public void a_successful_test_should_not_affect_a_resolved_issue() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Resolved");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void a_failing_test_should_open_a_resolved_issue() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Resolved");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker).doTransition("MYPROJECT-123", "Reopen Issue");
    }

    @Test
    public void a_failing_test_should_open_a_closed_issue() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Closed");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker).doTransition("MYPROJECT-123", "Reopen Issue");
    }

    @Test
    public void a_failing_test_should_leave_an_open_issue_open() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Open");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void a_failing_test_should_leave_a_reopened_issue_reopened() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Reopen");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void a_failing_test_should_leave_in_progress_issue_in_progress() {

        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("In Progress");

        JiraListener listener = new JiraListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("MYPROJECT-123"), anyString());
    }

    private TestOutcome newTestOutcome(String testMethod, TestResult testResult) {
        TestOutcome result = TestOutcome.forTest(testMethod, SampleTestCase.class);
        TestStep step = new TestStep("a narrative description");
        step.setResult(testResult);
        result.recordStep(step);
        return result;
    }
}
