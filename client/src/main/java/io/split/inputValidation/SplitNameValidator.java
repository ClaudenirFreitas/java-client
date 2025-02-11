package io.split.inputValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SplitNameValidator {
    private static final Logger _log = LoggerFactory.getLogger(SplitNameValidator.class);

    public static Optional<String> isValid(String name, String method) {
        if (name == null) {
            _log.error(String.format("%s: you passed a null split name, split name must be a non-empty string", method));
            return Optional.empty();
        }

        if (name.isEmpty()) {
            _log.error(String.format("%s: you passed an empty split name, split name must be a non-empty string", method));
            return Optional.empty();
        }

        String trimmed = name.trim();
        if (!trimmed.equals(name)) {
            _log.warn(String.format("%s: split name %s has extra whitespace, trimming", method, name));
            name = trimmed;
        }

        return Optional.of(name);
    }

    public static List<String> areValid(List<String> splits, String method) {
        return splits.stream().distinct()
                .map(s -> isValid(s, method).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
