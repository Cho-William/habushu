package org.technologybrewery.habushu;

import com.google.common.base.Predicates;
import io.github.itning.retry.Retryer;
import io.github.itning.retry.RetryerBuilder;
import io.github.itning.retry.strategy.stop.StopStrategies;
import io.github.itning.retry.strategy.stop.StopStrategy;
import io.github.itning.retry.strategy.wait.WaitStrategies;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TestRetryPublishToPyPiRepoMojo extends PublishToPyPiRepoMojo {

    protected int finalRetryNumber;

    public TestRetryPublishToPyPiRepoMojo(int numberOfRetries, int finalRetryNumber) {
        super();

        this.packaging = "habushu";
        this.useDevRepository = false;
        this.pypiPushRetryMultiplier = 50;
        this.pypiPushRetryMaxTimeout = 1;

        if (numberOfRetries >= 0) {
            this.pypiPushRetries = numberOfRetries;
        }

        this.finalRetryNumber = finalRetryNumber;

    }

    @Override
    protected Callable<Boolean> getPyPiPushCallable(PoetryCommandHelper poetryHelper, List<Pair<String, Boolean>> publishToOfficialPypiRepoArgs) {
        return new Callable<Boolean>() {
            int counter = 0;

            @Override
            public Boolean call() throws IOException {
                if (counter < finalRetryNumber) {
                    counter++;
                    getLog().warn("Faux publish to PyPI failed (attempt " + counter + ")");

                    // simulate both returning false (non-0 return code) and a RuntimeException:
                    if (counter % 2 == 0) {
                        throw new HabushuException();
                    } else {
                        return false;
                    }
                }

                return true;
            }
        };
    }
}
