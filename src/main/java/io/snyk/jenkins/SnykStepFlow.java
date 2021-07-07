package io.snyk.jenkins;

import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.exception.SnykErrorException;
import io.snyk.jenkins.exception.SnykIssueException;
import io.snyk.jenkins.tools.SnykInstallation;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class SnykStepFlow {

  public static void perform(SnykConfig config, Supplier<SnykContext> contextSupplier) throws SnykIssueException, SnykErrorException {
    SnykContext context = null;
    AtomicInteger testExitCode = new AtomicInteger(0);
    Exception cause = null;

    try {
      context = contextSupplier.get();
      SnykStepFlow.scan(context, config, testExitCode);
    } catch (IOException | InterruptedException | RuntimeException ex) {
      if (context != null) {
        TaskListener listener = context.getTaskListener();
        if (ex instanceof IOException) {
          Util.displayIOException((IOException) ex, listener);
        }
        ex.printStackTrace(listener.fatalError("Snyk Security failed with errors."));
      }
      cause = ex;
    }

    if (config.isFailOnIssues() && testExitCode.get() == 1) {
      throw new SnykIssueException();
    }
    if (config.isFailOnError() && cause != null) {
      throw new SnykErrorException(cause.getMessage());
    }
  }

  private static void scan(SnykContext context, SnykConfig config, AtomicInteger testExitCode)
  throws IOException, InterruptedException {
    SnykInstallation installation = SnykInstallation.install(
      context,
      config.getSnykInstallation()
    );

    context.getEnvVars().put("SNYK_TOKEN", SnykApiToken.getToken(context, config.getSnykTokenId()));

    SnykTest.Result testResult = SnykTest.testProject(context, config, installation, testExitCode);
    String htmlResult = SnykToHTML.generateReport(context, installation, testResult.getJson());
    addReportAction(context, testResult.getName(), htmlResult);

    if (config.isMonitorProjectOnBuild()) {
      SnykMonitor.monitorProject(context, config, installation);
    }
  }

  private static void addReportAction(SnykContext context, String name, String report) {
    Run<?, ?> run = context.getRun();
    Optional.ofNullable(run.getAction(SnykReportBuildAction.class))
      .orElseGet(() -> {
        SnykReportBuildAction newAction = new SnykReportBuildAction();
        run.addAction(newAction);
        return newAction;
      })
      .getReports()
      .put(name, report);
  }
}
