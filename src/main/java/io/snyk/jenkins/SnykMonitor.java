package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.command.Command;
import io.snyk.jenkins.command.CommandLine;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.model.ObjectMapperHelper;
import io.snyk.jenkins.model.SnykMonitorResult;
import io.snyk.jenkins.tools.SnykInstallation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static hudson.Util.fixEmptyAndTrim;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(SnykMonitor.class);

  public static void monitorProject(
    SnykContext context,
    SnykConfig config,
    SnykInstallation installation
    ) throws IOException, InterruptedException {
    PrintStream logger = context.getLogger();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars envVars = context.getEnvVars();

    ArgumentListBuilder command = CommandLine.asArgumentList(
      installation.getSnykExecutable(launcher),
      Command.MONITOR,
      config,
      envVars
    );

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    logger.println("Remember project for continuous monitoring...");
    logger.println("> " + command);
    int exitCode = launcher.launch()
      .cmds(command)
      .envs(envVars)
      .stdout(stdout)
      .stderr(logger)
      .quiet(true)
      .pwd(workspace)
      .join();

    String reportJson = stdout.toString(UTF_8.name());
    if (exitCode != 0) {
      logger.println("Failed to monitor project. (Exit Code: " + exitCode + ")");
      logger.println(reportJson);
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("snyk monitor command: {}", command);
      LOG.trace("snyk monitor exit code: {}", exitCode);
      LOG.trace("snyk monitor stdout: {}", reportJson);
    }

    SnykMonitorResult result = ObjectMapperHelper.unmarshallMonitorResult(reportJson);
    if (result != null && fixEmptyAndTrim(result.uri) != null) {
      logger.println("Explore the snapshot at " + result.uri);
    }
  }
}
