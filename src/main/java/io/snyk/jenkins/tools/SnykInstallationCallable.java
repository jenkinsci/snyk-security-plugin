package io.snyk.jenkins.tools;

import java.io.IOException;
import java.lang.String;
import java.nio.file.Path;
import java.nio.file.Paths;

import jenkins.security.MasterToSlaveCallable;

public class SnykInstallationCallable extends MasterToSlaveCallable<String, IOException> {

    private static final long serialVersionUID = -2170188619091680208L;
    private final String executableName;
    private final String home;

    public SnykInstallationCallable(String executableName, String home) {
        this.executableName = executableName;
        this.home = home;
    }

    @Override
    public String call() throws IOException {
        return resolveExecutable(this.executableName, Platform.current());
    }

    private String resolveExecutable(String file, Platform platform) throws IOException {
        final Path nodeModulesBin = getNodeModulesBin();
        if (nodeModulesBin != null) {
            final Path executable = nodeModulesBin.resolve(file);
            if (!executable.toFile().exists()) {
                throw new IOException(String.format("Could not find executable <%s>", executable));
            }
            return executable.toAbsolutePath().toString();
        } else {
            String root = getHome();
            if (root == null) {
                return null;
            }
            String wrapperFileName = "snyk".equals(file) ? platform.snykWrapperFileName
                    : platform.snykToHtmlWrapperFileName;
            final Path executable = Paths.get(root).resolve(wrapperFileName);
            if (!executable.toFile().exists()) {
                throw new IOException(String.format("Could not find executable <%s>", wrapperFileName));
            }
            return executable.toAbsolutePath().toString();
        }
    }

    private Path getNodeModulesBin() {
        String root = getHome();
        if (root == null) {
            return null;
        }

        Path nodeModules = Paths.get(root).resolve("node_modules").resolve(".bin");
        if (!nodeModules.toFile().exists()) {
            return null;
        }

        return nodeModules;
    }

    public String getHome() {
        return home;
    }

    public String getExecutableName() {
        return executableName;
    }
}
