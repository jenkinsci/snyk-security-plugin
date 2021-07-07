package io.snyk.jenkins;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class SnykReportBuildAction implements RunAction2 {

  private Run<?, ?> run;
  private String report;

  public SnykReportBuildAction(String report) {
    this.report = report;
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
  public String getReport() {
    return report;
  }
}
