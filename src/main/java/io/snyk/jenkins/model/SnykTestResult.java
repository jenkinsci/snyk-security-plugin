package io.snyk.jenkins.model;

public class SnykTestResult {
  public boolean ok = true;
  public String error;
  public int dependencyCount;
  public int uniqueCount;
  public String projectName;
}
