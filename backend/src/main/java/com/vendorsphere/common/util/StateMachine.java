package com.vendorsphere.common.util;

import com.vendorsphere.common.exception.BusinessException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;

public final class StateMachine<S extends Enum<S>> {

    private final Map<S, Set<S>> transitions;

    private StateMachine(Map<S, Set<S>> transitions) {
        this.transitions = transitions;
    }

    public static <S extends Enum<S>> StateMachine<S> of(Map<S, Set<S>> transitions) {
        Objects.requireNonNull(transitions, "transitions must not be null");
        Map<S, Set<S>> copy = null;
        for (Map.Entry<S, Set<S>> entry : transitions.entrySet()) {
            S from = Objects.requireNonNull(entry.getKey(), "transition source must not be null");
            Set<S> targets =
                    Objects.requireNonNull(entry.getValue(), "transition targets must not be null");
            if (copy == null) {
                copy = new EnumMap<>(from.getDeclaringClass());
            }
            Set<S> targetCopy = EnumSet.noneOf(from.getDeclaringClass());
            for (S to : targets) {
                targetCopy.add(Objects.requireNonNull(to, "transition target must not be null"));
            }
            copy.put(from, Collections.unmodifiableSet(targetCopy));
        }
        return new StateMachine<>(
                copy == null ? Collections.emptyMap() : Collections.unmodifiableMap(copy));
    }

    public boolean permits(S from, S to) {
        if (from == null || to == null) {
            return false;
        }
        return targetsFrom(from).contains(to);
    }

    public Set<S> targetsFrom(S from) {
        if (from == null) {
            return Collections.emptySet();
        }
        Set<S> targets = transitions.get(from);
        return targets == null ? Collections.emptySet() : targets;
    }

    public void assertTransition(S from, S to) {
        if (!permits(from, to)) {
            throw new BusinessException(
                    "Cannot transition from " + name(from) + " to " + name(to), HttpStatus.CONFLICT);
        }
    }

    private String name(S state) {
        return state == null ? "null" : state.name();
    }
}
