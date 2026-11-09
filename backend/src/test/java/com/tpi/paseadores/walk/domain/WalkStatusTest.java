package com.tpi.paseadores.walk.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tpi.paseadores.walk.domain.WalkStatus.CANCELLED;
import static com.tpi.paseadores.walk.domain.WalkStatus.COMPLETED;
import static com.tpi.paseadores.walk.domain.WalkStatus.FULL;
import static com.tpi.paseadores.walk.domain.WalkStatus.IN_PROGRESS;
import static com.tpi.paseadores.walk.domain.WalkStatus.PUBLISHED;
import static org.assertj.core.api.Assertions.assertThat;

class WalkStatusTest {
    // RN-29, written out as the table in CLAUDE.md
    private static final Map<WalkStatus, Set<WalkStatus>> ALLOWED = Map.of(
            PUBLISHED, EnumSet.of(FULL, IN_PROGRESS, CANCELLED),
            FULL, EnumSet.of(PUBLISHED, IN_PROGRESS, CANCELLED),
            IN_PROGRESS, EnumSet.of(COMPLETED, CANCELLED),
            COMPLETED, EnumSet.noneOf(WalkStatus.class),
            CANCELLED, EnumSet.noneOf(WalkStatus.class));

    @Test
    void allowsExactlyTheTransitionsOfRn29() {
        for (WalkStatus from : WalkStatus.values()) {
            for (WalkStatus to : WalkStatus.values()) {
                assertThat(from.canTransitionTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(ALLOWED.get(from).contains(to));
            }
        }
    }

    @Test
    void completedAndCancelledAreTerminal() {
        for (WalkStatus to : WalkStatus.values()) {
            assertThat(COMPLETED.canTransitionTo(to)).isFalse();
            assertThat(CANCELLED.canTransitionTo(to)).isFalse();
        }
    }
}
