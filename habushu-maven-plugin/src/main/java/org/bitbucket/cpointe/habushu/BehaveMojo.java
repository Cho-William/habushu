package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes behave for Cucumber testing in python following the standard behave
 * structure of a features directory.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class BehaveMojo extends AbstractHabushuMojo {

	private static final Logger logger = LoggerFactory.getLogger(BehaveMojo.class);

	/**
	 * Folder in which python unit test files are located.
	 */
	@Parameter(property = "pythonTestDirectory", required = true, defaultValue = "${project.basedir}/src/test/python")
	protected File pythonTestDirectory;
	
	/**
	 * The base directory of the project.
	 */
	@Parameter(property = "baseDirectory", required = true, defaultValue = "${project.basedir}")
	protected File baseDirectory;

	/**
	 * Additional options that should be passed to the behave command.
	 */
	@Parameter(property = "cucumber.options", required = false)
	protected String cucumberOptions;

	/**
	 * By default, exclude any scenario or feature file tagged with '@manual'.
	 */
	@Parameter(property = "excludeManualTag", required = true, defaultValue = "true")
	protected boolean excludeManualTag;

	/**
	 * Set this to "true" to skip running tests. Its use is NOT RECOMMENDED, but
	 * quite convenient on occasion.
	 */
	@Parameter(property = "skipTests", defaultValue = "false")
	protected boolean skipTests;

	/**
	 * The output directory into which to copy the resources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/"
			+ AbstractHabushuMojo.DEFAULT_TEST_STAGING_FOLDER, required = true)
	private File outputDirectory;

	/**
	 * The path to this environment's behave dependency.
	 */
	@Parameter(property = "pathToBehave", defaultValue = "${project.build.directory}/virtualenvs/${project.artifactId}/bin/behave", required = true)
	private String pathToBehave;

	/**
	 * The generated shell script that uses behave to run the tests.
	 */
	@Parameter(defaultValue = "${project.build.directory}/run-tests.sh", property = "runTestsScript", required = false)
	private File runTestsScript;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		boolean hasTests = false;
		File behaveDirectory = new File(pythonTestDirectory, "features");
		if (!skipTests && behaveDirectory.exists()) {
			hasTests = hasTestArtifactsToProcess(behaveDirectory);
		}

		if (hasTests) {
			verifyBehaveExistsInEnvironment();

			HabushuUtil.createFileAndGivePermissions(runTestsScript);
			writeCommandsToRunTestsScript();

			logger.info("-------------------------------------------------------");
			logger.info("T E S T S");
			logger.info("-------------------------------------------------------");

			HabushuUtil.runBashScript(runTestsScript.getAbsolutePath(), null, false);
			
		} else if (skipTests) {
			logger.info("Tests are skipped.");
		} else {
			logger.info("No tests found in {}", getCanonicalPathForFile(behaveDirectory));

		}

	}

	private void writeCommandsToRunTestsScript() {
		File behaveDirectory = new File(pythonTestDirectory, "features");

		StringBuilder commandList = new StringBuilder();
		commandList.append("#!/bin/bash" + "\n");
		commandList.append("cd " + baseDirectory.getAbsolutePath() + "\n");
		commandList.append("source " + pathToActivationScript + "\n");

		StringBuilder behaveCommand = new StringBuilder();
		behaveCommand.append(pathToBehave);
		behaveCommand.append(" " + getCanonicalPathForFile(behaveDirectory));

		if (excludeManualTag) {
			behaveCommand.append(" --tags=-manual");
		}

		if (StringUtils.isNotBlank(cucumberOptions)) {
			behaveCommand.append(" " + cucumberOptions);
		}

		commandList.append(behaveCommand.toString());

		logger.debug("To run command manually, use the bash script located at {}.", runTestsScript.getAbsolutePath());
		HabushuUtil.writeLinesToFile(commandList.toString(), runTestsScript.getAbsolutePath());
	}

	private boolean hasTestArtifactsToProcess(File behaveDirectory) {
		boolean hasTests;
		IOFileFilter fileExtensionFilter = new WildcardFileFilter("*.*");
		IOFileFilter directoryFilter = TrueFileFilter.INSTANCE; // any directory
		Collection<File> testFiles = FileUtils.listFiles(behaveDirectory, fileExtensionFilter, directoryFilter);
		logger.debug("{} test artifacts found", testFiles.size());
		hasTests = !testFiles.isEmpty();
		return hasTests;
	}

	private void verifyBehaveExistsInEnvironment() {
		VirtualEnvFileHelper venvFileHelper = new VirtualEnvFileHelper(venvDependencyFile);
		List<String> dependencies = venvFileHelper.readDependencyListFromFile();

		boolean behaving = false;
		for (String dependency : dependencies) {
			if (StringUtils.startsWith(dependency, "behave")) {
				behaving = true;
			}
		}

		if (logger.isDebugEnabled()) {
			// Check the environment dependency list and output results for logging
			String pathToPip = pathToVirtualEnvironment + "/bin/pip";
			VenvExecutor executor = createExecutorWithDirectory(venvDirectory, pathToPip + " freeze");
			executor.executeAndRedirectOutput(logger);
		}

		if (!behaving) {
			logger.error(
					"Your venv environment MUST contain a dependency to the 'behave' package to support habushu's behave functionality.");
			logger.error("Please update {} as follows:", getCanonicalPathForFile(venvDependencyFile));
			logger.error("");
			logger.error("\tdependencies:");
			logger.error("\t    - ...");
			logger.error("\t    - behave   <----------  ******** ADD THIS ********");
			logger.error("");

			throw new HabushuException("'behave' package MUST be a dependency in your venv environment configuration!");
		}
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
}
