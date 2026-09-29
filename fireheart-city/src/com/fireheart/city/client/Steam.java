package com.fireheart.city.client;

import com.fireheart.city.Dishes;
import com.fireheart.city.FireheartCity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Soft rising steam: wisps that start small, swell, curl and fade. Hot SolEats dishes give it off in hands and on the ground. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Steam extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float grow, phase;

    Steam(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.xd = vx * 0.3 + (random.nextDouble() - 0.5) * 0.004;
        this.yd = 0.012 + random.nextDouble() * 0.012 + Math.max(0, vy) * 0.3;
        this.zd = vz * 0.3 + (random.nextDouble() - 0.5) * 0.004;
        this.lifetime = 28 + random.nextInt(22);
        this.quadSize = 0.035f + random.nextFloat() * 0.03f;
        this.grow = 0.0035f + random.nextFloat() * 0.003f;
        this.phase = random.nextFloat() * 6.28f;
        float g = 0.92f + random.nextFloat() * 0.08f;
        setColor(g, g, g);
        this.alpha = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float k = age / (float) lifetime;
        quadSize += grow;
        alpha = k < 0.15f ? k / 0.15f * 0.32f : 0.32f * (1 - (k - 0.15f) / 0.85f);
        xd += Mth.sin(age * 0.25f + phase) * 0.0012;
        zd += Mth.cos(age * 0.21f + phase) * 0.0012;
        yd *= 0.985;
        xd *= 0.96;
        zd *= 0.96;
        setSprite(sprites.get(lifetime - age, lifetime));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new Steam(level, x, y, z, vx, vy, vz, sprites);
        }
    }

    static void puff(ClientLevel lv, Vec3 at, float heat) {
        if (heat <= 0.02f || lv.random.nextFloat() > 0.25f + heat * 0.6f) return;
        lv.addParticle(FireheartCity.STEAM.get(), at.x + (lv.random.nextDouble() - 0.5) * 0.12, at.y, at.z + (lv.random.nextDouble() - 0.5) * 0.12, 0, 0, 0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        if (lv == null || mc.player == null || mc.isPaused()) return;
        for (Player p : lv.players()) {
            if (p.distanceToSqr(mc.player) > 40 * 40) continue;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack s = p.getItemInHand(hand);
                if (!(s.getItem() instanceof Dishes.DishItem)) continue;
                float h = Dishes.heat(s, lv);
                if (h <= 0) continue;
                puff(lv, handPos(mc, p, hand), h);
            }
        }
        for (Entity en : lv.entitiesForRendering()) {
            if (en instanceof ItemEntity ie && ie.getItem().getItem() instanceof Dishes.DishItem && en.distanceToSqr(mc.player) < 32 * 32)
                puff(lv, en.position().add(0, 0.45, 0), Dishes.heat(ie.getItem(), lv));
            else if (en instanceof net.minecraft.world.entity.Display.ItemDisplay d && en.distanceToSqr(mc.player) < 32 * 32) {
                ItemStack s = d.getSlot(0).get();
                if (s.getItem() instanceof Dishes.DishItem) puff(lv, en.position().add(0, 0.5, 0), Math.max(0.6f, Dishes.heat(s, lv)));
            }
        }
    }

    static Vec3 handPos(Minecraft mc, Player p, InteractionHand hand) {
        boolean right = (hand == InteractionHand.MAIN_HAND) == (p.getMainArm() == HumanoidArm.RIGHT);
        if (p == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Vec3 look = p.getViewVector(1);
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(right ? 0.38 : -0.38);
            return p.getEyePosition().add(look.scale(0.75)).add(side).add(0, -0.2, 0);
        }
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x).scale(right ? -0.36 : 0.36);
        return p.position().add(fwd.scale(0.35)).add(side).add(0, 1.05, 0);
    }
}
