package kitnapper.service;

/** User-facing settlement messages, kept out of the service algorithms. */
final class PenaltyMessages {

    static final String MANAGER_ONLY = "Only a manager can settle penalties.";
    static final String PENALTY_MISSING = "That penalty is no longer in the records.";
    static final String ALREADY_SETTLED = "This penalty is already settled.";
    static final String REASON_REQUIRED = "A reason is required to waive a penalty.";

    private PenaltyMessages() {
    }
}
