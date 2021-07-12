package io.snyk.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.exception.SnykErrorException;
import io.snyk.jenkins.exception.SnykIssueException;
import io.snyk.jenkins.tools.SnykInstallation;

import java.io.IOException;
import java.util.function.Supplier;

public class SnykStepFlow {

  public static void perform(SnykConfig config, Supplier<SnykContext> contextSupplier) throws SnykIssueException, SnykErrorException {
    int testExitCode = 0;
    Exception cause = null;
    SnykContext context = null;

    try {
      context = contextSupplier.get();
      testExitCode = SnykStepFlow.scan(context, config);
    } catch (IOException | InterruptedException | RuntimeException ex) {
      if (context != null) {
        TaskListener listener = context.getTaskListener();
        if (ex instanceof IOException) {
          Util.displayIOException((IOException) ex, listener);
        }
        ex.printStackTrace(listener.fatalError("Snyk command execution failed"));
      }
      cause = ex;
    }

    if (config.isFailOnIssues() && testExitCode == 1) {
      throw new SnykIssueException();
    }
    if (config.isFailOnError() && cause != null) {
      throw new SnykErrorException(cause.getMessage());
    }
  }

  private static int scan(SnykContext context, SnykConfig config)
  throws IOException, InterruptedException {
    SnykInstallation installation = SnykInstallation.install(
      context,
      config.getSnykInstallation()
    );

    context.getEnvVars().put("SNYK_TOKEN", SnykApiToken.getToken(context, config.getSnykTokenId()));

    int testExitCode = SnykTest.testProject(context, config, installation);

    if (config.isMonitorProjectOnBuild()) {
      SnykMonitor.monitorProject(context, config, installation);
    }

    FilePath report = SnykToHTML.generateReport(context, installation);
    archiveReport(context, report);
    addSidebarLink(context);

    return testExitCode;
  }

  private static void archiveReport(SnykContext context, FilePath report) throws IOException, InterruptedException {
    Run<?, ?> run = context.getRun();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    TaskListener listener = context.getTaskListener();
    new ArtifactArchiver(report.getName())
      .perform(run, workspace, launcher, listener);
  }

  private static void addSidebarLink(SnykContext context) {
    Run<?, ?> run = context.getRun();
    if (run.getActions(SnykReportBuildAction.class).isEmpty()) {
      run.addAction(new SnykReportBuildAction(run));
    }
  }
}
