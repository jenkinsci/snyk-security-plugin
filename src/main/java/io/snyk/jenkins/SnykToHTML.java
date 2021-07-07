package io.snyk.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.tools.SnykInstallation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykToHTML {

  private static final Logger LOG = LoggerFactory.getLogger(SnykToHTML.class);

  public static String generateReport(
    SnykContext context,
    SnykInstallation installation,
    String input
  ) throws IOException, InterruptedException {
    PrintStream logger = context.getLogger();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars env = context.getEnvVars();

    ArgumentListBuilder command = new ArgumentListBuilder()
      .add(installation.getReportExecutable(launcher));

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    logger.println("Generating report...");
    logger.println("> " + command);
    int exitCode = launcher.launch()
      .cmds(command)
      .envs(env)
      .stdin(new ByteArrayInputStream(input.getBytes(UTF_8.name())))
      .stdout(stdout)
      .stderr(logger)
      .quiet(true)
      .pwd(workspace)
      .join();

    String report = stdout.toString(UTF_8.name());

    if (LOG.isTraceEnabled()) {
      LOG.trace("snyk monitor command: {}", command);
      LOG.trace("snyk monitor exit code: {}", exitCode);
      LOG.trace("snyk monitor stdout: {}", report);
    }

    if (exitCode != 0) {
      throw new AbortException("Failed to generate report. (Exit Code: " + exitCode + ")");
    }

    return report;
  }
}
