package radon.jujutsu_kaisen.client.gui.screen.base;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.ISorcererData;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.client.ability.ClientAbilityHandler;
import radon.jujutsu_kaisen.client.gui.screen.DisplayItem;
import radon.jujutsu_kaisen.entity.base.ISorcerer;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.c2s.CurseSummonC2SPacket;
import radon.jujutsu_kaisen.network.packet.c2s.SetAbsorbedC2SPacket;
import radon.jujutsu_kaisen.network.packet.c2s.SetAdditionalC2SPacket;
import radon.jujutsu_kaisen.network.packet.c2s.TriggerAbilityC2SPacket;
import radon.jujutsu_kaisen.util.HelperMethods;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class RadialScreen extends Screen {
    private static final float RADIUS_IN = 50.0F;
    private static final float RADIUS_OUT = RADIUS_IN * 2.0F;

    private final List<DisplayItem> items = new ArrayList<>();

    private int hovered = -1;

    public RadialScreen() {
        super(Component.nullToEmpty(null));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        assert this.minecraft != null;
        this.items.addAll(this.getItems());

        if (this.items.isEmpty()) {
            this.onClose();
        }
    }

    protected abstract List<DisplayItem> getItems();

    private void drawSlot(PoseStack poseStack, BufferBuilder buffer, float centerX, float centerY,
                          float startAngle, float endAngle, int color) {
        float angle = endAngle - startAngle;
        float precision = 2.5F / 360.0F;
        int sections = Math.max(1, Mth.ceil(angle / precision));

        angle = endAngle - startAngle;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        float slice = angle / sections;

        for (int i = 0; i < sections; i++) {
            float angle1 = startAngle + i * slice;
            float angle2 = startAngle + (i + 1) * slice;

            float x1 = centerX + RADIUS_IN * (float) Math.cos(angle1);
            float y1 = centerY + RADIUS_IN * (float) Math.sin(angle1);
            float x2 = centerX + RADIUS_OUT * (float) Math.cos(angle1);
            float y2 = centerY + RADIUS_OUT * (float) Math.sin(angle1);
            float x3 = centerX + RADIUS_OUT * (float) Math.cos(angle2);
            float y3 = centerY + RADIUS_OUT * (float) Math.sin(angle2);
            float x4 = centerX + RADIUS_IN * (float) Math.cos(angle2);
            float y4 = centerY + RADIUS_IN * (float) Math.sin(angle2);

            Matrix4f pose = poseStack.last().pose();
            buffer.vertex(pose, x2, y2, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x1, y1, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x4, y4, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(pose, x3, y3, 0.0F).color(r, g, b, a).endVertex();
        }
    }

    @Override
    public void onClose() {
        if (this.hovered != -1) {
            if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
                if (this.minecraft.player.getCapability(SorcererDataHandler.INSTANCE).isPresent()) {
                    ISorcererData cap = this.minecraft.player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

                    DisplayItem item = this.items.get(this.hovered);

                    if (item.type == DisplayItem.Type.ABILITY) {
                        Ability ability = item.ability;
                        PacketHandler.sendToServer(new TriggerAbilityC2SPacket(JJKAbilities.getKey(ability)));
                        ClientAbilityHandler.trigger(ability);
                    } else if (item.type == DisplayItem.Type.CURSE) {
                        EntityType<?> type = item.curse.getKey();
                        Registry<EntityType<?>> registry = this.minecraft.level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
                        PacketHandler.sendToServer(new CurseSummonC2SPacket(registry.getKey(type)));
                    } else if (item.type == DisplayItem.Type.COPIED) {
                        PacketHandler.sendToServer(new SetAdditionalC2SPacket(item.copied));
                        cap.setCurrentCopied(item.copied);
                    } else if (item.type == DisplayItem.Type.ABSORBED) {
                        PacketHandler.sendToServer(new SetAbsorbedC2SPacket(item.absorbed));
                        cap.setCurrentAbsorbed(item.absorbed);
                    }
                }
            }
        }
        super.onClose();
    }

    public static void drawCenteredString(@NotNull GuiGraphics pGuiGraphics, Font pFont, Component pText, int pX, int pY, int pColor) {
        FormattedCharSequence sql = pText.getVisualOrderText();
        pGuiGraphics.drawString(pFont, sql, pX - pFont.width(sql) / 2, pY, pColor);
    }

    private static void renderEntityInInventoryFollowsAngle(PoseStack pPoseStack, int pX, int pY, int pScale, float angleXComponent, float angleYComponent, Entity pEntity) {
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float) Math.PI);
        Quaternionf quaternionf1 = (new Quaternionf()).rotateX(angleYComponent * 20.0F * ((float) Math.PI / 180.0F));
        quaternionf.mul(quaternionf1);

        if (pEntity instanceof LivingEntity living) {
            living.yBodyRot = 180.0F + angleXComponent * 20.0F;
        }
        pEntity.setYRot(180.0F + angleXComponent * 40.0F);
        pEntity.setXRot(-angleYComponent * 20.0F);

        if (pEntity instanceof LivingEntity living) {
            living.yHeadRot = pEntity.getYRot();
            living.yHeadRotO = pEntity.getYRot();
        }
        renderEntityInInventory(pPoseStack, pX, pY, pScale, quaternionf, quaternionf1, pEntity);
    }

    private static void renderEntityInInventory(PoseStack pPoseStack, int pX, int pY, int pScale, Quaternionf p_275229_, @Nullable Quaternionf pCameraOrientation, Entity pEntity) {
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate(0.0D, 0.0D, 1000.0D);
        RenderSystem.applyModelViewMatrix();
        pPoseStack.pushPose();
        pPoseStack.translate(pX, pY, -950.0D);
        pPoseStack.mulPoseMatrix((new Matrix4f()).scaling((float)pScale, (float)pScale, (float)(-pScale)));
        pPoseStack.mulPose(p_275229_);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher Dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        if (pCameraOrientation != null) {
            pCameraOrientation.conjugate();
            Dispatcher.overrideCameraOrientation(pCameraOrientation);
        }
        Dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() ->
                Dispatcher.render(pEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, pPoseStack, buffer, 15728880));
        buffer.endBatch();
        Dispatcher.setRenderShadow(true);
        pPoseStack.popPose();
        Lighting.setupFor3DItems();
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);

        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) return;

        if (!this.minecraft.player.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return;
        ISorcererData cap = this.minecraft.player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        pGuiGraphics.pose().pushPose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < this.items.size(); i++) {
            float startAngle = getAngleFor(i - 0.5F);
            float endAngle = getAngleFor(i + 0.5F);

            DisplayItem item = this.items.get(i);
            int white = HelperMethods.toRGB24(255, 255, 255, 150);
            int black = HelperMethods.toRGB24(0, 0, 0, 150);

            int color;

            if (item.type == DisplayItem.Type.ABILITY && JJKAbilities.hasToggled(this.minecraft.player, item.ability) ||
                    item.type == DisplayItem.Type.COPIED && cap.getCurrentCopied() == item.copied ||
                    item.type == DisplayItem.Type.ABSORBED && cap.getCurrentCopied() == item.absorbed) {
                color = this.hovered == i ? black : white;
            } else {
                color = this.hovered == i ? white : black;
            }
            this.drawSlot(pGuiGraphics.pose(), buffer, centerX, centerY, startAngle, endAngle, color);
        }

        tesselator.end();
        RenderSystem.disableBlend();
        pGuiGraphics.pose().popPose();
        float radius = (RADIUS_IN + RADIUS_OUT) / 2.0F;

        for (int i = 0; i < this.items.size(); i++) {
            float start = getAngleFor(i - 0.5F);
            float end = getAngleFor(i + 0.5F);
            float middle = (start + end) / 2.0F;
            int posX = (int) (centerX + radius * (float) Math.cos(middle));
            int posY = (int) (centerY + radius * (float) Math.sin(middle));

            DisplayItem item = this.items.get(i);

            if (this.hovered == i) {
                List<Component> lines = new ArrayList<>();

                if (item.type == DisplayItem.Type.ABILITY) {
                    float cost = item.ability.getRealCost(this.minecraft.player);

                    if (cost > 0.0F) {
                        Component costText = Component.translatable(String.format("gui.%s.ability_overlay.cost", JujutsuKaisen.MOD_ID), cost);
                        lines.add(costText);
                    }

                    if (item instanceof Ability.IDurationable durationable) {
                        int duration = durationable.getRealDuration(this.minecraft.player);

                        if (duration > 0) {
                            Component durationText = Component.translatable(String.format("gui.%s.ability_overlay.duration", JujutsuKaisen.MOD_ID), (float) duration / 20);
                            lines.add(durationText);
                        }
                    }
                } else if (item.type == DisplayItem.Type.CURSE) {
                    Component countText = Component.translatable(String.format("gui.%s.ability_overlay.count", JujutsuKaisen.MOD_ID), item.curse.getValue());
                    lines.add(countText);

                    if (item.curse.getKey().create(this.minecraft.level) instanceof ISorcerer curse) {
                        Component costText = Component.translatable(String.format("gui.%s.ability_overlay.cost", JujutsuKaisen.MOD_ID),
                                JJKAbilities.getCurseCost(this.minecraft.player, curse.getGrade()));
                        lines.add(costText);
                    }
                }

                int x = this.width / 2;
                int y = this.height / 2 - this.font.lineHeight / 2 - ((lines.size() - 1) * this.font.lineHeight);

                for (Component line : lines) {
                    drawCenteredString(pGuiGraphics, this.font, line, x, y, 0xFFFFFF);
                    y += this.font.lineHeight;
                }
            }

            if ((item.ability instanceof Summon<?> summon && summon.display()) || item.type == DisplayItem.Type.CURSE) {
                EntityType<?> type = item.type == DisplayItem.Type.ABILITY ? ((Summon<?>) item.ability).getTypes().get(0) : item.curse.getKey();

                Entity entity = type.create(this.minecraft.level);

                if (entity == null) continue;

                float height = entity.getBbHeight();
                int scale = (int) Math.max(3.0F, 10.0F - entity.getBbHeight());
                renderEntityInInventoryFollowsAngle(pGuiGraphics.pose(), posX, (int) (posY + (height * scale / 2.0F)), scale, -1.0F, -0.5F, entity);
            } else if (item.type == DisplayItem.Type.ABILITY) {
                int y = posY - this.font.lineHeight / 2;

                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose().scale(0.5F, 0.5F, 0.0F);
                pGuiGraphics.pose().translate(posX, y, 0.0F);
                drawCenteredString(pGuiGraphics, this.font, item.ability.getName(), posX, y, 0xFFFFFF);
                pGuiGraphics.pose().popPose();
            } else if (item.type == DisplayItem.Type.COPIED || item.type == DisplayItem.Type.ABSORBED) {
                int y = posY - this.font.lineHeight / 2;

                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose().scale(0.5F, 0.5F, 0.0F);
                pGuiGraphics.pose().translate(posX, y, 0.0F);
                drawCenteredString(pGuiGraphics, this.font, item.type == DisplayItem.Type.COPIED ? item.copied.getName() : item.absorbed.getName(), posX, y, 0xAA00AA);
                pGuiGraphics.pose().popPose();
            }
        }
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        super.mouseMoved(pMouseX, pMouseY);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        double mouseAngle = Math.atan2(pMouseY - centerY, pMouseX - centerX);
        double mousePos = Math.sqrt(Math.pow(pMouseX - centerX, 2.0D) + Math.pow(pMouseY - centerY, 2.0D));

        if (this.items.size() > 0) {
            float startAngle = getAngleFor(-0.5F);
            float endAngle = getAngleFor(this.items.size() - 0.5F);

            while (mouseAngle < startAngle) {
                mouseAngle += Mth.TWO_PI;
            }
            while (mouseAngle >= endAngle) {
                mouseAngle -= Mth.TWO_PI;
            }
        }

        this.hovered = -1;

        for (int i = 0; i < this.items.size(); i++) {
            float startAngle = getAngleFor(i - 0.5F);
            float endAngle = getAngleFor(i + 0.5F);

            if (mouseAngle >= startAngle && mouseAngle < endAngle && mousePos >= RADIUS_IN && mousePos < RADIUS_OUT) {
                this.hovered = i;
                break;
            }
        }
    }

    private float getAngleFor(double i) {
        if (this.items.size() == 0) {
            return 0;
        }
        return (float) (((i / this.items.size()) + 0.25) * Mth.TWO_PI + Math.PI);
    }
}