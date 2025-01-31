package radon.jujutsu_kaisen.client.render.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.entity.projectile.FireArrowProjectile;

public class FireArrowRenderer extends EntityRenderer<FireArrowProjectile> {
    private static final float SIZE = 1.5F;
    private static final int TEXTURE_WIDTH = 32;
    private static final int STARTUP_TEXTURE_HEIGHT = 256;
    private static final int STILL_TEXTURE_HEIGHT = 128;
    private static final ResourceLocation STARTUP = new ResourceLocation(JujutsuKaisen.MOD_ID, "textures/entity/fire_arrow_startup.png");
    private static final ResourceLocation STILL = new ResourceLocation(JujutsuKaisen.MOD_ID, "textures/entity/fire_arrow.png");

    public FireArrowRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull FireArrowProjectile pEntity, float pEntityYaw, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        Minecraft mc = Minecraft.getInstance();

        pPoseStack.pushPose();
        pPoseStack.translate(0.0F, pEntity.getBbHeight() / 2.0F, 0.0F);

        float yaw = Mth.lerp(pPartialTick, pEntity.yRotO, pEntity.getYRot());
        float pitch = Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot());

        pPoseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(45.0F));

        boolean still = pEntity.getTime() >= FireArrowProjectile.DELAY;
        RenderType type = RenderType.entityCutoutNoCull(still ? STILL : STARTUP);

        for (int i = 0; i < 2; i++) {
            pPoseStack.pushPose();
            pPoseStack.translate(0.0F, i * 0.1F, i * 0.1F - 0.1F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(i * 90.0F));

            VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(type);
            Matrix4f pose = pPoseStack.last().pose();

            int frame = Mth.floor((pEntity.animation - 1 + pPartialTick) * 2);

            if (frame < 0) {
                frame = still ? FireArrowProjectile.STILL_FRAMES : FireArrowProjectile.STARTUP_FRAMES * 2;
            }

            float minU = 0.0F;
            float minV = 32.0F / (still ? STILL_TEXTURE_HEIGHT : STARTUP_TEXTURE_HEIGHT) * frame;
            float maxU = minU + 32.0F / TEXTURE_WIDTH;
            float maxV = minV + 32.0F / (still ? STILL_TEXTURE_HEIGHT : STARTUP_TEXTURE_HEIGHT);

            consumer.vertex(pose, -SIZE, 0.0F, -SIZE)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .uv(minU, minV)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_SKY)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
            consumer.vertex(pose, -SIZE, 0.0F, SIZE)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .uv(minU, maxV)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_SKY)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
            consumer.vertex(pose, SIZE, 0.0F, SIZE)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .uv(maxU, maxV)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_SKY)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
            consumer.vertex(pose, SIZE, 0.0F, -SIZE)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .uv(maxU, minV)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_SKY)
                    .normal(0.0F, 1.0F, 0.0F)
                    .endVertex();
            mc.renderBuffers().bufferSource().endBatch(type);

            pPoseStack.popPose();
        }
        pPoseStack.popPose();
    }

    @Override
    protected int getBlockLightLevel(@NotNull FireArrowProjectile pEntity, @NotNull BlockPos pPos) {
        return 15;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FireArrowProjectile pEntity) {
        return null;
    }
}
