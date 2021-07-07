package io.snyk.jenkins.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class ObjectMapperHelper {

  private static final JsonFactory JSON_FACTORY;

  static {
    JSON_FACTORY = new MappingJsonFactory();
  }

  private ObjectMapperHelper() {
  }

  public static SnykMonitorResult unmarshallMonitorResult(String content) throws IOException {
    if (content == null || content.isEmpty()) {
      return null;
    }

    try (JsonParser parser = JSON_FACTORY.createParser(content)) {
      if (parser.nextToken() != JsonToken.START_OBJECT) {
        return null;
      }

      SnykMonitorResult snykMonitorResult = new SnykMonitorResult();
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String fieldName = parser.getCurrentName();

        if ("uri".equals(fieldName)) {
          parser.nextToken();
          snykMonitorResult.uri = parser.getText();
        } else {
          parser.skipChildren();
        }
      }
      return snykMonitorResult;
    }
  }

  public static SnykTestResult unmarshallTestResult(String content) throws IOException {
    if (content == null || content.isEmpty()) {
      return null;
    }

    try (JsonParser parser = JSON_FACTORY.createParser(content)) {
      JsonToken token = parser.nextToken();
      if (token == null) {
        return null;
      }

      if (token == JsonToken.START_ARRAY) {
        List<SnykTestResult> testStatuses = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
          testStatuses.add(readTestResult(parser));
        }
        return aggregateTestResults(testStatuses);
      } else if (token == JsonToken.START_OBJECT) {
        return readTestResult(parser);
      } else {
        return null;
      }
    }
  }

  private static SnykTestResult readTestResult(JsonParser parser) throws IOException {
    SnykTestResult snykTestResult = new SnykTestResult();

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String fieldName = parser.getCurrentName();

      if ("ok".equals(fieldName)) {
        parser.nextToken();
        snykTestResult.ok = parser.getBooleanValue();
      } else if ("error".equals(fieldName)) {
        parser.nextToken();
        snykTestResult.error = parser.getText();
      } else if ("uniqueCount".equals(fieldName)) {
        parser.nextToken();
        snykTestResult.uniqueCount = parser.getIntValue();
      } else if ("dependencyCount".equals(fieldName)) {
        parser.nextToken();
        snykTestResult.dependencyCount = parser.getIntValue();
      } else if ("projectName".equals(fieldName)) {
        parser.nextToken();
        snykTestResult.projectName = parser.getText();
      } else {
        parser.skipChildren();
      }
    }

    return snykTestResult;
  }

  private static SnykTestResult aggregateTestResults(List<SnykTestResult> testResults) {
    SnykTestResult aggregatedTestResult = new SnykTestResult();

    testResults.forEach(entity -> {
      aggregatedTestResult.ok = aggregatedTestResult.ok && entity.ok;
      aggregatedTestResult.error = (entity.error != null && !entity.error.isEmpty())
        ? String.join(". ", aggregatedTestResult.error, entity.error)
        : aggregatedTestResult.error;
      aggregatedTestResult.dependencyCount += entity.dependencyCount;
      aggregatedTestResult.uniqueCount += entity.uniqueCount;
    });

    aggregatedTestResult.projectName = testResults.stream()
      .map(result -> result.projectName)
      .collect(Collectors.joining(", "));

    return aggregatedTestResult;
  }
}
