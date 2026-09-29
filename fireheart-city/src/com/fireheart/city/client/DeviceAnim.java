package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PcNet;
import com.fireheart.city.PhoneItem;
import com.fireheart.city.client.pc.ClientCall;
import com.fireheart.city.client.pc.ComputerScreen;
import com.fireheart.city.client.pc.PhoneScreen;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lets everyone see what a player is doing on their devices: holding the phone and tapping it, holding it to their ear
 * on a call, typing on a PC keyboard, thumbing a console pad or tapping a tablet.
 */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeviceAnim {
    private DeviceAnim() {}

    public static final int NONE = 0, PHONE = 1, CALL = 2, PC = 3, TABLET = 4, CONSOLE = 5, TV = 6, SOLBOX = 7;

    static final class State {
        int mode;
        float tap = -100, type = -100;
    }

    static final Map<Integer, Float> DON = new HashMap<>();

    static final Map<Integer, State> STATES = new HashMap<>();
    static int myMode, lastAct;

    public static void handle(String line) {
        String[] p = line.split("\\|");
        if (p.length < 3) return;
        int id;
        try { id = Integer.parseInt(p[1]); } catch (NumberFormatException e) { return; }
        Minecraft mc = Minecraft.getInstance();
        float now = mc.player == null ? 0 : mc.player.tickCount;
        if (p[0].equals("#dev")) {
            int m = Integer.parseInt(p[2]);
            if (m == NONE) STATES.remove(id);
            else STATES.computeIfAbsent(id, k -> new State()).mode = m;
        } else if (p[0].equals("#deva")) {
            State s = STATES.computeIfAbsent(id, k -> new State());
            var ent = mc.level == null ? null : mc.level.getEntity(id);
            float t = ent == null ? now : ent.tickCount;
            if (p[2].equals("don")) { DON.put(id, t); if (s.mode == NONE) STATES.remove(id); }
            else if (p[2].equals("type")) s.type = t;
            else s.tap = t;
        }
    }

    static int currentMode(Minecraft mc) {
        if (mc.screen instanceof PhoneScreen) return ClientCall.active() ? CALL : PHONE;
        if (mc.screen instanceof com.fireheart.city.client.pc.ConsoleScreen) return SOLBOX;
        if (mc.screen instanceof ComputerScreen cs) return switch (cs.data.device) {
            case 2 -> TABLET;
            case 3 -> CONSOLE;
            case 4 -> TV;
            default -> PC;
        };
        return ClientCall.active() ? CALL : NONE;
    }

    /** Called by the phone/PC screens on every click or key press. */
    public static void activity(boolean typing) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        State s = STATES.computeIfAbsent(mc.player.getId(), k -> new State());
        if (typing) s.type = mc.player.tickCount;
        else s.tap = mc.player.tickCount;
        if (mc.player.tickCount - lastAct < 3) return;
        lastAct = mc.player.tickCount;
        PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "devact", typing ? "type" : "tap", ""));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            STATES.clear();
            myMode = 0;
            return;
        }
        int m = currentMode(mc);
        if (m != myMode) {
            myMode = m;
            if (m == NONE) STATES.remove(mc.player.getId());
            else STATES.computeIfAbsent(mc.player.getId(), k -> new State()).mode = m;
            PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "devmode", String.valueOf(m), ""));
        }
    }

    static float pulse(float age, float at, float len) {
        float d = age - at;
        return d < 0 || d > len ? 0 : 1 - d / len;
    }

    /** Runs at the end of PlayerModel.setupAnim (see PlayerModelMixin). */
    public static void apply(PlayerModel<?> m, LivingEntity e, float age) {
        if (!(e instanceof Player)) return;
        Float don = DON.get(e.getId());
        if (don != null) {
            float d = age - don;
            if (d < 0 || d > 18) DON.remove(e.getId());
            else {
                float k = d < 9 ? d / 9f : (18 - d) / 9f;
                k = Mth.sin(k * Mth.HALF_PI);
                m.rightArm.xRot = Mth.lerp(k, m.rightArm.xRot, -2.9f);
                m.rightArm.zRot = Mth.lerp(k, m.rightArm.zRot, -0.55f);
                m.rightArm.yRot = 0;
                m.leftArm.xRot = Mth.lerp(k, m.leftArm.xRot, -2.9f);
                m.leftArm.zRot = Mth.lerp(k, m.leftArm.zRot, 0.55f);
                m.leftArm.yRot = 0;
                m.head.xRot = Mth.lerp(k, m.head.xRot, 0.15f);
                m.hat.copyFrom(m.head);
                m.leftSleeve.copyFrom(m.leftArm);
                m.rightSleeve.copyFrom(m.rightArm);
                return;
            }
        }
        State s = STATES.get(e.getId());
        if (s == null || s.mode == NONE) return;
        if (com.fireheart.city.client.sky.SkyClient.skydiving(e)) return;
        float tap = pulse(age, s.tap, 6), typing = pulse(age, s.type, 12);
        float poke = Mth.sin(tap * Mth.PI) * 0.22f;
        float jit = typing > 0 ? Mth.sin(age * 2.6f) * 0.09f * Math.min(1, typing * 2) : 0;
        switch (s.mode) {
            case PHONE -> {
                m.rightArm.xRot = -1.1f - poke - jit;
                m.rightArm.yRot = -0.35f;
                m.rightArm.zRot = 0;
                m.leftArm.xRot = -1.0f + jit;
                m.leftArm.yRot = 0.45f;
                m.leftArm.zRot = 0;
                m.head.xRot = 0.62f;
                m.head.yRot *= 0.25f;
            }
            case CALL -> {
                m.rightArm.xRot = -2.35f;
                m.rightArm.yRot = -0.55f;
                m.rightArm.zRot = 0.55f;
                m.head.zRot = -0.12f;
                if (typing > 0 || tap > 0) {
                    m.leftArm.xRot = -1.0f + jit - poke;
                    m.leftArm.yRot = 0.4f;
                } else m.leftArm.xRot += Mth.sin(age * 0.15f) * 0.12f - 0.1f;
            }
            case PC -> {
                m.rightArm.xRot = -1.25f + jit - poke * 0.4f;
                m.rightArm.yRot = -0.12f - (tap > 0 ? 0.25f * tap : 0);
                m.rightArm.zRot = 0;
                m.leftArm.xRot = -1.25f - jit;
                m.leftArm.yRot = 0.15f;
                m.leftArm.zRot = 0;
                m.head.xRot = 0.22f;
            }
            case TABLET -> {
                m.rightArm.xRot = -0.95f - poke - jit;
                m.rightArm.yRot = -0.5f;
                m.leftArm.xRot = -0.95f + jit * 0.5f;
                m.leftArm.yRot = 0.55f;
                m.head.xRot = 0.5f;
            }
            case CONSOLE, SOLBOX -> {
                float mash = Math.max(typing, tap) > 0 ? Mth.sin(age * 3.1f) * 0.07f : 0;
                m.rightArm.xRot = -1.05f + mash;
                m.rightArm.yRot = -0.42f;
                m.leftArm.xRot = -1.05f - mash;
                m.leftArm.yRot = 0.42f;
                m.head.xRot = 0.3f;
            }
            case TV -> {
                m.rightArm.xRot = -1.15f - poke * 1.5f;
                m.rightArm.yRot = -0.15f;
                m.rightArm.zRot = 0;
                m.head.xRot *= 0.3f;
            }
            default -> {}
        }
        m.hat.copyFrom(m.head);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
    }

    static ItemStack saved;
    static int savedFor = -1;

    @SubscribeEvent
    public static void onRenderPre(RenderPlayerEvent.Pre e) {
        State s = STATES.get(e.getEntity().getId());
        if (s == null || s.mode == NONE || s.mode == PC) return;
        Player p = e.getEntity();
        ItemStack held = p.getMainHandItem();
        ItemStack show = switch (s.mode) {
            case TABLET -> new ItemStack(FireheartCity.TABLET.get());
            case CONSOLE -> new ItemStack(FireheartCity.CONSOLE.get());
            case SOLBOX -> new ItemStack(FireheartCity.CONTROLLER.get());
            case TV -> new ItemStack(FireheartCity.REMOTE.get());
            default -> held.getItem() instanceof PhoneItem ? held : PhoneItem.make(0);
        };
        if (show == held) return;
        saved = held;
        savedFor = p.getId();
        p.setItemSlot(EquipmentSlot.MAINHAND, show);
    }

    @SubscribeEvent
    public static void onRenderPost(RenderPlayerEvent.Post e) {
        if (savedFor == e.getEntity().getId() && saved != null) {
            e.getEntity().setItemSlot(EquipmentSlot.MAINHAND, saved);
            saved = null;
            savedFor = -1;
        }
    }
}
