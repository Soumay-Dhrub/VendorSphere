package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vendorsphere.common.exception.BusinessException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StateMachineTest {

    private enum Status {
        DRAFT,
        OPEN,
        CLOSED,
        CANCELLED
    }

    private static StateMachine<Status> machine() {
        return StateMachine.of(
                Map.of(
                        Status.DRAFT, EnumSet.of(Status.OPEN, Status.CANCELLED),
                        Status.OPEN, EnumSet.of(Status.CLOSED, Status.CANCELLED)));
    }

    @Test
    void permitsListedTransitionsOnly() {
        StateMachine<Status> machine = machine();

        assertThat(machine.permits(Status.DRAFT, Status.OPEN)).isTrue();
        assertThat(machine.permits(Status.OPEN, Status.CLOSED)).isTrue();
        assertThat(machine.permits(Status.CLOSED, Status.OPEN)).isFalse();
        assertThat(machine.permits(Status.DRAFT, Status.DRAFT)).isFalse();
    }

    @Test
    void permitsIsFalseForNullStates() {
        StateMachine<Status> machine = machine();

        assertThat(machine.permits(null, Status.OPEN)).isFalse();
        assertThat(machine.permits(Status.DRAFT, null)).isFalse();
    }

    @Test
    void targetsFromReturnsEmptySetForUnmappedState() {
        assertThat(machine().targetsFrom(Status.CANCELLED)).isEmpty();
        assertThat(machine().targetsFrom(null)).isEmpty();
    }

    @Test
    void targetsFromIsImmutable() {
        Set<Status> targets = machine().targetsFrom(Status.DRAFT);

        assertThatThrownBy(() -> targets.add(Status.CLOSED))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void assertTransitionPassesForPermittedTransition() {
        machine().assertTransition(Status.OPEN, Status.CLOSED);
    }

    @Test
    void assertTransitionRejectsUnlistedTransitionWithConflictNamingBothStates() {
        assertThatThrownBy(() -> machine().assertTransition(Status.CLOSED, Status.OPEN))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot transition from CLOSED to OPEN")
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void transitionTableIsDefensivelyCopied() {
        Map<Status, Set<Status>> source = new HashMap<>();
        Set<Status> targets = new HashSet<>(Set.of(Status.OPEN));
        source.put(Status.DRAFT, targets);

        StateMachine<Status> machine = StateMachine.of(source);
        targets.add(Status.CLOSED);
        source.put(Status.CLOSED, Set.of(Status.OPEN));

        assertThat(machine.permits(Status.DRAFT, Status.CLOSED)).isFalse();
        assertThat(machine.permits(Status.CLOSED, Status.OPEN)).isFalse();
    }
}
