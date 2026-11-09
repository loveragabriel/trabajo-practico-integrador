package com.tpi.paseadores.walk.domain;

// matches the Postgres enum walk_status
public enum WalkStatus {
    PUBLISHED, FULL, IN_PROGRESS, COMPLETED, CANCELLED;

    // RN-29: the only valid transitions. PUBLISHED <-> FULL is driven by bookings (RN-30),
    // COMPLETED and CANCELLED are terminal
    public boolean canTransitionTo(WalkStatus next) {
        return switch (this) {
            case PUBLISHED -> next == FULL || next == IN_PROGRESS || next == CANCELLED;
            case FULL -> next == PUBLISHED || next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
