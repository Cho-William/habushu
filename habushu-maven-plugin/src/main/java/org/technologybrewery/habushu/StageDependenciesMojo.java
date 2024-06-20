package org.technologybrewery.habushu;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
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
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.getCommonPrefix;

/**
 * Helper mojo that stages the source files of a monorepo dependency
 * to the target directory along with the source file of
 * any transitive path-based dependencies
 * {@link LifecyclePhase#VALIDATE} build phase.
 *
 * @throws HabushuException
 */

// TODO: check that this only runs if the execution is declared, and does not run by default with Habushu lifecycle
@Mojo(name = "stage-dependencies", defaultPhase = LifecyclePhase.VALIDATE)
public class StageDependenciesMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(StageDependenciesMojo.class);

    @Component
    protected MavenSession session;

    protected File anchorSourceDirectory;
    protected File anchorOutputDirectory;

    protected final String HABUSHU = "habushu";

    protected MavenSession getSession() {
        return this.session;
    }

    protected void setSession(MavenSession session) {
        this.session = session;
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        Set<MavenProject> habushuProjects = getHabushuProjects();
        this.anchorSourceDirectory = calculateAnchorDirectory(habushuProjects);
        try {
            copyRelevant(anchorSourceDirectory, habushuProjects);
        } catch (Exception e) {
            throw new MojoExecutionException("");
        }
        String s = "";
        s += "asdf";
    }

    private void copyRelevant(File anchorDirectory, Set<MavenProject> habushuProjects) throws IOException {
        File destDir = new File(getSession().getCurrentProject().getBuild().getDirectory() + "/venv-support");
        setAnchorOutputDirectory(destDir);

        Set<IOFileFilter> habushuDirsFilters = new HashSet<>();
        for (MavenProject project : habushuProjects) {
            habushuDirsFilters.add(FileFilterUtils.nameFileFilter(project.getFile().getParentFile().getName()));
        }




        // Define what to include
        IOFileFilter whiteListFilter = FileFilterUtils.or(
                FileFilterUtils.nameFileFilter("poetry.lock"),
                FileFilterUtils.nameFileFilter("pyproject.toml"),
                FileFilterUtils.nameFileFilter("README.md")
        );

        IOFileFilter whiteListDirFilter = FileFilterUtils.or(
                habushuDirsFilters.toArray(new IOFileFilter[0])
        );


        // Get the files to be copied
        Collection<File> filesToCopy = FileUtils.listFilesAndDirs(anchorDirectory, FileFilterUtils.trueFileFilter(), whiteListDirFilter);

        // Copy the filtered files and directories
        for (File file : filesToCopy) {
            File destFile = new File(destDir, anchorDirectory.toURI().relativize(file.toURI()).getPath());
            if (file.isDirectory()) {
                FileUtils.forceMkdir(destFile);
            } else {
                FileUtils.copyFile(file, destFile);
            }
        }

    }

    protected File calculateAnchorDirectory(Set<MavenProject> habushuProjects) {
        ArrayList<String> habushuDirectories = new ArrayList<>();
        for (MavenProject project : habushuProjects) {
            habushuDirectories.add(project.getFile().toString());
        }
        return new File(getCommonPrefix(habushuDirectories.toArray(new String[0])));
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

    private void mockSuceess() {
        File source = new File("src/test/resources/stage-dependencies"
                + "/default-single-monorepo-dep/test-monorepo"
        );
        File destination = new File("target/test-classes/stage-dependencies"
                + "/default-single-monorepo-dep/test-monorepo/extensions/extensions-monorepo-dep-consuming-application"
                + "/target/venv-support/test-monorepo"
        );
        setAnchorSourceDirectory(destination);
        try {
            FileUtils.copyDirectory(source, destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Overriding to allow execution in non-habushu projects
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute();
    }

    public File getAnchorSourceDirectory() {
        return anchorSourceDirectory;
    }

    protected void setAnchorSourceDirectory(File anchorSourceDirectory) {
        this.anchorSourceDirectory = anchorSourceDirectory;
    }

    public File getAnchorOutputDirectory() {
        return anchorOutputDirectory;
    }

    protected void setAnchorOutputDirectory(File anchorOutputDirectory) {
        this.anchorOutputDirectory = anchorOutputDirectory;
    }
}
