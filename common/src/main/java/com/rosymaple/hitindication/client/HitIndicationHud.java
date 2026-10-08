package com.rosymaple.hitindication.client;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import com.rosymaple.hitindication.latesthits.HitIndicator;
import com.rosymaple.hitindication.latesthits.HitIndicatorType;
import com.rosymaple.hitindication.latesthits.HitMarker;
import com.rosymaple.hitindication.latesthits.HitMarkerType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the hit indicators and crit/kill markers. Registered as the last HUD layer on every
 * loader, which is where the original drew (after the whole overlay).
 */
public class HitIndicationHud {
    public static final ResourceLocation LAYER_ID = HitIndication.id("hit_indicators");

    private static final ResourceLocation INDICATOR = HitIndication.id("textures/hit/indicator.png");
    private static final ResourceLocation EDGE_INDICATOR = HitIndication.id("textures/hit/edge_indicator.png");
    private static final ResourceLocation INDICATOR_BLOCK = HitIndication.id("textures/hit/indicator_block.png");
    private static final ResourceLocation ND_INDICATOR = HitIndication.id("textures/hit/nd_person_damage.png");
    private static final ResourceLocation[] MARKER_CRIT = {
            HitIndication.id("textures/hit/marker_crit1.png"),
            HitIndication.id("textures/hit/marker_crit2.png"),
            HitIndication.id("textures/hit/marker_crit3.png"),
            HitIndication.id("textures/hit/marker_crit4.png")
    };
    private static final ResourceLocation[] MARKER_KILL = {
            HitIndication.id("textures/hit/marker_kill1.png"),
            HitIndication.id("textures/hit/marker_kill2.png"),
            HitIndication.id("textures/hit/marker_kill3.png"),
            HitIndication.id("textures/hit/marker_kill4.png")
    };
    private static final int ND_INDICATOR_WIDTH = 66;
    private static final float ND_INDICATOR_DEFAULT_SCALE = 1.25F;
    private static final int INDICATOR_WIDTH = 42;
    private static final int INDICATOR_HEIGHT = 13;
    private static final int MARKER_WIDTH = 20;
    private static final int MARKER_HEIGHT = 20;
    private static final int EDGE_INDICATOR_WIDTH = 16;
    private static final int EDGE_INDICATOR_HEIGHT = 16;
    /** Space Edge of Screen mode keeps free at the bottom for the hotbar, health and food. */
    private static final int HUD_HEIGHT = 50;

    private static String lastHitColorString = HitIndicatorClientConfigs.DEFAULT_HIT_INDICATOR_COLOR;
    private static String lastBlockColorString = HitIndicatorClientConfigs.DEFAULT_BLOCK_INDICATOR_COLOR;
    private static int hitColor = 0xFF0000;
    private static int blockColor = 0x0000FF;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if(player == null || mc.level == null)
            return;
        if(ClientLatestHits.latestHitIndicators.isEmpty() && ClientLatestHits.currentHitMarker == null)
            return;

        // The textures are tinted and made translucent through the shader colour, as in the
        // original; both have to be reset for whatever draws next.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            renderHits(graphics, player);
        } finally {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static void renderHits(GuiGraphics graphics, LocalPlayer player) {
        HitIndicatorClientConfigs.Values config = HitIndicatorClientConfigs.get();
        int screenMiddleX = graphics.guiWidth() / 2;
        int screenMiddleY = graphics.guiHeight() / 2;

        // The toggles also hide indicators that are already on screen, not only later ones.
        if(config.enableHitIndication() && !ClientLatestHits.latestHitIndicators.isEmpty()) {
            updateColorsIfNeeded(config);
            IndicatorStyle style = new IndicatorStyle(config);
            float yaw = player.getYRot();
            for (HitIndicator hit : ClientLatestHits.latestHitIndicators) {
                if(hit.getType() == HitIndicatorType.BLOCK && !config.showBlueIndicators())
                    continue;
                // Edge of Screen mode never drew the non-directional indicator.
                if(hit.getType() == HitIndicatorType.ND_HIT && (!config.enableNonDirectionalDamage() || style.edgeOfScreen))
                    continue;

                // Subtract in double precision: a float cannot hold a world coordinate near the
                // border to the block.
                double dx = hit.getX() - player.getX();
                double dy = hit.getY() - player.getY();
                double dz = hit.getZ() - player.getZ();
                float angle = IndicatorMath.angleTo(yaw, dx, dz);
                float scale = indicatorScale(hit, style, Math.sqrt(dx * dx + dy * dy + dz * dz));
                if(style.edgeOfScreen)
                    drawIndicatorEdge(graphics, hit, style, angle, scale, screenMiddleX, screenMiddleY);
                else
                    drawIndicator(graphics, hit, style, angle, scale, screenMiddleX, screenMiddleY);
            }
        }

        if (ClientLatestHits.currentHitMarker != null && config.enableHitMarkers())
            drawHitMarker(graphics, ClientLatestHits.currentHitMarker, screenMiddleX, screenMiddleY);
    }

    /** The config values every indicator drawn this frame needs, read once per frame. */
    private static final class IndicatorStyle {
        final boolean edgeOfScreen;
        final float defaultScale;
        final boolean sizeDependsOnDamage;
        final boolean distanceScaling;
        final int distanceScalingCutoff;
        final int distanceFromCrosshair;
        final int opacityPercent;

        IndicatorStyle(HitIndicatorClientConfigs.Values config) {
            edgeOfScreen = config.edgeOfScreenMode();
            defaultScale = 1.0F + config.indicatorDefaultScale() / 100.0F;
            sizeDependsOnDamage = config.sizeDependsOnDamage();
            distanceScaling = config.enableDistanceScaling();
            distanceScalingCutoff = config.distanceScalingCutoff();
            distanceFromCrosshair = config.distanceFromCrosshair();
            opacityPercent = config.indicatorOpacity();
        }
    }

    private static void updateColorsIfNeeded(HitIndicatorClientConfigs.Values config) {
        String currentHitColor = config.hitIndicatorColor();
        String currentBlockColor = config.blockIndicatorColor();
        if (!lastHitColorString.equals(currentHitColor)) {
            hitColor = parseColor(currentHitColor, 0xFF0000);
            lastHitColorString = currentHitColor;
        }

        if (!lastBlockColorString.equals(currentBlockColor)) {
            blockColor = parseColor(currentBlockColor, 0x0000FF);
            lastBlockColorString = currentBlockColor;
        }
    }

    private static int parseColor(String hex, int fallback) {
        try {
            return Integer.parseInt(hex.trim(), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void drawHitMarker(GuiGraphics graphics, HitMarker hitMarker, int screenMiddleX, int screenMiddleY) {
        float opacity = hitMarker.getType() == HitMarkerType.CRIT ? 0.3F : 0.6F;

        setColor(graphics, 0xFFFFFF, opacity);
        graphics.blit(markerTexture(hitMarker.getType(), hitMarker.getLifeTime()),
                screenMiddleX - MARKER_WIDTH / 2, screenMiddleY - MARKER_HEIGHT / 2,
                0, 0, MARKER_WIDTH, MARKER_HEIGHT, MARKER_WIDTH, MARKER_HEIGHT);
    }

    private static void drawIndicator(GuiGraphics graphics, HitIndicator hit, IndicatorStyle style, float angle, float scale, int screenMiddleX, int screenMiddleY) {
        boolean nonDirectional = hit.getType() == HitIndicatorType.ND_HIT;
        int width = (int)Math.floor((nonDirectional ? ND_INDICATOR_WIDTH : INDICATOR_WIDTH) * scale);
        int height = (int)Math.floor((nonDirectional ? ND_INDICATOR_WIDTH : INDICATOR_HEIGHT) * scale);

        renderIndicator(graphics, hit, style, screenMiddleX, screenMiddleY, angle,
                screenMiddleX - width / 2,
                screenMiddleY - height / 2 - (nonDirectional ? 0 : style.distanceFromCrosshair),
                width, height);
    }

    private static void drawIndicatorEdge(GuiGraphics graphics, HitIndicator hit, IndicatorStyle style, float angle, float scale, int screenMiddleX, int screenMiddleY) {
        int width = (int)Math.floor(EDGE_INDICATOR_WIDTH * scale);
        int height = (int)Math.floor(EDGE_INDICATOR_HEIGHT * scale);
        if(width <= 0 || height <= 0)
            return;

        IndicatorMath.EdgePosition position = IndicatorMath.edgePosition(angle,
                2 * screenMiddleX, 2 * screenMiddleY, width, height, HUD_HEIGHT);
        int blitX = Math.round(position.centerX() - width / 2.0F);
        int blitY = Math.round(position.centerY() - height / 2.0F);
        renderIndicator(graphics, hit, style, blitX + width / 2.0F, blitY + height / 2.0F,
                angle, blitX, blitY, width, height);
    }

    /** Multiplier on the indicator texture's size. */
    private static float indicatorScale(HitIndicator hit, IndicatorStyle style, double distance) {
        // The original drew the non-directional indicator at a fixed size, ignoring these options.
        if(hit.getType() == HitIndicatorType.ND_HIT)
            return ND_INDICATOR_DEFAULT_SCALE;

        float scale = style.defaultScale;
        if (style.sizeDependsOnDamage)
            scale *= IndicatorMath.damageScale(hit.getDamagePercent());
        if (style.distanceScaling)
            scale *= IndicatorMath.distanceScale(distance, style.distanceScalingCutoff);
        return scale;
    }

    private static void renderIndicator(GuiGraphics graphics, HitIndicator hit, IndicatorStyle style, float centerX, float centerY, float rotAngle, int posX, int posY, int scaledTextureWidth, int scaledTextureHeight) {
        if(scaledTextureWidth <= 0 || scaledTextureHeight <= 0)
            return;

        int configuredOpacity = style.opacityPercent;
        float opacity = hit.getLifeTime() >= 25
                ? configuredOpacity
                : configuredOpacity * hit.getLifeTime() / 25.0F;
        opacity = Mth.clamp(opacity / 100.0F, 0.0F, 1.0F);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (hit.getType() != HitIndicatorType.ND_HIT) {
            pose.translate(centerX, centerY, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(rotAngle));
            pose.translate(-centerX, -centerY, 0.0F);
        }

        int rgb = hit.getType() == HitIndicatorType.BLOCK ? blockColor : hitColor;
        setColor(graphics, rgb, opacity);
        graphics.blit(indicatorTexture(hit, style), posX, posY,
                0, 0, scaledTextureWidth, scaledTextureHeight, scaledTextureWidth, scaledTextureHeight);

        pose.popPose();
    }

    private static ResourceLocation indicatorTexture(HitIndicator hit, IndicatorStyle style) {
        if (style.edgeOfScreen) return EDGE_INDICATOR;
        else if (hit.getType() == HitIndicatorType.ND_HIT) return ND_INDICATOR;
        else if (hit.getType() == HitIndicatorType.HIT) return INDICATOR;
        else return INDICATOR_BLOCK;
    }

    /** Tints what is drawn next with {@code rgb} at {@code alpha} (0 to 1). */
    private static void setColor(GuiGraphics graphics, int rgb, float alpha) {
        graphics.setColor(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, alpha);
    }

    private static ResourceLocation markerTexture(HitMarkerType type, int lifetime) {
        ResourceLocation[] frames = type == HitMarkerType.KILL ? MARKER_KILL : MARKER_CRIT;
        return lifetime > 6 ? frames[9 - lifetime] : frames[3];
    }
}
