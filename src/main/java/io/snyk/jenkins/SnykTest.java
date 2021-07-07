package io.snyk.jenkins;

import hudson.*;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.command.Command;
import io.snyk.jenkins.command.CommandLine;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.model.ObjectMapperHelper;
import io.snyk.jenkins.model.SnykTestResult;
import io.snyk.jenkins.tools.SnykInstallation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykTest {

  private static final Logger LOG = LoggerFactory.getLogger(SnykTest.class);

  public static Result testProject(
    SnykContext context,
    SnykConfig config,
    SnykInstallation installation,
    AtomicInteger exitCode
  ) throws IOException, InterruptedException {
    PrintStream logger = context.getLogger();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars envVars = context.getEnvVars();

    ArgumentListBuilder command = CommandLine.asArgumentList(
      installation.getSnykExecutable(launcher),
      Command.TEST,
      config,
      envVars
    );

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    logger.println("Testing for known issues...");
    logger.println("> " + command);
    exitCode.set(
      launcher.launch()
        .cmds(command)
        .envs(envVars)
        .stdout(stdout)
        .stderr(logger)
        .quiet(true)
        .pwd(workspace)
        .join()
    );

    String reportJson = stdout.toString(UTF_8.name());
    if (LOG.isTraceEnabled()) {
      LOG.trace("snyk test command: {}", command);
      LOG.trace("snyk test exit code: {}", exitCode);
      LOG.trace("snyk test stdout: {}", reportJson);
    }

    SnykTestResult result = Optional.ofNullable(ObjectMapperHelper.unmarshallTestResult(reportJson))
      .orElseThrow(() -> new AbortException("Failed to test project. Could not parse JSON output."));

    Optional.ofNullable(result.error)
      .map(Util::fixEmptyAndTrim)
      .ifPresent(error -> {
        throw new RuntimeException("Failed to test project. " + result.error);
      });

    if (exitCode.get() >= 2) {
      throw new AbortException("Failed to test project. (Exit Code: " + exitCode + ")");
    }

    if (!result.ok) {
      logger.println("Vulnerabilities found!");
      logger.printf(
        "Result: %s known vulnerabilities | %s dependencies%n",
        result.uniqueCount,
        result.dependencyCount
      );
    }

    return new Result(result.projectName, reportJson);
  }

  public static class Result {
    private final String name;
    private final String json;

    public Result(String name, String json) {
      this.name = name;
      this.json = json;
    }

    public String getName() {
      return name;
    }

    public String getJson() {
      return json;
    }
  }
}
