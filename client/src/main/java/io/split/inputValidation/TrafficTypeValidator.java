package io.split.inputValidation;

import io.split.cache.SplitCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrafficTypeValidator {
    private static final Logger _log = LoggerFactory.getLogger(TrafficTypeValidator.class);

    public static InputValidationResult isValid(String trafficTypeName, SplitCache splitCache, String method) {
        if (trafficTypeName == null) {
            _log.error(String.format("%s: you passed a null trafficTypeName, trafficTypeName must be a non-empty string", method));
            return new InputValidationResult(false);
        }

        if (trafficTypeName.isEmpty()) {
            _log.error(String.format("%s: you passed an empty trafficTypeName, trafficTypeName must be a non-empty string", method));
            return new InputValidationResult(false);
        }

        if (!trafficTypeName.equals(trafficTypeName.toLowerCase())) {
            _log.warn(String.format("%s: trafficTypeName should be all lowercase - converting string to lowercase", method));
            trafficTypeName = trafficTypeName.toLowerCase();
        }

        if (!splitCache.trafficTypeExists(trafficTypeName)) {
            _log.warn(String.format("%s: Traffic Type %s does not have any corresponding Splits in this environment, " +
                    "make sure you’re tracking your events to a valid traffic type defined in the Split console.", method, trafficTypeName));
        }

        return new InputValidationResult(true, trafficTypeName);
    }
}
