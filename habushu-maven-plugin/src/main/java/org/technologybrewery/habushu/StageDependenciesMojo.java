package org.technologybrewery.habushu;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.*;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper mojo that stages the source files of a monorepo dependency
 * to the target directory along with the source file of
 * any transitive path-based dependencies
 */

@Mojo(name = "stage-dependencies", defaultPhase = LifecyclePhase.VALIDATE)
public class StageDependenciesMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(StageDependenciesMojo.class);

    @Component
    protected MavenSession session;

    protected File anchorSourceDirectory;
    protected File anchorOutputDirectory;

    protected final String HABUSHU = "habushu";

    /**
     * Overriding to allow execution in non-habushu projects
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute();
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (this.anchorSourceDirectory == null) {
            this.anchorSourceDirectory = getUpstreamRoot(this.session.getCurrentProject());
        }

        Set<MavenProject> habushuProjects = getHabushuProjects();
        try {
            copyRelevant(habushuProjects);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


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
     * Determines the uppermost parent project's directory that exists locally
     * @param project the project that will be examined for its upstream parents
     * @return the uppermost upstream basedir that exists locally
     */
    protected File getUpstreamRoot(MavenProject project) {
//        MavenProject lastValidProject = project;
//
//        // Traverse up the parent hierarchy
//        while (lastValidProject.getModel().getParent() != null) {
//            MavenProject parentProject = lastValidProject.getParent();
//            if (parentProject != null) {
//                // Check if the parent's POM file exists
//                if (parentProject.getBasedir() != null) {
//                    // Update the last valid project
//                    lastValidProject = parentProject;
//                } else {
//                    // Stop if the parent POM does not exist
//                    break;
//                }
//            } else {
//                // Stop if there is no parent project
//                break;
//            }
//        }

        return new File(session.getExecutionRootDirectory());
    }

    /**
     * Checks listed habushu-type dependencies against the set of projects included in the Maven build's session
     * @return the corresponding Maven projects that match the habushu-type dependencies
     */
    protected Set<MavenProject> getHabushuProjects() {
        MavenProject project1 = project;
        HashSet<MavenProject> habushuProjects = new HashSet<>();
        collectHabushuDependencies(project1, habushuProjects);
        return habushuProjects;
    }

    private void collectHabushuDependencies(MavenProject project1, Set<MavenProject> habushuProjects) {
        Set<String> habushuDeps = project1.getDependencies().stream()
                .filter(d -> HABUSHU.equals(d.getType()))
                .map(StageDependenciesMojo::toGav)
                .collect(Collectors.toSet());
        for (MavenProject project : getSession().getProjects()) {
            if (habushuDeps.contains(toGav(project))) {
                habushuProjects.add(project);
                collectHabushuDependencies(project, habushuProjects);
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
