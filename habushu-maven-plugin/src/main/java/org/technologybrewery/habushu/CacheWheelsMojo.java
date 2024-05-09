package org.technologybrewery.habushu;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.util.HabushuUtil;

/**
 * Helper mojo that handles caching of a wheel dependency,
 * into Poetry cache during the {@link LifecyclePhase#INSTALL} build phase. 
 *
 * @param cacheWheels       A boolean that when implemented will cache a project's 
 *                          wheel files in poetry.
 * 
 * @throws HabushuException
 */
@Mojo(name = "cache-wheels", defaultPhase = LifecyclePhase.INSTALL)
public class CacheWheelsMojo extends AbstractHabushuMojo {
    /**
     * A boolean that when implemented will cache a project's wheel files in poetry.
     */
    @Parameter(property = "habushu.cacheWheels", required = false, defaultValue = "false")
    protected boolean cacheWheels;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (cacheWheels) {
            cacheWheels();
        }
    }

    private void cacheWheels() {
        getLog().info("Processing Cache Wheels..");
        try {
            File wheelSourceDirectory = getProjectBuildDirectory();
            // enforce artifact-specific and version-specific directories for wheels, similar to Maven
            File poetryWheelCacheDirectory = getCachedWheelDirectory(project.getArtifactId(), project.getVersion());
            // conditional will throw an error if cache directory isn't created
            if (poetryWheelCacheDirectory.exists() || poetryWheelCacheDirectory.mkdirs()) {
                List<File> wheelFiles = Stream.of(wheelSourceDirectory.listFiles())
                        .filter(file -> file.getAbsolutePath().endsWith(".whl"))
                        .map(File::getAbsoluteFile)
                        .collect(Collectors.toList());
                // Clearing any files in the Poetry wheel cache directory ensures that the directory
                // only houses wheel(s) that are artifact-specific and version-specific
                HabushuUtil.clearDirectoryFiles(poetryWheelCacheDirectory.toPath());
                for (File file : wheelFiles) {
                    HabushuUtil.copyFile(file.getPath(),
                            String.format("%s/%s", poetryWheelCacheDirectory.toPath(), file.getName()));
                    getLog().info(String.format("Cached the %s file", file.getName()));
                }
            }
        } catch (Exception e) {
            throw new HabushuException("Could not cache the " + project.getArtifactId() + " wheel file(s)!", e);
        }
    }

    protected File getProjectBuildDirectory() {
        return new File(project.getBuild().getDirectory());
    }
}
