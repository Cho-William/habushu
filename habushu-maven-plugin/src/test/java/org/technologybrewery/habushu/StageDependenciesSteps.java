package org.technologybrewery.habushu;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

public class StageDependenciesSteps {

    protected String mavenProjectPomPath;

    private final HabushuMojoTestCase mojoTestCase = new HabushuMojoTestCase();

    private StageDependenciesMojo mojo;

    @Before("@stageDependencies")
    public void configureMavenTestSession() throws Exception {
        // important for registering Habushu's mojos to AbstractTestCase.mojoDescriptors
        mojoTestCase.configurePluginTestHarness();
    }

    @After("@stageDependencies")
    public void tearDownMavenPluginTestHarness() throws Exception {
        mojoTestCase.tearDownPluginTestHarness();
    }

    @Given("a single Habushu-type dependency")
    public void a_single_habushu_type_dependency() throws Exception {
        mavenProjectPomPath = "src/test/resources/stage-dependencies/default-single-monorepo-dep/"
                + "test-monorepo/extensions/extensions-monorepo-dep-consuming-application/pom.xml";
        mojo = (StageDependenciesMojo) mojoTestCase.lookupConfiguredMojo(
            new File(mavenProjectPomPath), "stage-dependencies"
        );
    }

    @When("the stage-dependencies goal is executed")
    public void the_stage_dependencies_goal_is_executed() throws MojoExecutionException, MojoFailureException {

    }

    @Then("the dependency source files are staged in the build directory")
    public void the_dependency_source_files_are_staged_in_the_build_directory() {
    }

    @And("all transitive Habushu-type dependency source files are staged in the build directory")
    public void all_transitive_habushu_type_dependency_source_files_are_staged_in_the_build_directory() {

    }

    @And("the relative directory structure is preserved")
    public void the_relative_directory_structure_is_preserved() {

    }

    @Given("no Habushu-type dependencies")
    public void no_habushu_type_dependencies() {

    }

    @Then("no source files are staged in the build directory")
    public void no_source_files_are_staged_in_the_build_directory() {
    }
}
