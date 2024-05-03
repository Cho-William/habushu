package org.technologybrewery.habushu;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractValidateMojo extends AbstractHabushuMojo {
    protected static final String LINT_PACKAGE = "pylint";

    protected void runLinter(File lintDirectory, String disabledCheckers, String enabledCheckers, boolean failOnError)
            throws MojoExecutionException {
        runLinter(lintDirectory, disabledCheckers, enabledCheckers, failOnError, null);
    }

    /**
     * Runs the linter on the specified directory with the given checkers and arguments.
     *
     * @param lintDirectory directory to lint
     * @param disabledCheckers comma-separated list of checkers to disable
     * @param enabledCheckers comma-separated list of checkers to enable
     * @param failOnError whether to fail the build if lint errors are found
     * @param extraArgs additional arguments to pass to the linter
     *
     * @throws MojoExecutionException if an error occurs during linting or the linter cannot be installed
     */
    protected void runLinter(File lintDirectory, String disabledCheckers, String enabledCheckers, boolean failOnError,
                             List<String> extraArgs) throws MojoExecutionException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        if (lintDirectory.exists()) {
            List<String> executeLintArgs = new ArrayList<>(Arrays.asList("run", LINT_PACKAGE));
            executeLintArgs.add(getCanonicalPathForFile(lintDirectory));
            if (!poetryHelper.isDependencyInstalled(LINT_PACKAGE)) {
                getLog().info(String.format("%s dependency not specified in pyproject.toml - installing now...",
                        LINT_PACKAGE));
                poetryHelper.installDevelopmentDependency(LINT_PACKAGE);
            }

            if (StringUtils.isNotEmpty(disabledCheckers)) {
                executeLintArgs.addAll(Arrays.asList("--disable", disabledCheckers));
            }

            if (StringUtils.isNotEmpty(enabledCheckers)) {
                executeLintArgs.addAll(Arrays.asList("--enable", enabledCheckers));
            }

            if (CollectionUtils.isNotEmpty(extraArgs)) {
                executeLintArgs.addAll(extraArgs);
            }

            if (!failOnError) {
                executeLintArgs.add("--exit-zero");
            }


            getLog().info("Validating code using Pylint...");
            poetryHelper.executeAndLogOutput(executeLintArgs);
        } else {
            getLog().warn(String.format("Configured linting directory (%s) does not exist - skipping...",
                    lintDirectory));
        }
    }
}
