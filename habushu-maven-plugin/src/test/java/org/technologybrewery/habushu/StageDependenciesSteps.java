package org.technologybrewery.habushu;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StageDependenciesSteps {

    protected String testDefaultSingleMonorepoDepPath = "src/test/resources/stage-dependencies/"
            + "default-single-monorepo-dep/test-monorepo";
    protected String targetDefaultNoMonorepoDepPath = "target/test-classes/stage-dependencies/"
            + "default-no-monorepo-dep/test-monorepo";
    protected String targetDefaultSingleMonorepoDepPath = "target/test-classes/stage-dependencies/"
            + "default-single-monorepo-dep/test-monorepo";
    protected String mavenProjectPath;
    private final String POM_FILE = "pom.xml";

    private final StageDependenciesMojoTestCase mojoTestCase = new StageDependenciesMojoTestCase();

    private StageDependenciesMojo mojo;

    @Before("@stageDependencies")
    public void configureMavenTestSession() throws Exception {
        // important for registering Habushu's mojos to AbstractTestCase.mojoDescriptors,
        // which ensures that lookupConfiguredMojo will return the configured mojo
        mojoTestCase.configurePluginTestHarness();
    }

    @After("@stageDependencies")
    public void tearDownMavenPluginTestHarness() throws Exception {
        mojoTestCase.clearMavenProjectFiles();
        mojoTestCase.tearDownPluginTestHarness();
    }

    @Given("a single Habushu-type dependency")
    public void a_single_habushu_type_dependency() throws Exception {
        mavenProjectPath = targetDefaultSingleMonorepoDepPath + "/extensions/extensions-monorepo-dep-consuming-application";

        mojoTestCase.addMavenProjectFile(new File("src/test/resources/stage-dependencies/default-single-monorepo-dep/test-monorepo/extensions/extensions-python-dep-X/pom.xml"));
        mojoTestCase.addMavenProjectFile(new File("src/test/resources/stage-dependencies/default-single-monorepo-dep/test-monorepo/foundation/foundation-python-dep-Y/pom.xml"));

        mojo = (StageDependenciesMojo) mojoTestCase.lookupConfiguredMojo(
                new File(mavenProjectPath, POM_FILE), "stage-dependencies"
        );
    }

    @When("the stage-dependencies goal is executed")
    public void the_stage_dependencies_goal_is_executed() throws MojoExecutionException, MojoFailureException {
        mojo.execute();
    }

    @Then("the sources files of the dependency and transitive Habushu-type dependencies are staged and structured")
    public void the_source_files_of_the_dependency_and_transitive_habushu_type_dependencies_are_staged_and_structured() {
        Set<String> expectedHabushuDirsNames = new HashSet<>();
        expectedHabushuDirsNames.add("extensions/extensions-python-dep-X");
        expectedHabushuDirsNames.add("foundation/foundation-python-dep-Y");
        expectedHabushuDirsNames.add("foundation/foundation-sub/foundation-sub-python-dep-Z");
        assertStaged(new File(testDefaultSingleMonorepoDepPath), mojo.getAnchorOutputDirectory(), expectedHabushuDirsNames);
    }

    @Given("no Habushu-type dependencies")
    public void no_habushu_type_dependencies() throws Exception {
        mavenProjectPath = targetDefaultNoMonorepoDepPath + "/no-monorepo-dep-application";
        mojo = (StageDependenciesMojo) mojoTestCase.lookupConfiguredMojo(
                new File(mavenProjectPath, POM_FILE), "stage-dependencies"
        );
    }

    @Then("no source files are staged in the build directory")
    public void no_source_files_are_staged_in_the_build_directory() {
        String venvSupportPath = mojo.getSession().getCurrentProject().getBuild().getDirectory() + "/venv-support";
        Assertions.assertTrue(isNonExistentOrEmptyDir(new File(venvSupportPath)));
    }

    private boolean isNonExistentOrEmptyDir(File dir) {
        if (!dir.exists()) {
            // Directory does not exist
            return true;
        }

        if (!dir.isDirectory()) {
            // The path exists but is not a directory
            return false;
        }

        // Check if the directory is empty
        String[] contents = dir.list();
        return contents == null || contents.length == 0;
    }

    private void assertStaged(File expectedRoot, File actual, Set<String> expectedHabushuDirNames) {
        try {
            // find the subdirectory of the source that matches the stage anchor of the mojo
            // File expected = getMatchingDir(actual.getName(), expectedRoot);

            // relativize
            Set<Path> actualFiles = getRelativizedPaths(actual);



            assertFile(actualFiles, "test-monorepo/extensions/extensions-python-dep-X/src/python_dep_x/python_dep_x.py");
            assertFile(actualFiles, "test-monorepo/extensions/extensions-python-dep-X/pyproject.toml");
            assertFile(actualFiles, "test-monorepo/extensions/extensions-python-dep-X/README.md");
            assertFile(actualFiles, "test-monorepo/foundation/foundation-python-dep-Y/src/python_dep_x/python_dep_y.py");
            assertFile(actualFiles, "test-monorepo/foundation/foundation-python-dep-Y/pyproject.toml");
            assertFile(actualFiles, "test-monorepo/foundation/foundation-python-dep-Y/README.md");

        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static void assertFile(Set<Path> actualFiles, String path) {
        Assertions.assertTrue(actualFiles.contains(Paths.get(path)), "Could not find: " + path);
    }

    private boolean filesHaveSameContent(File expectedFile, File actualFile) throws IOException {
        InputStream expectedStream = new FileInputStream(expectedFile);
        InputStream actualStream = new FileInputStream(actualFile);
        return IOUtils.contentEquals(expectedStream, actualStream);
    }

    private Set<Path> getRelativizedPaths(File rootDir) throws IOException {
        try (Stream<Path> paths = Files.walk(rootDir.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(rootDir.toPath()::relativize)
                    .collect(Collectors.toSet());
        }
    }

    private File getMatchingDir(String name, File startDir) {
        List<File> matches = new ArrayList<>();
        try {
            List<File> contents = walkDirectory(startDir.toPath());
            for (File file : contents) {
                if (file.toPath().toString().endsWith(name) && file.isDirectory()) {
                    matches.add(file);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
        if (matches.size() != 1) {
            throw new RuntimeException();
        } else {
            return matches.get(0);
        }
    }

    private static List<File> walkDirectory(Path startDir) throws IOException {
        try (Stream<Path> stream = Files.walk(startDir)) {
            return stream.map(Path::toFile).collect(Collectors.toList());
        }
    }
}
