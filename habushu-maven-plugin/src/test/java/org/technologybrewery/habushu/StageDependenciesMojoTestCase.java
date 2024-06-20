package org.technologybrewery.habushu;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.*;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.*;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.execution.MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wraps the default behavior provided by the Maven plugin testing harness through {@link AbstractMojoTestCase} to
 * set standard Maven defaults on any {@link Mojo}s that are created and reduce the amount of mock stubs required.
 * <p>
 * This class is largely adapted from the testing approach developed by the license-audit-maven-plugin's
 * {@code BetterAbstractMojoTestCase} (https://github.com/ahgittin/license-audit-maven-plugin)
 */
public class StageDependenciesMojoTestCase extends AbstractMojoTestCase {

    private static final Logger logger = LoggerFactory.getLogger(StageDependenciesMojoTestCase.class);

    private List<File> mavenProjectFiles = new ArrayList<>();

    public void configurePluginTestHarness() throws Exception {
        super.setUp();
    }

    public void tearDownPluginTestHarness() throws Exception {
        super.tearDown();
    }

    /**
     * Creates a new {@link MavenSession} with default configurations relevant for testing the plugin.
     *
     * @return MavenSession
     */
    public MavenSession newDefaultMavenSession() {
        try {
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            MavenExecutionResult result = new DefaultMavenExecutionResult();

            // Populates sensible defaults, including repository basedir and remote repos
            MavenExecutionRequestPopulator populator = getContainer().lookup(MavenExecutionRequestPopulator.class);
            populator.populateDefaults(request);

            // Enables the usage of Java system properties for interpolation and profile activation
            request.setSystemProperties(System.getProperties());

            request.setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

            // Ensures that the repo session in the maven session has a repo manager and points to the local repo
            DefaultMaven maven = (DefaultMaven) getContainer().lookup(Maven.class);
            DefaultRepositorySystemSession repoSession =
                (DefaultRepositorySystemSession) maven.newRepositorySession(request);
            repoSession.setLocalRepositoryManager(
                new SimpleLocalRepositoryManagerFactory().newInstance(
                    repoSession,
                    new LocalRepository(request.getLocalRepository().getBasedir())
                )
            );

            // instantiate and seed the session, so it can be injected during initialization
            @SuppressWarnings("deprecation")
            MavenSession session = new MavenSession(getContainer(), repoSession, request, result);
            SessionScope sessionScope = getContainer().lookup(SessionScope.class);
            sessionScope.enter();
            sessionScope.seed(MavenSession.class, session);

            logger.info("TestCase Session with Make Behavior set:", session);
            return session;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Overrides super's {@link #newMavenSession(MavenProject)} to delegate to
     * the new {@link #newDefaultMavenSession()} introduced in {@link StageDependenciesMojoTestCase},
     * which sets the defaults that are normally expected by Maven.
     */
    @Override
    protected MavenSession newMavenSession(MavenProject project) {
        MavenSession session = newDefaultMavenSession();
        ArrayList<MavenProject> mavenProjects = new ArrayList<>();

        try {
            ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);

            for(File mavenProjectFile: this.mavenProjectFiles) {
                mavenProjects.add(projectBuilder.build(mavenProjectFile, session.getProjectBuildingRequest()).getProject());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        session.setProjects(mavenProjects);
        session.setCurrentProject(project);

        return session;
    }

    public void addMavenProjectFile(File mavenProjectFile) {
        this.mavenProjectFiles.add(mavenProjectFile);
    }

    public void clearMavenProjectFiles() {
        this.mavenProjectFiles = new ArrayList<>();
    }

    /**
     * Overloads super's {@link #lookupConfiguredMojo(MavenProject, String)}
     * to ingest the given {@code pom.xml} {@link File} instead of a {@link MavenProject}.
     * Creates and appropriately configures the {@link Mojo} responsible for executing
     * the plugin goal as defined within the given {@code pom.xml} {@link File}.
     *
     * @param pom  {@code pom.xml} file defining desired plugin and configuration to test.
     * @param goal target plugin goal for which to create the associated {@link Mojo}
     * @return
     * @throws Exception
     */
    public Mojo lookupConfiguredMojo(File pom, String goal) throws Exception {
        assertNotNull(pom);
        assertTrue(pom.exists());

        // how can I add -am to this building request or do I just do the root project pom for testing?
        MavenSession session = newDefaultMavenSession();
        ProjectBuildingRequest buildingRequest = session.getProjectBuildingRequest();
        buildingRequest.setResolveDependencies(true);
        ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);

        MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();
//        StageDependenciesMojo mojo = (StageDependenciesMojo) lookupConfiguredMojo(project, goal);
//        mojo.getSession().getRequest().setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

//        ProjectDependenciesResolver resolver = lookup(ProjectDependenciesResolver.class);
//        DependencyResolutionResult result = resolver.resolve(
//                new DefaultDependencyResolutionRequest(project, buildingRequest.getRepositorySession()));
//        DependencyNode rootNode = result.getDependencyGraph();


        StageDependenciesMojo mojo = lookupConfiguredMojo(project, goal);
        //mojo.setProjectBuilder(projectBuilder);
        return mojo;
    }



}
