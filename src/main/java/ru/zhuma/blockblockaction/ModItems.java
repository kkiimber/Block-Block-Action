package ru.zhuma.blockblockaction;

import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All items registered by Block Block Action. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BlockBlockAction.MOD_ID);

    public static final DeferredItem<Item> PROTECTOR_WAND = ITEMS.registerSimpleItem(
            "protector_wand",
            new Item.Properties().stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
    );

    private ModItems() {
    }
}
