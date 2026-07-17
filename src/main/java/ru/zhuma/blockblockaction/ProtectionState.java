package ru.zhuma.blockblockaction;

/** All selectable protection modes. */
public enum ProtectionState {
    NONE(false, false, false, "protection.blockblockaction.none"),
    BREAK_ONLY(true, false, false, "protection.blockblockaction.break_only"),
    BREAK_AND_INTERACT(true, true, false, "protection.blockblockaction.break_and_interact"),
    BREAK_AND_PLACE(true, false, true, "protection.blockblockaction.break_and_place"),
    BREAK_INTERACT_AND_PLACE(true, true, true, "protection.blockblockaction.break_interact_and_place");

    private final boolean preventsBreaking;
    private final boolean preventsInteraction;
    private final boolean preventsPlacementOnFace;
    private final String displayKey;

    ProtectionState(boolean preventsBreaking, boolean preventsInteraction, boolean preventsPlacementOnFace, String displayKey) {
        this.preventsBreaking = preventsBreaking;
        this.preventsInteraction = preventsInteraction;
        this.preventsPlacementOnFace = preventsPlacementOnFace;
        this.displayKey = displayKey;
    }

    public boolean preventsBreaking() {
        return preventsBreaking;
    }

    public boolean preventsInteraction() {
        return preventsInteraction;
    }

    public boolean preventsPlacementOnFace() {
        return preventsPlacementOnFace;
    }

    public boolean isProtected() {
        return this != NONE;
    }

    public String displayKey() {
        return displayKey;
    }

    /** Entity protection has no placement concept, so placement modes collapse to their base modes. */
    public ProtectionState forEntity() {
        return switch (this) {
            case BREAK_AND_PLACE -> BREAK_ONLY;
            case BREAK_INTERACT_AND_PLACE -> BREAK_AND_INTERACT;
            default -> this;
        };
    }

    /** Keeps the documented integer-only block file format by repeating its coordinate. */
    public int storageCopies() {
        return ordinal();
    }

    public static ProtectionState fromStorageCopies(int copies) {
        return switch (copies) {
            case 1 -> BREAK_ONLY;
            case 2 -> BREAK_AND_INTERACT;
            case 3 -> BREAK_AND_PLACE;
            default -> BREAK_INTERACT_AND_PLACE;
        };
    }

    public static ProtectionState fromId(int id) {
        ProtectionState[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
