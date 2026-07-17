package ru.zhuma.blockblockaction;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** The dedicated creative inventory tab for this mod. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlockBlockAction.MOD_ID);

    public static final Supplier<CreativeModeTab> BLOCK_BLOCK_ACTION = CREATIVE_MODE_TABS.register(
            "block_block_action",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blockblockaction.block_block_action"))
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .displayItems((parameters, output) -> output.accept(ModItems.PROTECTOR_WAND.get()))
                    .build()
    );

    private ModCreativeTabs() {
    }
}
