package org.technologybrewery.habushu;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Stages the source files of a monorepo dependency
 * to the target directory along with the source files of
 * any transitive path-based dependencies.
 */
@Mojo(name = "containerize-dependencies", defaultPhase = LifecyclePhase.PACKAGE)
public class ContainerizeDepsMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(ContainerizeDepsMojo.class);

    @Component
    protected MavenSession session;

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
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    @Parameter(defaultValue = "pyenv-patch-install-python-version.sh", readonly = true)
    private String patchInstallScriptRelativeToBuildDirectory;

    /**
     * Working directory relative to the basedir - typically working directory and basedir are synonymous.
     */
    @Parameter(readonly = true, required = true)
    protected String workingDirectoryRelativeToBasedir;

    /**
     * Expected subdirectory of the monorepo dependency's basedir
     * in which Poetry places generated source and wheel archive distributions.
     */
    @Parameter(defaultValue = "/dist", readonly = true, required = true)
    protected String distDirectoryRelativeToBasedir;

    /**
     * Expected subdirectory of the monorepo dependency's basedir
     * in which Maven places build-time artifacts. Should NOT include dist items.
     */
    @Parameter(defaultValue = "/target", readonly = true, required = true)
    protected String targetDirectoryRelativeToBasedir;

    /**
     * Location of where containerization files will be placed.
     */
    @Parameter(defaultValue = "${project.build.directory}/containerize-support", readonly = true, required = true)
    protected File containerizeSupportDirectory;

    /**
     * Upstream directory that houses all necessary monorepo dependencies.
     * Monorepo dependency source files will be copied from here.
     */
    @Parameter(readonly = true, required = true)
    protected File anchorSourceDirectory;

    private File anchorOutputDirectory;

    protected final String HABUSHU = "habushu";
    protected final String FORWARD_SLASH = "/";
    protected final String GLOB_RECURSIVE_ALL = "/**";

    /**
     * Overriding to allow execution in non-habushu projects.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute();
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (this.anchorSourceDirectory == null) {
            this.anchorSourceDirectory = new File(session.getExecutionRootDirectory());
        }

        if (this.workingDirectoryRelativeToBasedir == null) {
            this.workingDirectoryRelativeToBasedir = "";
        }

        Set<MavenProject> habushuProjects = getHabushuProjects();
        try {
            copySourceCode(habushuProjects);
        } catch (IOException e) {
            throw new HabushuException(e);
        }
    }

    /**
     * Copies the relevant source files by leveraging {@link FileSet}s to filter appropriately.
     * @param habushuProjects corresponding projects of the pom's habushu-type dependencies
     * @throws IOException
     * @throws MojoExecutionException
     */
    protected void copySourceCode(Set<MavenProject> habushuProjects) throws IOException, MojoExecutionException {
        this.anchorOutputDirectory = new File(containerizeSupportDirectory
                + FORWARD_SLASH + this.anchorSourceDirectory.getName());
        Path srcRoot = this.anchorSourceDirectory.toPath();
        Path destRoot = this.anchorOutputDirectory.toPath();

        Map<Path, FileSet> dependencyFileSets = new HashMap<>();
        for (MavenProject project : habushuProjects) {
            Path projectPath = getWorkingDirectoryPath(project);
            Path relativeProjectPath = srcRoot.relativize(projectPath);
            FileSet fileSet = getDefaultFileSet(project);
            fileSet.setDirectory(projectPath.toString());
            dependencyFileSets.put(relativeProjectPath, fileSet);
        }

        FileSetManager fileSetManager = new FileSetManager();
        for (Path project : dependencyFileSets.keySet()) {
            FileSet fileSet = dependencyFileSets.get(project);
            logger.info("Staging {} monorepo dependency files from {}.",
                    fileSetManager.getIncludedFiles(fileSet).length,
                    project.getFileName()
            );
            for (String includedFile : fileSetManager.getIncludedFiles(fileSet)) {
                Path relativePath = project.resolve(includedFile);
                Files.createDirectories(destRoot.resolve(relativePath).getParent());
                Files.copy(srcRoot.resolve(relativePath), destRoot.resolve(relativePath));
            }
        }
    }

    private Path getWorkingDirectoryPath(MavenProject project) {
        return project.getBasedir().toPath().resolve(workingDirectoryRelativeToBasedir);
    }

    private FileSet getDefaultFileSet(MavenProject project) throws MojoExecutionException {
        FileSet fileSet = new FileSet();
        fileSet.addExclude(distDirectoryRelativeToBasedir + GLOB_RECURSIVE_ALL);
        fileSet.addExclude(targetDirectoryRelativeToBasedir + GLOB_RECURSIVE_ALL);

        // find the virtual environment path for the given Habushu-packaged project
        String virtualEnvironmentPath = HabushuUtil.findCurrentVirtualEnvironmentFullPath(
                pythonVersion,
                usePyenv,
                new File(project.getBuild().getDirectory() + patchInstallScriptRelativeToBuildDirectory),
                new File(project.getBasedir() + workingDirectoryRelativeToBasedir),
                rewriteLocalPathDepsInArchives,
                getLog()
        );
        virtualEnvironmentPath = HabushuUtil.getCleanVirtualEnvironmentPath(virtualEnvironmentPath);

        // if a valid virtual environment was found, relativize and add to the fileSet for exclusions
        if (virtualEnvironmentPath != null && !virtualEnvironmentPath.isEmpty()
                && new File(virtualEnvironmentPath).isDirectory()) {
            Path venvDirRelativeToBasedir = getWorkingDirectoryPath(project)
                    .relativize(Paths.get(virtualEnvironmentPath));
            fileSet.addExclude(venvDirRelativeToBasedir + GLOB_RECURSIVE_ALL);
        }

        return fileSet;
    }

    /**
     * Checks listed habushu-type dependencies against the set of projects included in the Maven build's session
     * @return the corresponding Maven projects that match the habushu-type dependencies
     */
    protected Set<MavenProject> getHabushuProjects() {
        Set<MavenProject> habushuProjects = new HashSet<>();
        Set<Dependency> directHabushuDeps = session.getCurrentProject().getDependencies().stream()
                .filter(d -> HABUSHU.equals(d.getType()))
                .collect(Collectors.toSet());
        // TODO: modify this exception throw, once support for
        //  more than one direct monorepo dep specification is implemented
        if (directHabushuDeps.size() > 1) {
            throw new HabushuException("More than one habushu-type dependency was found."
                    + "Only one habushu-type dependency should be specified.");
        }

        collectHabushuDependenciesAsProjects(project, habushuProjects);
        return habushuProjects;
    }

    /**
     * Collects the projects with habushu-type dependencies and adds them to the given project set
     * @param currentProject the project to interrogate the habushu-type dependencies against
     * @param habushuProjects the set to append to with matching projects
     */
    protected void collectHabushuDependenciesAsProjects(MavenProject currentProject, Set<MavenProject> habushuProjects) {
        Set<String> habushuDeps = currentProject.getDependencies().stream()
                .filter(d -> HABUSHU.equals(d.getType()))
                .map(ContainerizeDepsMojo::toGav)
                .collect(Collectors.toSet());
        for (MavenProject project : getSession().getProjects()) {
            if (habushuDeps.contains(toGav(project))) {
                logger.info("Found project {} as habushu-type dependency.", project);
                habushuProjects.add(project);
                collectHabushuDependenciesAsProjects(project, habushuProjects);
            }
        }
    }

    protected static String toGav(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
    }
    protected static String toGav(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
    }

    protected MavenSession getSession() {
        return this.session;
    }

    protected void setAnchorSourceDirectory(File anchorSourceDirectory) {
        this.anchorSourceDirectory = anchorSourceDirectory;
    }

    public File getAnchorOutputDirectory() {
        return anchorOutputDirectory;
    }
}
