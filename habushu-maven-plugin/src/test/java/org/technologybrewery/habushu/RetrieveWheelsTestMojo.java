package org.technologybrewery.habushu;

import java.io.File;
import java.util.List;

public class RetrieveWheelsTestMojo extends RetrieveWheelsMojo{

    void setWheelDependencies(List<WheelDependency> wheelDependencies) {
        this.wheelDependencies = wheelDependencies;
    }

    List<WheelDependency> getWheelDependencies() {
        return this.wheelDependencies;
    }

    protected File getPoetryCacheDirectory(){
        return getPoetryCacheDirectory();
    }

    @Override
    public File getCachedWheelDirectory(String artifactId, String version){
        String baseDirectory = new File("").getAbsolutePath();
        return new File(baseDirectory+"/src/test/resources/testCacheDirectory/" + artifactId + "/" + version);
    }
}
