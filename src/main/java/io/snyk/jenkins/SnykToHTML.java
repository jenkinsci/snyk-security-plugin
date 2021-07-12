package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.transform.ReportConverter;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.time.Instant;

import static io.snyk.jenkins.config.SnykConstants.SNYK_REPORT_HTML;
import static io.snyk.jenkins.config.SnykConstants.SNYK_TEST_REPORT_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykToHTML {
  public static FilePath generateReport(
    SnykContext context,
    SnykInstallation installation
  ) {
    try {
      FilePath workspace = context.getWorkspace();
      Launcher launcher = context.getLauncher();
      EnvVars env = context.getEnvVars();

      FilePath snykReportJson = workspace.child(SNYK_TEST_REPORT_JSON);
      if (!snykReportJson.exists()) {
        throw new RuntimeException("Snyk Report JSON does not exist.");
      }

      FilePath reportPath = workspace.child(getURLSafeDateTime() + "_" + SNYK_REPORT_HTML);
      reportPath.write("", UTF_8.name());

      ArgumentListBuilder args = new ArgumentListBuilder()
        .add(installation.getReportExecutable(launcher))
        .add("-i", SNYK_TEST_REPORT_JSON);

      int exitCode = launcher.launch()
        .cmds(args)
        .envs(env)
        .stdout(reportPath.write())
        .quiet(true)
        .pwd(workspace)
        .join();

      if (exitCode != 0) {
        throw new RuntimeException("Exit Code: " + exitCode);
      }

      String reportContents = ReportConverter.getInstance().modifyHeadSection(
        reportPath.readToString(),
        Jenkins.get().servletContext.getContextPath()
      );

      reportPath.write(reportContents, UTF_8.name());

      return reportPath;
    } catch (IOException | InterruptedException | RuntimeException ex) {
      throw new RuntimeException("Failed to generate report.", ex);
    }
  }

  private static String getURLSafeDateTime() {
    return Instant.now().toString()
      .replaceAll(":", "-")
      .replaceAll("\\.", "-");
  }
}
