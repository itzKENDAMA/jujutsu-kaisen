package radon.jujutsu_kaisen.client.render.entity.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.client.ability.ClientProjectionHandler;
import radon.jujutsu_kaisen.client.JJKRenderTypes;
import radon.jujutsu_kaisen.entity.effect.ProjectionFrameEntity;

public class ProjectionFrameRenderer extends EntityRenderer<ProjectionFrameEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(JujutsuKaisen.MOD_ID, "textures/entity/projection_frame.png");
    private static final RenderType RENDER_TYPE = JJKRenderTypes.entityCutoutNoCull(TEXTURE);
    private static final float SIZE = 1.0F;

    public ProjectionFrameRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(ProjectionFrameEntity pEntity, float pEntityYaw, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        LivingEntity victim = pEntity.getVictim();

        if (victim == null) return;

        Minecraft mc = Minecraft.getInstance();

        pPoseStack.pushPose();

        float yaw = Mth.lerp(pPartialTick, victim.yRotO, victim.getYRot());
        pPoseStack.mulPose(Axis.YP.rotationDegrees(360.0F - yaw));

        ClientProjectionHandler.frame = true;

        pPoseStack.pushPose();
        pPoseStack.scale(1.0F, 1.0F, 0.02F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        EntityRenderDispatcher manager = mc.getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity> renderer = manager.getRenderer(victim);
        renderer.render(victim, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);

        ClientProjectionHandler.frame = false;

        pPoseStack.popPose();

        pPoseStack.translate(0.0D, victim.getBbHeight() / 2.0F, 0.0D);
        pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RENDER_TYPE);
        Matrix4f pose = pPoseStack.last().pose();

        consumer.vertex(pose, -SIZE, 0.0F, -SIZE)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_SKY)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(pose, -SIZE, 0.0F, SIZE)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_SKY)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(pose, SIZE, 0.0F, SIZE)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_SKY)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(pose, SIZE, 0.0F, -SIZE)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_SKY)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
        mc.renderBuffers().bufferSource().endBatch(RENDER_TYPE);

        pPoseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ProjectionFrameEntity pEntity) {
        return null;
    }

    @Override
    protected int getBlockLightLevel(@NotNull ProjectionFrameEntity pEntity, @NotNull BlockPos pPos) {
        return 15;
    }
}