package org.technologybrewery.habushu;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RetrieveWheelsSteps {

    private RetrieveWheelsTestMojo mojo;
    private final String targetDirectory = "src/test/resources/testTargetDirectory/";

    @After
    public void cleanUp() {
        HabushuUtil.clearDirectoryFiles(new File(targetDirectory).toPath());
    }

    @Given("a Habushu configuration with no wheel dependencies entries")
    public void a_habushu_configuration_with_no_wheel_dependency_entries() throws Exception {
        mojo = new RetrieveWheelsTestMojo();
        mojo.setWheelDependencies(new ArrayList<>());
    }

    @Given("a Habushu configuration with a wheel dependency on {string} version {string}")
    public void aHabushuConfigurationWithAWheelDependencyOnVersion(String artifact, String version) {
        mojo = new RetrieveWheelsTestMojo();
        List<WheelDependency> wheelDependencies = new ArrayList<>();
        WheelDependency wheelDependency = new WheelDependency();

        wheelDependency.setArtifactId(artifact);
        if (version.equals("null")) {
            wheelDependency.setVersion(null);
        } else {
            wheelDependency.setVersion(version);
        }
        wheelDependency.setTargetDirectory(targetDirectory);

        wheelDependencies.add(wheelDependency);
        mojo.setWheelDependencies(wheelDependencies);
    }
    @And("a Maven project with version set to {string}")
    public void aMavenProjectWithVersionSetTo(String version) {
        MavenProject sampleMavenProject = new MavenProject();
        sampleMavenProject.setVersion(version);
        mojo.project = sampleMavenProject;
    }



    @When("Habushu executes retrieve wheel dependencies")
    public void habushu_executes_retrieve_wheel_dependencies() throws Exception {
        mojo.processWheelDependencies();
    }

    @Then("no wheel artifacts are copied")
    public void no_wheel_artifacts_are_copied() {
        //assert the wheel dependency target directory does not contain the poetry cache wheel artifact
        Assertions.assertTrue(checkIfTargetHasNoWheels(), "Expected no copied wheels, but at least one wheel present.");
    }

    /**
     * Asserts that the wheel artifacts identified by artifactId have been copied exactly into the target directory.
     */
    @Then("the {string} version of the {string} wheels are copied")
    public void theVersionOfTheWheelArtifactSAreCopied(String expectedVersion, String expectedArtifactId) {
        // assert the wheel dependency target directory contains the poetry cache wheel artifact
        Assertions.assertTrue(checkIfWheelsWereCopied(expectedVersion, expectedArtifactId), "Expected the wheel artifact in the target directory, but didn't find it!");
    }

    private boolean checkIfWheelsWereCopied(String expectedVersion, String expectedArtifactId){
        boolean isWheelCopied = false;
        String targetDirectory;
        List<WheelDependency> wheelDependencies = mojo.getWheelDependencies();
        for (WheelDependency wd : wheelDependencies) {
            targetDirectory = wd.getTargetDirectory();
            // get the wheels from the artifact- and version-specific cache directory
            File artifactPoetryCacheDirectory = mojo.getCachedWheelDirectory(expectedArtifactId, expectedVersion);
            List<File> wheelFiles = getWheelFiles(artifactPoetryCacheDirectory);
            // ensure that the target directory has the same wheels
            for (File f : wheelFiles) {
                isWheelCopied = new File(targetDirectory, f.getName()).exists();
            }     
        }  
        return isWheelCopied;      
    }

    private boolean checkIfTargetHasNoWheels() {
        boolean targetHasNoWheels = true;
        List<WheelDependency> wheelDependencies = mojo.getWheelDependencies();
        for (WheelDependency wd : wheelDependencies) {
            File targetDirectory = new File(wd.getTargetDirectory());
            List<File> wheelFiles = getWheelFiles(targetDirectory);
            if (!wheelFiles.isEmpty()) {
                targetHasNoWheels = false;
            }
        }
        return targetHasNoWheels;
    }

    private static List<File> getWheelFiles(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return Collections.emptyList();
        }
        List<File> wheelFiles = Stream.of(directory.listFiles())
                .filter(file -> file.getAbsolutePath().endsWith(".whl"))
                .map(File::getAbsoluteFile)
                .collect(Collectors.toList());
        return wheelFiles;
    }
}
