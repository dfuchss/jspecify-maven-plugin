/* Licensed under MIT 2025. */
package org.fuchss.jspecify;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Maven plugin Mojo that generates @NullMarked package-info.java files
 * into target/generated-sources/jspecify and registers it as source root.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JSpecifyGeneratorMojo extends AbstractMojo {

    /**
     * The source directory to scan for Java packages.
     * Defaults to src/main/java of the project.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/java", required = true)
    private File sourceDir;

    /**
     * The directory where generated sources will be written.
     * Defaults to target/generated-sources/jspecify.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/jspecify", required = true)
    private File generatedSourcesDirectory;

    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating package-info.java files from source directory: " + sourceDir);
        getLog().info("Generated files will be written to: " + generatedSourcesDirectory);

        if (!sourceDir.exists()) {
            getLog().warn("Source directory does not exist: " + sourceDir);
            return; // nothing to do
        }

        initializeGeneratedSourcesDirectory();

        try {
            // Walk through source directory packages
            try (Stream<Path> packageDirs = Files.walk(sourceDir.toPath())) {
                for (Path path : packageDirs.filter(Files::isDirectory).toList()) {
                    processPackageDirectory(path);
                }
            }

            // Register generated sources directory as a compile source root
            project.addCompileSourceRoot(generatedSourcesDirectory.getAbsolutePath());
            getLog().info("Added generated sources root: " + generatedSourcesDirectory.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating package-info.java files", e);
        }
    }

    private void initializeGeneratedSourcesDirectory() throws MojoExecutionException {
        if (!generatedSourcesDirectory.exists()) {
            if (!generatedSourcesDirectory.mkdirs()) {
                throw new MojoExecutionException(
                        "Could not create generated sources directory: " + generatedSourcesDirectory);
            }
            getLog().info("Created generated sources directory: " + generatedSourcesDirectory);
        } else {
            // Clean up existing generated files
            try (Stream<Path> existingFiles = Files.walk(generatedSourcesDirectory.toPath())) {
                existingFiles.filter(Files::isRegularFile).forEach(path -> path.toFile()
                        .delete());
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Could not clean up existing generated files in: " + generatedSourcesDirectory, e);
            }
            getLog().info("Cleaned up existing generated files in: " + generatedSourcesDirectory);
        }
    }

    private void processPackageDirectory(Path packageDir) {
        try {
            // Check if directory contains Java files (excluding package-info.java)
            boolean hasJavaFiles = Files.list(packageDir)
                    .anyMatch(p -> p.toString().endsWith(".java")
                            && !p.getFileName().toString().equals("package-info.java"));

            if (!hasJavaFiles) {
                return; // no Java files, skip
            }

            if (packageDir.resolve("package-info.java").toFile().exists()) {
                getLog().info("Skipping package-info.java file generation for " + packageDir
                        + " as it already exists.");
                return; // package-info.java already exists, skip
            }

            // Compute the package name relative to sourceDir
            Path relativePath = sourceDir.toPath().relativize(packageDir);
            String packageName = relativePath.toString().replace(File.separatorChar, '.');

            // Target package-info.java path in generated sources folder
            Path targetDir = generatedSourcesDirectory.toPath().resolve(relativePath);
            Path targetFile = targetDir.resolve("package-info.java");

            if (Files.exists(targetFile)) {
                // Already exists, skip
                return;
            }

            // Create directories if needed
            Files.createDirectories(targetDir);

            // Define content of packageinfo as NullMarked for package.
            String content = String.format(
                    """
                    @org.jspecify.annotations.NullMarked
                    package %s;
                    """,
                    packageName);

            Files.writeString(targetFile, content, StandardOpenOption.CREATE_NEW);
            getLog().info("Generated package-info.java for package: " + packageName);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate package-info.java in " + packageDir, e);
        }
    }
}
