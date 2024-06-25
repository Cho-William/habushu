package org.technologybrewery.habushu.migration;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractGroupMigrationTestSteps {

    protected File testTomlFileDirectory = new File("./target/test-classes/migration");
    protected File pyProjectToml;

    protected boolean shouldExecute;

    protected boolean executionSucceeded;

    protected void verifyExecutionOccurred() {
        assertTrue(shouldExecute, "Migration should have been selected to execute!");
        assertTrue(executionSucceeded, "Migration should have executed successfully!");
    }

}
