package org.technologybrewery.habushu;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerizeDepsSteps {

    protected String targetDefaultNoMonorepoDepPath = "target/test-classes/containerize-dependencies/"
            + "default-no-monorepo-dep/test-monorepo";
    protected String targetDefaultSingleMonorepoDepPath = "target/test-classes/containerize-dependencies/"
            + "default-single-monorepo-dep/test-monorepo";
    protected String mavenProjectPath;
    private final String POM_FILE = "pom.xml";

    private final ContainerizeDepsMojoTestWrapper mojoTestCase = new ContainerizeDepsMojoTestWrapper();

    private ContainerizeDepsMojo mojo;

    @Before("@containerizeDependencies")
    public void configureMavenTestSession() throws Exception {
        // important for registering Habushu's mojos to AbstractTestCase.mojoDescriptors,
        // which ensures that lookupConfiguredMojo will return the configured mojo
        mojoTestCase.configurePluginTestHarness();
    }

    @After("@containerizeDependencies")
    public void tearDownMavenPluginTestHarness() throws Exception {
        mojoTestCase.clearMavenProjectFiles();
        mojoTestCase.tearDownPluginTestHarness();
    }

    @Given("a single dependency with packaging type habushu")
    public void a_single_dependency_with_packaging_type_habushu() throws Exception {
        mavenProjectPath = targetDefaultSingleMonorepoDepPath + "/extensions/extensions-monorepo-dep-consuming-application";

        // enables us to mock a maven build with the --also-make flag
        mojoTestCase.addMavenProjectFile(new File(targetDefaultSingleMonorepoDepPath + "/extensions/extensions-python-dep-X/pom.xml"));
        mojoTestCase.addMavenProjectFile(new File(targetDefaultSingleMonorepoDepPath + "/foundation/foundation-python-dep-Y/pom.xml"));

        mojo = (ContainerizeDepsMojo) mojoTestCase.lookupConfiguredMojo(
                new File(mavenProjectPath, POM_FILE), "containerize-dependencies"
        );
        mojo.setAnchorSourceDirectory(new File("target/test-classes/containerize-dependencies/default-single-monorepo-dep/test-monorepo").getAbsoluteFile());
        mojo.session.getRequest().setBaseDirectory(new File(targetDefaultSingleMonorepoDepPath));

    }

    @When("the containerize-dependencies goal is executed")
    public void the_containerize_dependencies_goal_is_executed() throws MojoExecutionException, MojoFailureException {
        mojo.execute();
    }

    @Then("the source files of the dependency and transitive Habushu-type dependencies are staged for containerization")
    public void
    the_source_files_of_the_dependency_and_transitive_habushu_type_dependencies_are_staged_for_containerization() {
        assertStaged(mojo.getAnchorOutputDirectory().toPath());
    }

    @Given("no Habushu-type dependencies")
    public void no_habushu_type_dependencies() throws Exception {
        mavenProjectPath = targetDefaultNoMonorepoDepPath + "/no-monorepo-dep-application";
        mojo = (ContainerizeDepsMojo) mojoTestCase.lookupConfiguredMojo(
                new File(mavenProjectPath, POM_FILE), "containerize-dependencies"
        );
        mojo.session.getRequest().setBaseDirectory(new File(targetDefaultNoMonorepoDepPath));
    }

    @Then("no source files are staged in the build directory")
    public void no_source_files_are_staged_in_the_build_directory() {
        String containerizeSupportPath = mojo.getSession().getCurrentProject().getBuild().getDirectory()
                + "/containerize-support";
        Assertions.assertTrue(isNonExistentOrEmptyDir(new File(containerizeSupportPath)));
    }

    private boolean isNonExistentOrEmptyDir(File dir) {
        // Directory does not exist
        if (!dir.exists()) {
            return true;
        }

        // The path exists but is not a directory
        if (!dir.isDirectory()) {
            return false;
        }

        // Check if the directory is empty
        String[] contents = dir.list();
        return contents == null || contents.length == 0;
    }

    private void assertStaged(Path actual) {
        Set<Path> actualFiles;
        try {
            actualFiles = getRelativizedPaths(actual);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        assertFile(actualFiles, "extensions/extensions-python-dep-X/src/python_dep_x/python_dep_x.py");
        assertFile(actualFiles, "extensions/extensions-python-dep-X/pyproject.toml");
        assertFile(actualFiles, "extensions/extensions-python-dep-X/README.md");
        assertFile(actualFiles, "foundation/foundation-python-dep-Y/src/python_dep_y/python_dep_y.py");
        assertFile(actualFiles, "foundation/foundation-python-dep-Y/pyproject.toml");
        assertFile(actualFiles, "foundation/foundation-python-dep-Y/README.md");
    }

    private static void assertFile(Set<Path> actualFiles, String path) {
        Assertions.assertTrue(actualFiles.contains(Paths.get(path)), "Could not find: " + path);
    }

    private Set<Path> getRelativizedPaths(Path rootDir) throws IOException {
        try (Stream<Path> paths = Files.walk(rootDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(rootDir::relativize)
                    .collect(Collectors.toSet());
        }
    }

}
