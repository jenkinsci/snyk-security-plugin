package io.snyk.jenkins;

import hudson.model.Run;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.Stapler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykReportBuildAction implements RunAction2 {

  private transient Run<?, ?> run;
  private Map<String, String> reports;

  public SnykReportBuildAction() {
    this.reports = new HashMap<>();
  }

  @Override
  public void onAttached(Run<?, ?> run) {
    this.run = run;
  }

  @Override
  public void onLoad(Run<?, ?> run) {
    this.run = run;
  }

  @SuppressWarnings("unused")
  public Run<?, ?> getRun() {
    return run;
  }

  @Override
  public String getIconFileName() {
    return "/plugin/snyk-security-scanner/img/icon.png";
  }

  @Override
  public String getDisplayName() {
    return "Snyk Security Report";
  }

  @Override
  public String getUrlName() {
    return "snyk";
  }

  @SuppressWarnings("unused")
  public Map<String, String> getReports() {
    return reports;
  }

  @SuppressWarnings("unused")
  public Map<String, String> getReportLinks() {
    return reports.keySet().stream().collect(Collectors.toMap(
      key -> key,
      key -> {
        try {
          return "./report?name=" + URLEncoder.encode(key, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    ));
  }

  @SuppressWarnings("unused")
  public String getCurrentReport() {
    return reports.get(Stapler.getCurrentRequest().getParameter("name"));
  }
}
