package com.vendorsphere.common.util;

import com.vendorsphere.common.exception.BusinessException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * Declarative lifecycle state machine. A machine holds an immutable table of permitted
 * {@code source -> targets} pairs and answers whether a transition is allowed.
 *
 * <p>This class is the single source of the HTTP 409 wording used for vendor, purchase request,
 * RFQ, purchase order and invoice status changes: {@code Cannot transition from X to Y}.
 *
 * @param <S> the lifecycle enum type
 */
public final class StateMachine<S extends Enum<S>> {

    private final Map<S, Set<S>> transitions;

    private StateMachine(Map<S, Set<S>> transitions) {
        this.transitions = transitions;
    }

    /**
     * Builds a machine from the supplied transition table. The table and its target sets are
     * defensively copied, so later mutation of the argument cannot affect the machine.
     *
     * @param transitions permitted targets keyed by source state; neither keys, value sets nor the
     *     states they contain may be {@code null}
     * @return an immutable state machine over the given table
     */
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

    /**
     * Answers whether this machine permits the transition.
     *
     * @return {@code true} only when {@code to} is listed as a target of {@code from}
     */
    public boolean permits(S from, S to) {
        if (from == null || to == null) {
            return false;
        }
        return targetsFrom(from).contains(to);
    }

    /**
     * Permitted targets of the given source state.
     *
     * @return the targets of {@code from}, or an empty set when the state has no mapped targets
     */
    public Set<S> targetsFrom(S from) {
        if (from == null) {
            return Collections.emptySet();
        }
        Set<S> targets = transitions.get(from);
        return targets == null ? Collections.emptySet() : targets;
    }

    /**
     * Asserts that the transition is permitted.
     *
     * @throws BusinessException with HTTP status 409 and the message
     *     {@code Cannot transition from <from> to <to>} when the transition is not permitted
     */
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
