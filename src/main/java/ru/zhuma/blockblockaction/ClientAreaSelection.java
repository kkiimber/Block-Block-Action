package ru.zhuma.blockblockaction;

import net.minecraft.core.BlockPos;

/** Client preview state for the two-corner block selection. */
public final class ClientAreaSelection {
    private static BlockPos firstCorner;

    public static void beginOrCancel(BlockPos position) {
        firstCorner = firstCorner == null ? position.immutable() : null;
    }

    public static void finish() {
        firstCorner = null;
    }

    public static BlockPos firstCorner() {
        return firstCorner;
    }

    private ClientAreaSelection() {
    }
}
