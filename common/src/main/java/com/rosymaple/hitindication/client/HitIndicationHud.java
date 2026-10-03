package com.rosymaple.hitindication.client;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import com.rosymaple.hitindication.latesthits.HitIndicator;
import com.rosymaple.hitindication.latesthits.HitIndicatorType;
import com.rosymaple.hitindication.latesthits.HitMarker;
import com.rosymaple.hitindication.latesthits.HitMarkerType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

/**
 * Draws the hit indicators and crit/kill markers. Registered as the last HUD element on both
 * loaders, which is where the original drew (after the whole overlay).
 */
public class HitIndicationHud {
    public static final Identifier LAYER_ID = HitIndication.id("hit_indicators");

    private static final Identifier INDICATOR = HitIndication.id("textures/hit/indicator.png");
    private static final Identifier EDGE_INDICATOR = HitIndication.id("textures/hit/edge_indicator.png");
    private static final Identifier INDICATOR_BLOCK = HitIndication.id("textures/hit/indicator_block.png");
    private static final Identifier ND_INDICATOR = HitIndication.id("textures/hit/nd_person_damage.png");
    private static final Identifier[] MARKER_CRIT = {
            HitIndication.id("textures/hit/marker_crit1.png"),
            HitIndication.id("textures/hit/marker_crit2.png"),
            HitIndication.id("textures/hit/marker_crit3.png"),
            HitIndication.id("textures/hit/marker_crit4.png")
    };
    private static final Identifier[] MARKER_KILL = {
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
    private static final int HUD_HEIGHT = 50;

    private static String lastHitColorString = HitIndicatorClientConfigs.DEFAULT_HIT_INDICATOR_COLOR;
    private static String lastBlockColorString = HitIndicatorClientConfigs.DEFAULT_BLOCK_INDICATOR_COLOR;
    private static int hitColor = 0xFF0000;
    private static int blockColor = 0x0000FF;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || mc.level == null)
            return;
        if(ClientLatestHits.latestHitIndicators.isEmpty() && ClientLatestHits.currentHitMarker == null)
            return;

        int screenMiddleX = graphics.guiWidth() / 2;
        int screenMiddleY = graphics.guiHeight() / 2;

        updateColorsIfNeeded();

        Vec3 viewVector = calculateViewVector(0, mc.player.getYRot());
        Vec2 lookVec = new Vec2((float)viewVector.x, (float)viewVector.z);
        Vec2 playerPos = new Vec2((float)mc.player.getX(), (float)mc.player.getZ());
        if (HitIndicatorClientConfigs.get().edgeOfScreenMode()) {
            for (HitIndicator hit : ClientLatestHits.latestHitIndicators)
                drawIndicatorEdge(graphics, hit, screenMiddleX, screenMiddleY, playerPos, lookVec);
        } else {
            for (HitIndicator hit : ClientLatestHits.latestHitIndicators)
                drawIndicator(graphics, hit, screenMiddleX, screenMiddleY, playerPos, lookVec);
        }

        if (ClientLatestHits.currentHitMarker != null)
            drawHitMarker(graphics, ClientLatestHits.currentHitMarker, screenMiddleX, screenMiddleY);
    }

    private static void updateColorsIfNeeded() {
        String currentHitColor = HitIndicatorClientConfigs.get().hitIndicatorColor();
        String currentBlockColor = HitIndicatorClientConfigs.get().blockIndicatorColor();
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

    private static void drawHitMarker(GuiGraphicsExtractor graphics, HitMarker hitMarker, int screenMiddleX, int screenMiddleY) {
        float opacity = hitMarker.getType() == HitMarkerType.CRIT ? 0.3F : 0.6F;

        graphics.blit(RenderPipelines.GUI_TEXTURED, markerTexture(hitMarker.getType(), hitMarker.getLifeTime()),
                screenMiddleX - MARKER_WIDTH / 2, screenMiddleY - MARKER_HEIGHT / 2,
                0, 0, MARKER_WIDTH, MARKER_HEIGHT, MARKER_WIDTH, MARKER_HEIGHT,
                ARGB.white(opacity));
    }

    private static void drawIndicator(GuiGraphicsExtractor graphics, HitIndicator hit, int screenMiddleX, int screenMiddleY, Vec2 playerPos, Vec2 lookVec) {
        Vec3 sourceVec3d = hit.getLocation();
        Vec2 diff = new Vec2((float)(sourceVec3d.x - playerPos.x), (float)(sourceVec3d.z - playerPos.y));
        float angleBetween = angleBetween(lookVec, diff);
        int distanceFromCrosshair = HitIndicatorClientConfigs.get().distanceFromCrosshair();

        Vec2 textureScale = calculateIndicatorScale(hit);
        int scaledTextureWidth = (int)Math.floor(textureScale.x);
        int scaledTextureHeight = (int)Math.floor(textureScale.y);

        renderIndicator(graphics, hit, screenMiddleX, screenMiddleY, angleBetween,
                screenMiddleX - scaledTextureWidth / 2,
                screenMiddleY - scaledTextureHeight / 2 - (hit.getType() == HitIndicatorType.ND_HIT ? 0 : distanceFromCrosshair),
                scaledTextureWidth, scaledTextureHeight);
    }

    private static void drawIndicatorEdge(GuiGraphicsExtractor graphics, HitIndicator hit, int screenMiddleX, int screenMiddleY, Vec2 playerPos, Vec2 lookVec) {
        if (hit.getType() == HitIndicatorType.ND_HIT)
            return;

        Vec3 sourceVec3d = hit.getLocation();
        Vec2 diff = new Vec2((float)(sourceVec3d.x - playerPos.x), (float)(sourceVec3d.z - playerPos.y));
        float angleBetween = angleBetween(lookVec, diff);

        Vec2 textureScale = calculateIndicatorScale(hit);
        int scaledTextureWidth = (int)Math.floor(textureScale.x);
        int scaledTextureHeight = (int)Math.floor(textureScale.y);

        int blitX, blitY;
        double targetAngle = -angleBetween;
        if (targetAngle >= 45.0F && targetAngle <= 135.0F) {
            blitX = 0;
            blitY = (int)Mth.lerp((targetAngle - 45.0F) / (90.0F), 0, 2 * screenMiddleY - scaledTextureHeight - HUD_HEIGHT);
        } else if (targetAngle <= -45.0F && targetAngle >= -135.0F) {
            blitX = 2 * screenMiddleX - scaledTextureWidth;
            blitY = (int)Mth.lerp((targetAngle + 45.0F) / (-90.0F), 0, 2 * screenMiddleY - scaledTextureHeight - HUD_HEIGHT);
        } else if (targetAngle >= 0.0F && targetAngle <= 45.0) {
            blitX = (int)Mth.lerp(targetAngle / 45.0F, screenMiddleX, 0);
            blitY = 0;
        } else if (targetAngle >= -45.0 && targetAngle <= 0.0F) {
            blitX = (int)Mth.lerp(targetAngle / (-45.0F), screenMiddleX, 2 * screenMiddleX - scaledTextureWidth);
            blitY = 0;
        } else if (targetAngle <= 180F && targetAngle >= 135.0F) {
            blitX = (int)Mth.lerp((targetAngle - 135.0F) / (45.0F), 0, screenMiddleX);
            blitY = 2 * screenMiddleY - scaledTextureHeight - HUD_HEIGHT;
        } else {
            blitX = (int)Mth.lerp((targetAngle + 135.0F) / (-45.0F), 2 * screenMiddleX - scaledTextureWidth, screenMiddleX);
            blitY = 2 * screenMiddleY - scaledTextureHeight - HUD_HEIGHT;
        }

        renderIndicator(graphics, hit, blitX + scaledTextureWidth / 2.0F, blitY + scaledTextureHeight / 2.0F,
                angleBetween, blitX, blitY, scaledTextureWidth, scaledTextureHeight);
    }

    private static Vec2 calculateIndicatorScale(HitIndicator hit) {
        if(hit.getType() == HitIndicatorType.ND_HIT)
            return new Vec2(ND_INDICATOR_WIDTH * ND_INDICATOR_DEFAULT_SCALE, ND_INDICATOR_WIDTH * ND_INDICATOR_DEFAULT_SCALE);

        HitIndicatorClientConfigs.Values config = HitIndicatorClientConfigs.get();
        float defaultScale = 1.0F + config.indicatorDefaultScale() / 100.0F;
        float scaledTextureWidthF, scaledTextureHeightF;
        if(config.edgeOfScreenMode()) {
            scaledTextureWidthF = EDGE_INDICATOR_WIDTH * defaultScale;
            scaledTextureHeightF = EDGE_INDICATOR_HEIGHT * defaultScale;
        } else {
            scaledTextureWidthF = INDICATOR_WIDTH * defaultScale;
            scaledTextureHeightF = INDICATOR_HEIGHT * defaultScale;
        }

        if (config.sizeDependsOnDamage()) {
            float scale = (hit.getDamagePercent() > 30) ? (1.0F + hit.getDamagePercent()) / 125.0F : 1.0F;
            scale = Mth.clamp(scale, 0.0F, 3.0F);
            scaledTextureWidthF *= scale;
            scaledTextureHeightF *= scale;
        }

        if (config.enableDistanceScaling()) {
            float distanceFromPlayer = calculateDistanceFromPlayer(hit.getLocation());
            float distanceScalingCutoff = config.distanceScalingCutoff();
            float distanceScaling = (distanceFromPlayer <= distanceScalingCutoff)
                    ? 0F : ((distanceFromPlayer - distanceScalingCutoff) / 10.0F);

            distanceScaling = Mth.clamp(1.0F - distanceScaling, 0.0F, 1.0F);
            scaledTextureWidthF *= distanceScaling;
            scaledTextureHeightF *= distanceScaling;
        }

        return new Vec2(scaledTextureWidthF, scaledTextureHeightF);
    }

    private static void renderIndicator(GuiGraphicsExtractor graphics, HitIndicator hit, float centerX, float centerY, float rotAngle, int posX, int posY, int scaledTextureWidth, int scaledTextureHeight) {
        if(scaledTextureWidth <= 0 || scaledTextureHeight <= 0)
            return;

        int configuredOpacity = HitIndicatorClientConfigs.get().indicatorOpacity();
        float opacity = hit.getLifeTime() >= 25
                ? configuredOpacity
                : configuredOpacity * hit.getLifeTime() / 25.0F;
        opacity = Mth.clamp(opacity / 100.0F, 0.0F, 1.0F);

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        if (hit.getType() != HitIndicatorType.ND_HIT) {
            pose.translate(centerX, centerY);
            pose.rotate(rotAngle * Mth.DEG_TO_RAD);
            pose.translate(-centerX, -centerY);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, indicatorTexture(hit), posX, posY,
                0, 0, scaledTextureWidth, scaledTextureHeight, scaledTextureWidth, scaledTextureHeight,
                indicatorColor(hit, opacity));

        pose.popMatrix();
    }

    private static Identifier indicatorTexture(HitIndicator hit) {
        if (HitIndicatorClientConfigs.get().edgeOfScreenMode()) return EDGE_INDICATOR;
        else if (hit.getType() == HitIndicatorType.ND_HIT) return ND_INDICATOR;
        else if (hit.getType() == HitIndicatorType.HIT) return INDICATOR;
        else return INDICATOR_BLOCK;
    }

    private static int indicatorColor(HitIndicator hit, float opacity) {
        int rgb = hit.getType() == HitIndicatorType.BLOCK ? blockColor : hitColor;
        return ARGB.color(opacity, rgb);
    }

    private static Identifier markerTexture(HitMarkerType type, int lifetime) {
        Identifier[] frames = type == HitMarkerType.KILL ? MARKER_KILL : MARKER_CRIT;
        return lifetime > 6 ? frames[9 - lifetime] : frames[3];
    }

    private static float angleBetween(Vec2 first, Vec2 second) {
        double dot = first.x * second.x + first.y * second.y;
        double cross = first.x * second.y - second.x * first.y;
        double res = Math.atan2(cross, dot) * 180 / Math.PI;
        return (float)res;
    }

    private static float calculateDistanceFromPlayer(Vec3 damageLocation) {
        Vec3 playerPos = Minecraft.getInstance().player.position();
        double d0 = damageLocation.x - playerPos.x;
        double d1 = damageLocation.y - playerPos.y;
        double d2 = damageLocation.z - playerPos.z;

        return (float)Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    private static Vec3 calculateViewVector(float pPitch, float pYaw) {
        float f = pPitch * ((float) Math.PI / 180F);
        float f1 = -pYaw * ((float) Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }
}
