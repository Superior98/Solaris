package com.fireheart.city.client.pc;

import com.fireheart.city.FireheartCity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.openal.AL10;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.players.MediaPlayer;

/**
 * Plays a real video file or link on the Solaris Cinema big screen through WATERMeDIA. Only touched when the
 * watermedia mod is loaded. The picture is drawn as a textured quad on the screen wall and the audio source is placed
 * at the screen so it is heard in 3D.
 */
final class CinemaVideo {
    private CinemaVideo() {}

    static final float X_LEFT = 94f, X_RIGHT = 71f, Y_TOP = 82f, Y_BOTTOM = 73f, Z = 0.975f;
    static final float SX = 82.5f, SY = 77.5f, SZ = 1f;

    static String url;
    static long startTick;
    static MRL mrl;
    static MediaPlayer player;
    static boolean seeked;
    static String status = "";
    static long lastSeen;

    static void stop() {
        if (player != null) {
            try {
                player.stop();
                player.release();
            } catch (Throwable t) {
                FireheartCity.LOG.debug("Cinema player release failed", t);
            }
        }
        player = null;
        mrl = null;
        url = null;
        seeked = false;
        status = "";
    }

    static String toMrl(String link) {
        String s = link.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) s = s.substring(1, s.length() - 1);
        return s;
    }

    /** Called every frame while the booth program is url;... Returns false if nothing drawable yet. */
    static boolean frame(String link, long programStart, long nowTick, PoseStack ps, Vec3 cam) {
        lastSeen = nowTick;
        if (!link.equals(url)) {
            stop();
            url = link;
            startTick = programStart;
            try {
                mrl = MediaAPI.mrl(toMrl(link));
                status = "Loading...";
            } catch (Throwable t) {
                status = "Can't open that link";
                FireheartCity.LOG.warn("Cinema: bad link " + link, t);
            }
        }
        if (mrl == null) return false;
        if (player == null) {
            MRL.Status st = mrl.status();
            if (st.failed()) {
                status = "Couldn't load: " + st.name().toLowerCase();
                return false;
            }
            if (!st.loaded()) return false;
            Minecraft mc = Minecraft.getInstance();
            Thread render = Thread.currentThread();
            player = MediaAPI.createPlayer(mrl, () -> MediaAPI.glEngine(render, mc), MediaAPI::alEngine);
            if (player == null) {
                status = "No video in that link";
                mrl = null;
                return false;
            }
            player.repeat(true);
            player.start();
            status = "Starting...";
        }
        if (player.error()) {
            status = "Playback error";
            return false;
        }
        if (!seeked && player.playing() && player.canSeek()) {
            seeked = true;
            long ms = Math.max(0, (nowTick - startTick) * 50L);
            long dur = player.duration();
            if (dur > 0) ms %= dur;
            if (ms > 1500) player.seek(ms);
        }
        spatialAudio(cam);
        long tex = player.texture();
        if (tex <= 0 || player.width() <= 0) return false;
        draw(ps, cam, (int) tex, player.width(), player.height());
        return true;
    }

    static void spatialAudio(Vec3 cam) {
        try {
            int src = player.audioSource();
            if (src <= 0) return;
            AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL10.alSource3f(src, AL10.AL_POSITION, SX, SY, SZ);
            AL10.alSourcef(src, AL10.AL_REFERENCE_DISTANCE, 6f);
            AL10.alSourcef(src, AL10.AL_MAX_DISTANCE, 48f);
            AL10.alSourcef(src, AL10.AL_ROLLOFF_FACTOR, 1f);
            var opts = Minecraft.getInstance().options;
            float vol = opts.getSoundSourceVolume(SoundSource.MASTER) * opts.getSoundSourceVolume(SoundSource.RECORDS);
            player.volume(Math.round(vol * 100));
        } catch (Throwable t) {
            FireheartCity.LOG.debug("Cinema audio position failed", t);
        }
    }

    static void draw(PoseStack ps, Vec3 cam, int tex, int w, int h) {
        float screenW = X_LEFT - X_RIGHT, screenH = Y_TOP - Y_BOTTOM;
        float aspect = (float) w / h;
        float dw = screenW, dh = screenW / aspect;
        if (dh > screenH) { dh = screenH; dw = screenH * aspect; }
        float cx = (X_LEFT + X_RIGHT) / 2f, cy = (Y_TOP + Y_BOTTOM) / 2f;
        float x0 = cx + dw / 2f, x1 = cx - dw / 2f, y0 = cy + dh / 2f, y1 = cy - dh / 2f;
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(m, x0, y0, Z).uv(0f, 0f).endVertex();
        b.vertex(m, x0, y1, Z).uv(0f, 1f).endVertex();
        b.vertex(m, x1, y1, Z).uv(1f, 1f).endVertex();
        b.vertex(m, x1, y0, Z).uv(1f, 0f).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.enableCull();
        ps.popPose();
    }

    /** Stop playing when nobody is near the cinema any more. */
    static void idle(long nowTick) {
        if (player != null && nowTick - lastSeen > 100) stop();
    }
}
