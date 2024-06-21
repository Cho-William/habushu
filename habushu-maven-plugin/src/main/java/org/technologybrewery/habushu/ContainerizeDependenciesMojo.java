package org.technologybrewery.habushu;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper mojo that stages the source files of a monorepo dependency
 * to the target directory along with the source file of
 * any transitive path-based dependencies.
 * Additionally, inserts relevant Docker instructions to establish the virtual environment.
 */

@Mojo(name = "containerize-dependencies", defaultPhase = LifecyclePhase.VALIDATE)
public class ContainerizeDependenciesMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(ContainerizeDependenciesMojo.class);

    @Component
    protected MavenSession session;

    protected File anchorSourceDirectory;
    protected File anchorOutputDirectory;
    protected final String HABUSHU = "habushu";

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

        Set<MavenProject> habushuProjects = getHabushuProjects();
        try {
            copyRelevant(habushuProjects);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies the relevant source files by leveraging {@link FileSet}s to filter appropriately.
     * @param habushuProjects corresponding projects of the pom's habushu-type dependencies
     * @throws IOException
     */
    private void copyRelevant(Set<MavenProject> habushuProjects) throws IOException {
        this.anchorOutputDirectory = new File(project.getBuild().getDirectory()
                + "/venv-support/" + this.anchorSourceDirectory.getName());
        Path srcRoot = this.anchorSourceDirectory.toPath();
        Path destRoot = this.anchorOutputDirectory.toPath();

        Map<Path, FileSet> dependencyFileSets = new HashMap<>();
        for (MavenProject project : habushuProjects) {
            Path projectPath = project.getFile().getParentFile().toPath();
            Path relativeProjectPath = srcRoot.relativize(projectPath);
            FileSet fileSet = getDefaultFileSet();
            fileSet.setDirectory(projectPath.toString());
            dependencyFileSets.put(relativeProjectPath, fileSet);
        }

        FileSetManager fileSetManager = new FileSetManager();
        for (Path project : dependencyFileSets.keySet()) {
            FileSet fileSet = dependencyFileSets.get(project);
            logger.info("Copying {} files from {}",
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

    private FileSet getDefaultFileSet() {
        FileSet fileSet = new FileSet();
        fileSet.addExclude("target/**");
        fileSet.addExclude(".venv/**");
        fileSet.addExclude("dist/**");
        return fileSet;
    }

    /**
     * Checks listed habushu-type dependencies against the set of projects included in the Maven build's session
     * @return the corresponding Maven projects that match the habushu-type dependencies
     */
    protected Set<MavenProject> getHabushuProjects() {
        HashSet<MavenProject> habushuProjects = new HashSet<>();
        collectHabushuDependenciesAsProjects(project, habushuProjects);
        return habushuProjects;
    }

    /**
     * Collects the projects of the habushu-type dependencies and adds them to the given project set
     * @param currentProject the project to interrogate the habushu-type dependencies against
     * @param habushuProjects the set to append to with matching projects
     */
    private void collectHabushuDependenciesAsProjects(MavenProject currentProject, Set<MavenProject> habushuProjects) {
        Set<String> habushuDeps = currentProject.getDependencies().stream()
                .filter(d -> HABUSHU.equals(d.getType()))
                .map(ContainerizeDependenciesMojo::toGav)
                .collect(Collectors.toSet());
        for (MavenProject project : getSession().getProjects()) {
            if (habushuDeps.contains(toGav(project))) {
                logger.info("Found habushu-type dependency's project {}.", project);
                habushuProjects.add(project);
                collectHabushuDependenciesAsProjects(project, habushuProjects);
            }
        }
    }

    private static String toGav(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
    }
    private static String toGav(MavenProject project) {
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
