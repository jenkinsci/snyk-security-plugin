package io.snyk.jenkins.tools;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class SnykInstallationCallableTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public TemporaryFolder snykToolDirectory = new TemporaryFolder();

  @Test
  public void call_shouldNotThrownAnyExceptions_ifWrapperExecutableExists() throws Exception {
    Platform current = Platform.current();
    File root = snykToolDirectory.newFolder();
    File snyk = new File(root, current.snykWrapperFileName);
    snyk.mkdirs();
    SnykInstallationCallable snykInstallationCallable = new SnykInstallationCallable("snyk", root.getAbsolutePath());

    snykInstallationCallable.call();
  }

  @Test
  public void invoke_shouldNotThrownAnyExceptions_ifReportExecutableExists() throws Exception {
    Platform current = Platform.current();
    File root = snykToolDirectory.newFolder();
    File snyk = new File(root, current.snykToHtmlWrapperFileName);
    snyk.mkdirs();
    SnykInstallationCallable snykInstallationCallable = new SnykInstallationCallable("snyk-to-html", root.getAbsolutePath());

    snykInstallationCallable.call();
  }
}
