package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DevDependenciesGroupMigrationSteps extends AbstractGroupMigrationTestSteps {

    public DevDependenciesGroupMigrationSteps() {
        super();
        testTomlFileDirectory = new File("./target/test-classes/migration/dev-dependencies");
    }

    @Given("an existing pyproject.toml file with two dev-dependencies dependencies each in the tool.poetry.dev-dependencies and tool.poetry.group.dev.dependencies groups")
    public void an_existing_pyproject_toml_file_with_two_dev_dependencies_dependencies_each_in_the_tool_poetry_dev_dependencies_and_tool_poetry_group_dev_dependencies_groups() {
        pyProjectToml = new File(testTomlFileDirectory, "with-dependencies-in-both-groups.toml");
    }

    @Given("an existing pyproject.toml without any dev-dependencies dependencies")
    public void an_existing_pyproject_toml_without_any_dev_dependencies_dependencies() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-dev-dependencies-group.toml");
    }

    @When("the Habushu dev dependency migration executes")
    public void habushu_migrations_execute() {
        DevDependenciesGroupMigration migration = new DevDependenciesGroupMigration();
        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("{int} pyproject.toml dependencies exist in the tool.poetry.dev-dependencies group")
    public void pyproject_toml_dependencies_exist_in_the_tool_poetry_dev_dependencies_group(Integer expectedDevDashDependenciesGroupDependencies) {
        verifyExecutionOccurred();
        try (FileConfig tomlFileConfig = FileConfig.of(pyProjectToml)) {
            tomlFileConfig.load();

            Optional<Config> toolPoetryDevDependencies = tomlFileConfig.getOptional("tool.poetry.dev-dependencies");
            int numberOfLegacyDependencies = getNumberOfChildren(toolPoetryDevDependencies);
            assertEquals(expectedDevDashDependenciesGroupDependencies, numberOfLegacyDependencies,
                    expectedDevDashDependenciesGroupDependencies + " is an unexpected number of dev-dependencies!");


        }
    }

    @Then("{int} pyproject.toml dependencies exist in the tool.poetry.group.dev.dependencies group")
    public void pyproject_toml_dependencies_exist_in_the_tool_poetry_group_dev_dependencies_group(Integer expectedGroupDevDependencies) {
        verifyExecutionOccurred();
        try (FileConfig tomlFileConfig = FileConfig.of(pyProjectToml)) {
            tomlFileConfig.load();

            Optional<Config> toolPoetryDependencies = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_DEV_DEPENDENCIES);
            int numberOfGroupDevDependencies = getNumberOfChildren(toolPoetryDependencies);
            assertEquals(expectedGroupDevDependencies, numberOfGroupDevDependencies,
                    expectedGroupDevDependencies + " group.dev.depependenies should remain in main group!");
        }
    }

    protected int getNumberOfChildren(Optional<Config> tomlElement) {
        int numberOfLegacyDependencies = 0;
        if (tomlElement.isPresent()) {
            Config foundDependencies = tomlElement.get();
            Map<String, Object> dependencyMap = foundDependencies.valueMap();
            numberOfLegacyDependencies = dependencyMap.size();
        }

        return numberOfLegacyDependencies;
    }

}
