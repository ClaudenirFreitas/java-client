package io.split.client;

import io.split.client.api.Key;
import io.split.grammar.Treatments;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * A SplitClient that ensures that all features are turned off for all users.
 * Useful for testing
 *
 * @author adil
 */
public class AlwaysReturnControlSplitClient implements SplitClient {

    @Override
    public String getTreatment(String key, String split) {
        return Treatments.CONTROL;
    }

    @Override
    public String getTreatment(String key, String split, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public String getTreatment(Key key, String split, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean track(String key, String trafficType, String eventType) {
        return false;
    }

    @Override
    public boolean track(String key, String trafficType, String eventType, double value) {
        return false;
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        //AlwaysReturnControl is always ready
    }

}
