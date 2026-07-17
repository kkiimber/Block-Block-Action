package ru.zhuma.blockblockaction;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.UUID;

/** Renders coloured barrier-like outlines while the protector wand is held. */
@EventBusSubscriber(modid = BlockBlockAction.MOD_ID, value = Dist.CLIENT)
public final class ClientOverlayRenderer {
    private static final float OVERLAY_ALPHA = 0.45F;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.level.dimension() != Level.OVERWORLD
                || !minecraft.player.getMainHandItem().is(ModItems.PROTECTOR_WAND.get())) {
            return;
        }

        Map<BlockPos, ProtectionState> protections = ClientProtectionCache.protections();
        Map<UUID, ProtectionState> entityProtections = ClientProtectionCache.entityProtections();
        if (protections.isEmpty() && entityProtections.isEmpty() && ClientAreaSelection.firstCorner() == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Frustum frustum = event.getFrustum();
        Vec3 cameraPosition = event.getCamera().getPosition();
        VertexConsumer vertices = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        for (Map.Entry<BlockPos, ProtectionState> entry : protections.entrySet()) {
            renderBox(poseStack, vertices, frustum, cameraPosition, new AABB(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<UUID, ProtectionState> entry : entityProtections.entrySet()) {
            for (net.minecraft.world.entity.Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity.getUUID().equals(entry.getKey())) {
                    renderBox(poseStack, vertices, frustum, cameraPosition, entity.getBoundingBox(), entry.getValue());
                    break;
                }
            }
        }
        renderAreaPreview(minecraft, poseStack, vertices, frustum, cameraPosition);

        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }

    private static void renderBox(PoseStack poseStack, VertexConsumer vertices, Frustum frustum, Vec3 cameraPosition,
                                  AABB worldBox, ProtectionState state) {
        if (!frustum.isVisible(worldBox)) {
            return;
        }
        float[] colour = switch (state) {
            case BREAK_ONLY -> new float[]{1.0F, 0.85F, 0.05F};
            case BREAK_AND_INTERACT -> new float[]{1.0F, 0.10F, 0.10F};
            case BREAK_AND_PLACE -> new float[]{0.10F, 0.45F, 1.0F};
            case BREAK_INTERACT_AND_PLACE -> new float[]{0.72F, 0.18F, 1.0F};
            case NONE -> new float[]{1.0F, 1.0F, 1.0F};
        };
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        LevelRenderer.renderLineBox(poseStack, vertices, worldBox, colour[0], colour[1], colour[2], OVERLAY_ALPHA);
        poseStack.popPose();
    }

    private static void renderAreaPreview(Minecraft minecraft, PoseStack poseStack, VertexConsumer vertices,
                                          Frustum frustum, Vec3 cameraPosition) {
        BlockPos first = ClientAreaSelection.firstCorner();
        if (first == null || !(minecraft.hitResult instanceof BlockHitResult hitResult)) {
            return;
        }
        BlockPos second = hitResult.getBlockPos();
        AABB preview = new AABB(
                Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()) + 1, Math.max(first.getY(), second.getY()) + 1, Math.max(first.getZ(), second.getZ()) + 1
        );
        renderBox(poseStack, vertices, frustum, cameraPosition, preview, ClientWandControls.selectedMode());
    }

    private ClientOverlayRenderer() {
    }
}
