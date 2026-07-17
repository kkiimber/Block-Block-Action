package ru.zhuma.blockblockaction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(BlockBlockAction.MOD_ID)
public final class BlockBlockAction {
    public static final String MOD_ID = "blockblockaction";
    private static final ProtectionManager PROTECTION_MANAGER = new ProtectionManager();

    public BlockBlockAction(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::registerPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(this::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(this::onLevelSave);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(this::onBlockToolModification);
        NeoForge.EVENT_BUS.addListener(this::onFluidPlaceBlock);
        NeoForge.EVENT_BUS.addListener(this::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(this::onPlayerAttackEntity);
        NeoForge.EVENT_BUS.addListener(this::onEntityInvulnerabilityCheck);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    public static ProtectionManager protectionManager() {
        return PROTECTION_MANAGER;
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(ProtectionSyncPayload.TYPE, ProtectionSyncPayload.STREAM_CODEC, ProtectionSyncPayload::handleOnClient);
        registrar.playToServer(ModeSelectionPayload.TYPE, ModeSelectionPayload.STREAM_CODEC, ModeSelectionPayload::handleOnServer);
    }

    private void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && isOverworld(level)) {
            PROTECTION_MANAGER.load(level);
        }
    }

    private void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level && isOverworld(level)) {
            PROTECTION_MANAGER.save(level);
        }
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        boolean usingWand = event.getHand() == InteractionHand.MAIN_HAND
                && player.getMainHandItem().is(ModItems.PROTECTOR_WAND.get());

        if (player.level().isClientSide) {
            if (usingWand && player.hasPermissions(2)) {
                if (player.isShiftKeyDown()) {
                    ClientAreaSelection.beginOrCancel(event.getPos());
                } else if (ClientAreaSelection.firstCorner() != null) {
                    ClientAreaSelection.finish();
                }
            }
            return;
        }
        if (!isOverworld(player.level())) {
            return;
        }

        if (usingWand && player.hasPermissions(2)) {
            if (player.isShiftKeyDown()) {
                PROTECTION_MANAGER.beginOrCancelArea((ServerPlayer) player, event.getPos());
            } else {
                ProtectionState mode = PROTECTION_MANAGER.selectedMode((ServerPlayer) player);
                BlockPos firstCorner = PROTECTION_MANAGER.finishArea((ServerPlayer) player);
                if (firstCorner == null) {
                    PROTECTION_MANAGER.setBlockProtection(event.getPos(), mode);
                } else {
                    PROTECTION_MANAGER.setBlockProtectionInArea(firstCorner, event.getPos(), mode);
                }
                PROTECTION_MANAGER.syncToAllPlayers();
            }
            player.swing(InteractionHand.MAIN_HAND, true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        ProtectionState state = PROTECTION_MANAGER.getBlockProtection(event.getPos()).orElse(ProtectionState.NONE);
        if (state.preventsInteraction()) {
            // This blocks all vanilla and Create block GUIs/configuration interactions.
            event.setUseBlock(TriState.FALSE);
            if (state.preventsPlacementOnFace() || !(player.getItemInHand(event.getHand()).getItem() instanceof BlockItem)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        } else if (state.preventsPlacementOnFace()
                && player.getItemInHand(event.getHand()).getItem() instanceof BlockItem) {
            // Keep button/door/etc. interaction, but stop the BlockItem from placing next to this face.
            event.setUseItem(TriState.FALSE);
        }
    }

    private void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleEntityInteraction(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleEntityInteraction(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private boolean handleEntityInteraction(Player player, Entity target, InteractionHand hand) {
        if (player.level().isClientSide || !isOverworld(player.level())) {
            return false;
        }
        boolean usingWand = hand == InteractionHand.MAIN_HAND && player.getMainHandItem().is(ModItems.PROTECTOR_WAND.get());
        if (usingWand && player.hasPermissions(2)) {
            PROTECTION_MANAGER.setEntityProtection(target, PROTECTION_MANAGER.selectedMode((ServerPlayer) player));
            PROTECTION_MANAGER.syncToAllPlayers();
            player.swing(InteractionHand.MAIN_HAND, true);
            return true;
        }
        if (PROTECTION_MANAGER.getEntityProtection(target).map(ProtectionState::preventsInteraction).orElse(false)) {
            return true;
        }
        return false;
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isOverworld(level) && PROTECTION_MANAGER.isBlockProtected(event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isOverworld(level) && PROTECTION_MANAGER.isBlockProtected(event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && isOverworld(level)
                && PROTECTION_MANAGER.isBlockProtected(event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && isOverworld(level)
                && PROTECTION_MANAGER.isBlockProtected(event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide && isOverworld(event.getLevel())) {
            event.getAffectedBlocks().removeIf(PROTECTION_MANAGER::isBlockProtected);
            event.getAffectedEntities().removeIf(PROTECTION_MANAGER::isEntityProtected);
        }
    }

    private void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide
                && isOverworld(event.getEntity().level())
                && PROTECTION_MANAGER.isEntityProtected(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    private void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide
                && isOverworld(event.getEntity().level())
                && PROTECTION_MANAGER.isEntityProtected(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private void onEntityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!event.getEntity().level().isClientSide
                && isOverworld(event.getEntity().level())
                && PROTECTION_MANAGER.isEntityProtected(event.getEntity())) {
            event.setInvulnerable(true);
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isOverworld(player.level())) {
            PROTECTION_MANAGER.syncToPlayer(player);
        }
    }

    private static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }
}
