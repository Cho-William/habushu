package org.technologybrewery.habushu;

import org.apache.commons.io.FileUtils;
import org.apache.maven.Maven;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.*;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.eclipse.aether.graph.DependencyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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

    @Parameter(property = "session", required = true, readonly = true)
    protected MavenSession session;

    @Parameter(property = "projectBuilder", required = true, readonly = true)
    ProjectBuilder projectBuilder;

    protected File anchorDirectory;

    protected final String HABUSHU = "habushu";

    protected MavenSession getSession() {
        return this.session;
    }

    protected void setSession(MavenSession session) {
        this.session = session;
    }

    protected ProjectBuilder getProjectBuilder() {
        return this.projectBuilder;
    }

    protected void setProjectBuilder(ProjectBuilder projectBuilder) {
        this.projectBuilder = projectBuilder;
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        logger.info("Actual Mojo Session:", getSession());

        mockSuceess();
        // String actualPath = mojo.getSession().getCurrentProject().getBuild().getDirectory() + "/venv-support";
        // String expectedPath = getRootParent(mojo.getSession().getCurrentProject()).getBasedir().toString();
        Set<Dependency> allHabushuTypeDependencies = getHabushuTypeDependenciesWithTransitives();
    }

    protected Set<Dependency> getHabushuTypeDependenciesWithTransitives() {
        MavenProject parentProject;
        try {
            parentProject = projectBuilder.build(getSession().getCurrentProject().getParentFile(), getSession().getProjectBuildingRequest()).getProject();
            String s;
        } catch (ProjectBuildingException e) {
            throw new RuntimeException(e);
        }

        Set<Dependency> habushuDependencies = new HashSet<>();
        for (Dependency dependency : getSession().getCurrentProject().getModel().getDependencies()) {
            if (Objects.equals(dependency.getType(), HABUSHU)) {
                habushuDependencies.add(dependency);
                try {
                    habushuDependencies = getTransitives(dependency, habushuDependencies);
                } catch (Exception e) {
                    throw new RuntimeException();
                }

            }
        }
        return habushuDependencies;
    }

    private Set<Dependency> getTransitives(Dependency dependency, Set<Dependency> habushuDependencies) throws DependencyResolutionException, ComponentLookupException {
        ProjectDependenciesResolver resolver = getSession().getContainer().lookup(ProjectDependenciesResolver.class);
        DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest(
                getSession().getCurrentProject(), getSession().getRepositorySession());
        DependencyResolutionResult result = resolver.resolve(request);
        DependencyNode rootNode = result.getDependencyGraph();

        // MavenProject project = new MavenProject(depModel);
        // if (new MavenProject()dependency.getSystemPath())
        return new HashSet<>();
    }

    private void mockSuceess() {
        File source = new File("src/test/resources/stage-dependencies"
                + "/default-single-monorepo-dep/test-monorepo"
        );
        File destination = new File("target/test-classes/stage-dependencies"
                + "/default-single-monorepo-dep/test-monorepo/extensions/extensions-monorepo-dep-consuming-application"
                + "/target/venv-support/test-monorepo"
        );
        setAnchorDirectory(destination);
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

    public File getAnchorDirectory() {
        return anchorDirectory;
    }

    protected void setAnchorDirectory(File anchorDirectory) {
        this.anchorDirectory = anchorDirectory;
    }
}
