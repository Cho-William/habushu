package org.technologybrewery.habushu;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
            this.anchorSourceDirectory = getProjectRoot(this.session.getCurrentProject());
        }

        Set<MavenProject> habushuProjects = getHabushuProjects();
        try {
            copyRelevant(habushuProjects);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void copyRelevant(Set<MavenProject> habushuProjects) throws IOException {
        File destDir = new File(getSession().getCurrentProject().getBuild().getDirectory()
                + "/venv-support/" + this.anchorSourceDirectory.getName());
        this.anchorOutputDirectory = destDir;

        for (MavenProject project : habushuProjects) {
            Path habushuDir = project.getFile().getParentFile().toPath();
            Path a = this.anchorSourceDirectory.toPath().relativize(habushuDir);
            if (!destDir.exists()) {
                FileUtils.forceMkdir(destDir);
            }

            FileFilter fileFilter = file -> {
                if (file.isDirectory() && blackListCheck(file)) {
                    return false;
                } else {
                    return true;
                }
            };

            FileUtils.copyDirectory(habushuDir.toFile(), destDir.toPath().resolve(a).toFile(), fileFilter);
        }
    }

    private boolean blackListCheck(File file) {
        boolean returnBool;
        returnBool = file.getName().equals(".venv");
        returnBool = returnBool || file.getName().equals("dist");
        returnBool = returnBool || file.getName().equals("target");
        return returnBool;
    }

    protected File getProjectRoot(MavenProject project) {
        MavenProject lastValidProject = project;

        // Traverse up the parent hierarchy
        while (lastValidProject.getModel().getParent() != null) {
            MavenProject parentProject = lastValidProject.getParent();
            if (parentProject != null) {
                // Check if the parent's POM file exists
                if (parentProject.getBasedir() != null) {
                    // Update the last valid project
                    lastValidProject = parentProject;
                } else {
                    // Stop if the parent POM does not exist
                    break;
                }
            } else {
                // Stop if there is no parent project
                break;
            }
        }

        return lastValidProject.getBasedir();
    }

    protected Set<MavenProject> getHabushuProjects() {
        Set<MavenProject> habushuProjects = new HashSet<>();
        for (MavenProject project : getSession().getProjects()) {
            if (Objects.equals(project.getModel().getPackaging(), HABUSHU)) {
                habushuProjects.add(project);
            }
        }
        return habushuProjects;
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
