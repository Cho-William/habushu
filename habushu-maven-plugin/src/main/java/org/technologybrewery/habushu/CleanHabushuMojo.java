package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.clean.CleanMojo;
import org.apache.maven.plugins.clean.Fileset;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Overrides the default {@link CleanMojo} behavior to additionally delete the
 * virtual environment that is created/managed by Poetry if the
 * {@link #deleteVirtualEnv} option is enabled.
 */
@Mojo(name = "clean-habushu", defaultPhase = LifecyclePhase.CLEAN)
public class CleanHabushuMojo extends CleanMojo {

    /**
     * Base directory in which Poetry projects will be located - should always be
     * the basedir of the encapsulating Maven project.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    protected File workingDirectory;

    /**
     * Directory in which Poetry places generated source and wheel archive
     * distributions.
     */
    @Parameter(defaultValue = "${project.basedir}/dist", readonly = true, required = true)
    protected File distDirectory;

    /**
     * Directory in which Maven places build-time artifacts - should NOT include dist items.
     */
    @Parameter(defaultValue = "${project.basedir}/target", readonly = true, required = true)
    protected File targetDirectory;

    /**
     * The packaging type of the current Maven project. If it is not "habushu", then this mojo will be skipped.
     */
    @Parameter(defaultValue = "${project.packaging}", readonly = true, required = true)
    protected String packaging;

    /**
     * Enables the explicit deletion of the virtual environment that is
     * created/managed by Poetry.
     */
    @Parameter(property = "habushu.deleteVirtualEnv", required = true, defaultValue = "false")
    protected boolean deleteVirtualEnv;

    /**
     * The desired version of Python to use.
     */
    @Parameter(defaultValue = PyenvAndPoetrySetup.PYTHON_DEFAULT_VERSION_REQUIREMENT, property = "habushu.pythonVersion")
    protected String pythonVersion;

    /**
     * Should Habushu use pyenv to manage the utilized version of Python?
     */
    @Parameter(defaultValue = "true", property = "habushu.usePyenv")
    protected boolean usePyenv;

    /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    @Parameter(defaultValue = "${project.build.directory}/pyenv-patch-install-python-version.sh", readonly = true)
    private File patchInstallScript;

    /**
     * Indicates whether Habushu should leverage the
     * {@code poetry-monorepo-dependency-plugin} to rewrite any local path
     * dependencies (to other Poetry projects) as versioned packaged dependencies in
     * generated wheel/sdist archives. If {@code true}, Habushu will replace
     * invocations of Poetry's {@code build} and {@code publish} commands in the
     * {@link BuildDeploymentArtifactsMojo} and {@link PublishToPyPiRepoMojo} with
     * the extensions of those commands exposed by the
     * {@code poetry monorepo-dependency-plugin}, which are
     * {@code build-rewrite-path-deps} and {@code publish-rewrite-path-deps}
     * respectively.
     * <p>
     * Typically, this flag will only be {@code true} when deploying/releasing
     * Habushu modules within a CI environment that are part of a monorepo project
     * structure which multiple Poetry projects depend on one another.
     */
    @Parameter(defaultValue = "false", property = "habushu.rewriteLocalPathDepsInArchives")
    protected boolean rewriteLocalPathDepsInArchives;

    /**
     * Whether to configure Poetry's {@code virtualenvs.in-project} value for this project.
     * If configured, virtual environments will be migrated to this approach during the clean phase of the build.
     * <p>
     * While generally easier to find and use for tasks like debugging, having your virtual environment co-located in
     * your project may be less useful for executions like CI builds where you may want to centrally caches virtual
     * environments from a central location.
     */
    @Parameter(defaultValue = "true", property = "habushu.useInProjectVirtualEnvironment")
    protected boolean useInProjectVirtualEnvironment;


    @Override
    public void execute() throws MojoExecutionException {
        if ("habushu".equals(packaging)) {
            clean();
        } else {
            getLog().info("Skipping execution - packaging type is not 'habushu'");
        }
    }

    private void clean() throws MojoExecutionException {
        List<Fileset> filesetsToDelete = new ArrayList<>();
        boolean removeVenvManually = false;

        String virtualEnvFullPath = findCurrentVirtualEnvironmentFullPath();
        virtualEnvFullPath = getCleanVirtualEnvironmentPath(virtualEnvFullPath);

        String inVirtualEnvironmentPath = HabushuUtil.getInProjectVirtualEnvironmentPath(this.workingDirectory);
        File venv = new File(inVirtualEnvironmentPath);

        if (this.useInProjectVirtualEnvironment) {
            getLog().debug("`in-project` virtual environment configured for this project");
            if (StringUtils.isNotBlank(virtualEnvFullPath) && !inVirtualEnvironmentPath.equals(virtualEnvFullPath)) {
                getLog().warn("'in-project' virtual environment is configured, but an external virtual environment was found!");
                getLog().warn("Deleting external virtual environment: " + virtualEnvFullPath);
                deleteVirtualEnv = true;
            }
        } else {
            if (venv.exists()) {
                getLog().warn("'in-project' virtual environment is NOT configured, but an 'in-project' virtual environment was found!");
                getLog().warn("Deleting 'in-project' virtual environment: " + virtualEnvFullPath);
                deleteVirtualEnv = true;
            }
        }

        if (deleteVirtualEnv) {
            if (StringUtils.isBlank(virtualEnvFullPath)) {
                getLog().warn("No Poetry virtual environment was detected for deletion.");
            } else {

                String virtualEnvName = new File(virtualEnvFullPath).getName();
                if (StringUtils.isNotBlank(virtualEnvName)) {
                    PoetryCommandHelper poetryHelper = new PoetryCommandHelper(this.workingDirectory);
                    List<String> arguments = new ArrayList<>();
                    arguments.add("env");
                    arguments.add("remove");
                    if (!".venv".equals(virtualEnvName)) {
                        arguments.add(virtualEnvName);
                    } else {
                        // While Poetry 1.8.3 and lower will unregister the .venv virtual environment, it doesn't
                        // remove it, creating confusion:
                        removeVenvManually = true;
                    }
                    poetryHelper.execute(arguments);
                }
            }
        }

        try {
            Fileset distArchivesFileset = createFileset(distDirectory);
            filesetsToDelete.add(distArchivesFileset);

            Fileset targetArchivesFileset = createFileset(targetDirectory);
            filesetsToDelete.add(targetArchivesFileset);

            if (removeVenvManually) {
                filesetsToDelete.add(createFileset(venv));
            }

        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Could not write to private field in Fileset class.", e);
        }

        getLog().info(String.format("Deleting distribution archives at %s", distDirectory));
        getLog().info(String.format("Deleting target archives at %s", targetDirectory));

        setPrivateParentField("filesets", filesetsToDelete.toArray(new Fileset[0]));
        super.execute();
    }

    /**
     * Removes (Activated) from path in 1.3.x and higher versions.
     *
     * @param virtualEnvFullPath path to clean
     * @return cleaned path
     */
    private static String getCleanVirtualEnvironmentPath(String virtualEnvFullPath) {
        return StringUtils.replace(virtualEnvFullPath, " (Activated)", StringUtils.EMPTY);

    }

    private String findCurrentVirtualEnvironmentFullPath() throws MojoExecutionException {
        String virtualEnvFullPath = null;
        PyenvAndPoetrySetup configureTools = new PyenvAndPoetrySetup(pythonVersion, usePyenv,
                patchInstallScript, workingDirectory, rewriteLocalPathDepsInArchives, getLog());
        configureTools.execute();

        try {
            PoetryCommandHelper poetryHelper = new PoetryCommandHelper(this.workingDirectory);
            virtualEnvFullPath = poetryHelper.execute(Arrays.asList("env", "list", "--full-path"));
        } catch (RuntimeException e) {
            getLog().debug("Could not retrieve Poetry-managed virtual environment path - it likely does not exist",
                    e);
        }

        return virtualEnvFullPath;
    }

    /**
     * Creates a new {@link Fileset} that may be used to identify a set of files
     * that are targeted for deletion by the {@link CleanMojo}.
     *
     * @param directory directory that is desired for deletion.
     * @return
     * @throws IllegalAccessException
     */
    private Fileset createFileset(File directory) throws IllegalAccessException {
        Fileset fileset = new Fileset();
        FieldUtils.writeField(fileset, "directory", directory, true);
        return fileset;
    }

    /**
     * Sets a given field in the parent {@link CleanMojo} with a provided value.
     * This method is needed as {@link CleanMojo} is structured in a way that does
     * not easily facilitate extension.
     *
     * @param fieldName the name of the field in {@link CleanMojo}
     * @param value     the field's intended value
     */
    private void setPrivateParentField(String fieldName, Object value) {
        Field fieldInParentClass;
        try {
            fieldInParentClass = this.getClass().getSuperclass().getDeclaredField(fieldName);
            fieldInParentClass.setAccessible(true);
            fieldInParentClass.set(this, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new HabushuException("Could not write to field in CleanMojo class.", e);
        }

    }
}
