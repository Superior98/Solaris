package com.fireheart.city.client;

import com.fireheart.city.Resident;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class ResidentModel extends PlayerModel<Resident> {
    public ResidentModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(Resident e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbAmount, age, headYaw, headPitch);
        try {
            gestures(e, age, limbAmount);
            int sky = e.skyPhase();
            if (sky > 0) {
                double vy = e.getY() - e.yo;
                float dive = sky == com.fireheart.city.Skydive.FREEFALL ? (float) Mth.clamp((-vy - 0.9) / 0.5, 0, 1) : 0;
                com.fireheart.city.client.sky.SkyPose.limbs(this, sky, e.skyTime(0), dive, 0);
                crouching = false;
            }
        } catch (Throwable ignored) {
        }
    }

    private static final float BLEND = 5f;
    private final float[] base = new float[18], from = new float[18], to = new float[18], mix = new float[18];

    private ModelPart[] parts() {
        return new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg};
    }

    private void save(float[] into) {
        ModelPart[] ps = parts();
        for (int i = 0; i < ps.length; i++) {
            into[i * 3] = ps[i].xRot;
            into[i * 3 + 1] = ps[i].yRot;
            into[i * 3 + 2] = ps[i].zRot;
        }
    }

    private void load(float[] from) {
        ModelPart[] ps = parts();
        for (int i = 0; i < ps.length; i++) {
            ps[i].xRot = from[i * 3];
            ps[i].yRot = from[i * 3 + 1];
            ps[i].zRot = from[i * 3 + 2];
        }
    }

    /** Applies the gesture pose, blending smoothly from the previous gesture instead of snapping. */
    private void gestures(Resident e, float age, float limbAmount) {
        float t = age + e.getId() * 3.1f;
        body.xRot = 0;
        body.zRot = 0;
        idle(e, t, limbAmount);
        int g = e.getGesture();
        if (e.cGest != g) {
            e.cGestPrev = e.cGest < 0 ? Resident.G_NONE : e.cGest;
            e.cGest = g;
            e.cGestAt = age;
        }
        if (age < e.cGestAt) e.cGestAt = age;
        float lt = age - e.cGestAt;
        float w = Mth.clamp(lt / BLEND, 0f, 1f);
        w = w * w * (3f - 2f * w);
        save(base);
        if (w < 1f && e.cGestPrev != g) {
            pose(e, e.cGestPrev, t, lt + 1000f);
            save(from);
            load(base);
        } else {
            System.arraycopy(base, 0, from, 0, base.length);
        }
        pose(e, g, t, lt);
        save(to);
        for (int i = 0; i < mix.length; i++) mix[i] = from[i] + (to[i] - from[i]) * w;
        load(mix);
        hat.copyFrom(head);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        jacket.copyFrom(body);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
    }

    /** Standing weight shifts, breathing, and a forward lean when running. */
    private void idle(Resident e, float t, float limbAmount) {
        if (e.isSleeping() || e.isPassenger() || e.skyPhase() > 0) return;
        body.xRot += Mth.sin(t * 0.09f) * 0.012f;
        if (limbAmount < 0.08f) {
            float cyc = t * 0.004f;
            int side = ((int) cyc & 1) == 0 ? 1 : -1;
            float f = Mth.sin((cyc - (int) cyc) * Mth.PI);
            float k = Mth.clamp(f * 3f, 0f, 1f);
            if (side > 0) {
                rightLeg.xRot -= 0.08f * k;
                rightLeg.zRot += 0.04f * k;
            } else {
                leftLeg.xRot -= 0.08f * k;
                leftLeg.zRot -= 0.04f * k;
            }
            body.zRot += 0.025f * side * k;
            head.zRot -= 0.02f * side * k;
        } else if (limbAmount > 0.55f) {
            float lean = (limbAmount - 0.55f) * 0.35f;
            body.xRot += lean;
            head.xRot -= lean * 0.6f;
        }
    }

    private void pose(Resident e, int g, float t, float lt) {
        switch (g) {
            case Resident.G_WAVE -> {
                rightArm.xRot = -2.75f;
                rightArm.yRot = 0f;
                rightArm.zRot = -0.35f + Mth.sin(t * 0.55f) * 0.4f;
            }
            case Resident.G_EAT -> {
                rightArm.xRot = -1.75f + Mth.sin(t * 1.3f) * 0.18f;
                rightArm.yRot = -0.55f;
                rightArm.zRot = 0f;
                head.xRot = 0.2f + Mth.sin(t * 1.3f) * 0.08f;
            }
            case Resident.G_GIVE -> {
                rightArm.xRot = -1.25f;
                rightArm.yRot = -0.15f;
                rightArm.zRot = 0f;
            }
            case Resident.G_CHEER -> {
                float bounce = Mth.sin(t * 0.7f) * 0.25f;
                rightArm.xRot = -2.9f + bounce;
                leftArm.xRot = -2.9f - bounce;
                rightArm.zRot = -0.25f;
                leftArm.zRot = 0.25f;
                rightArm.yRot = 0f;
                leftArm.yRot = 0f;
            }
            case Resident.G_ANGRY -> {
                rightArm.xRot = -0.85f;
                leftArm.xRot = -0.85f;
                rightArm.yRot = -0.75f;
                leftArm.yRot = 0.75f;
                rightArm.zRot = 0f;
                leftArm.zRot = 0f;
                head.xRot += Mth.sin(t * 0.9f) * 0.05f;
            }
            case Resident.G_THINK -> {
                rightArm.xRot = -2.0f;
                rightArm.yRot = -0.75f;
                rightArm.zRot = 0.1f;
                head.zRot = 0.12f;
            }
            case Resident.G_PHONE -> {
                rightArm.xRot = -1.05f;
                rightArm.yRot = -0.38f;
                rightArm.zRot = 0f;
                leftArm.xRot = -0.9f;
                leftArm.yRot = 0.42f;
                leftArm.zRot = 0f;
                head.xRot = 0.55f + Mth.sin(t * 0.07f) * 0.04f;
                head.yRot *= 0.3f;
                if ((int) (t / 6) % 5 == 0) rightArm.xRot -= 0.05f;
            }
            case Resident.G_CALL -> {
                rightArm.xRot = -2.35f;
                rightArm.yRot = -0.55f;
                rightArm.zRot = 0.5f;
                head.zRot = -0.12f;
                String sp = e.getSpeech();
                if (sp != null && !sp.isEmpty()) {
                    head.xRot += Mth.sin(t * 0.9f) * 0.05f;
                    leftArm.xRot += Mth.sin(t * 0.45f) * 0.2f - 0.15f;
                }
            }
            case Resident.G_DANCE -> {
                float b = Mth.sin(t * 0.35f);
                rightArm.xRot = -2.2f + b * 0.9f;
                leftArm.xRot = -2.2f - b * 0.9f;
                rightArm.zRot = -0.3f;
                leftArm.zRot = 0.3f;
                rightArm.yRot = 0;
                leftArm.yRot = 0;
                body.zRot = b * 0.12f;
                head.zRot = -b * 0.15f;
                head.xRot += Mth.abs(Mth.cos(t * 0.35f)) * 0.2f - 0.1f;
                rightLeg.xRot = Mth.sin(t * 0.35f + 1.5f) * 0.4f;
                leftLeg.xRot = -Mth.sin(t * 0.35f + 1.5f) * 0.4f;
            }
            case Resident.G_LAUGH -> {
                float b = Mth.sin(t * 1.4f);
                head.xRot = -0.35f + b * 0.08f;
                body.xRot = -0.08f + b * 0.03f;
                rightArm.xRot = -0.5f + b * 0.1f;
                leftArm.xRot = -0.5f - b * 0.1f;
                rightArm.zRot = 0.2f;
                leftArm.zRot = -0.2f;
            }
            case Resident.G_SAD -> {
                head.xRot = 0.55f;
                rightArm.xRot = 0.1f;
                leftArm.xRot = 0.1f;
                rightArm.zRot = 0.05f;
                leftArm.zRot = -0.05f;
                body.xRot = 0.12f;
            }
            case Resident.G_SHRUG -> {
                rightArm.xRot = -0.5f;
                leftArm.xRot = -0.5f;
                rightArm.zRot = 0.75f;
                leftArm.zRot = -0.75f;
                rightArm.yRot = 0.3f;
                leftArm.yRot = -0.3f;
                head.zRot = 0.18f;
            }
            case Resident.G_POINT -> {
                rightArm.xRot = -1.55f;
                rightArm.yRot = head.yRot;
                rightArm.zRot = 0;
            }
            case Resident.G_CLAP -> {
                float c = Mth.abs(Mth.sin(t * 1.1f));
                rightArm.xRot = -1.2f;
                leftArm.xRot = -1.2f;
                rightArm.yRot = -0.55f * c - 0.1f;
                leftArm.yRot = 0.55f * c + 0.1f;
            }
            case Resident.G_FACEPALM -> {
                rightArm.xRot = -2.1f;
                rightArm.yRot = -0.55f;
                rightArm.zRot = 0.3f;
                head.xRot = 0.45f;
            }
            case Resident.G_STRETCH -> {
                float k = Mth.sin(t * 0.08f) * 0.1f;
                rightArm.xRot = -3.0f + k;
                leftArm.xRot = -3.0f - k;
                rightArm.zRot = -0.2f;
                leftArm.zRot = 0.2f;
                head.xRot = -0.4f;
                body.xRot = -0.08f;
            }
            case Resident.G_YAWN -> {
                rightArm.xRot = -2.3f;
                rightArm.yRot = -0.5f;
                rightArm.zRot = 0.25f;
                head.xRot = -0.3f;
            }
            case Resident.G_HUGSELF -> {
                float sh = Mth.sin(t * 2.2f) * 0.03f;
                rightArm.xRot = -1.05f + sh;
                leftArm.xRot = -1.05f - sh;
                rightArm.yRot = -0.85f;
                leftArm.yRot = 0.85f;
                head.xRot = 0.25f;
            }
            case Resident.G_THUMBS -> {
                rightArm.xRot = -1.35f;
                rightArm.yRot = -0.2f;
                rightArm.zRot = 0;
            }
            case Resident.G_SURPRISED -> {
                rightArm.xRot = -1.9f;
                leftArm.xRot = -1.9f;
                rightArm.zRot = -0.45f;
                leftArm.zRot = 0.45f;
                head.xRot = -0.25f;
                body.xRot = -0.06f;
            }
            case Resident.G_NOD -> head.xRot = 0.1f + Mth.sin(t * 0.9f) * 0.3f;
            case Resident.G_HEADSHAKE -> head.yRot += Mth.sin(t * 0.9f) * 0.45f;
            case Resident.G_BOW -> {
                body.xRot = 0.55f;
                head.xRot = 0.6f;
                rightArm.xRot = 0.35f;
                leftArm.xRot = 0.35f;
            }
            case Resident.G_GUARD -> {
                float bob = Mth.sin(t * 0.35f) * 0.06f;
                rightArm.xRot = -1.35f + bob;
                rightArm.yRot = -0.35f;
                leftArm.xRot = -1.2f - bob;
                leftArm.yRot = 0.45f;
                body.xRot = 0.12f;
                rightLeg.xRot = -0.25f;
                leftLeg.xRot = 0.25f;
            }
            case Resident.G_JAB_R -> {
                rightArm.xRot = -1.57f;
                rightArm.yRot = 0.1f;
                leftArm.xRot = -1.1f;
                leftArm.yRot = 0.45f;
                body.yRot = -0.35f;
                body.xRot = 0.15f;
            }
            case Resident.G_JAB_L -> {
                leftArm.xRot = -1.57f;
                leftArm.yRot = -0.1f;
                rightArm.xRot = -1.1f;
                rightArm.yRot = -0.45f;
                body.yRot = 0.35f;
                body.xRot = 0.15f;
            }
            case Resident.G_UPPERCUT -> {
                rightArm.xRot = -2.9f;
                rightArm.zRot = 0.2f;
                leftArm.xRot = 0.6f;
                body.xRot = -0.25f;
                rightLeg.xRot = 0.35f;
                leftLeg.xRot = -0.55f;
                head.xRot = -0.4f;
            }
            case Resident.G_KICK -> {
                rightLeg.xRot = -1.45f;
                rightLeg.zRot = 0.15f;
                leftLeg.xRot = 0.1f;
                rightArm.zRot = 1.3f;
                leftArm.zRot = -1.3f;
                body.xRot = 0.25f;
            }
            case Resident.G_SLAM -> {
                float k = (Mth.sin(t * 0.8f) + 1) / 2f;
                rightArm.xRot = Mth.lerp(k, -3.0f, -0.6f);
                leftArm.xRot = Mth.lerp(k, -3.0f, -0.6f);
                rightArm.zRot = -0.25f;
                leftArm.zRot = 0.25f;
                body.xRot = Mth.lerp(k, -0.2f, 0.55f);
                rightLeg.xRot = -0.4f;
                leftLeg.xRot = 0.4f;
            }
            case Resident.G_DASH -> {
                body.xRot = 0.6f;
                head.xRot = -0.3f;
                rightArm.xRot = -1.6f;
                rightArm.yRot = 0.2f;
                leftArm.xRot = 0.9f;
                rightLeg.xRot = -0.9f;
                leftLeg.xRot = 0.8f;
            }
            case Resident.G_TASER -> {
                rightArm.xRot = -1.57f + Mth.sin(t * 3f) * 0.04f;
                rightArm.yRot = -0.05f;
                leftArm.xRot = -1.4f;
                leftArm.yRot = 0.5f;
                head.xRot = 0.05f;
            }
            case Resident.G_AIM -> {
                rightArm.xRot = -1.57f + head.xRot;
                rightArm.yRot = -0.1f + head.yRot;
                leftArm.xRot = -1.5f + head.xRot;
                leftArm.yRot = 0.45f + head.yRot;
                body.yRot = 0.05f;
            }
            case Resident.G_ULT -> {
                float k = Mth.sin(t * 0.25f) * 0.1f;
                rightArm.xRot = -0.3f;
                rightArm.zRot = 1.1f + k;
                leftArm.xRot = -0.3f;
                leftArm.zRot = -1.1f - k;
                head.xRot = -0.55f;
                body.xRot = -0.15f;
                rightLeg.zRot = 0.2f;
                leftLeg.zRot = -0.2f;
            }
            case Resident.G_HAMMER -> {
                float k = (Mth.sin(t * 0.9f) + 1) / 2f;
                rightArm.xRot = Mth.lerp(k, -2.6f, -0.7f);
                rightArm.yRot = -0.2f;
                leftArm.xRot = -0.6f;
                body.xRot = 0.2f;
                head.xRot = 0.5f;
            }
            case Resident.G_COOK -> {
                rightArm.xRot = -0.95f + Mth.sin(t * 0.5f) * 0.12f;
                rightArm.yRot = -0.3f + Mth.cos(t * 0.5f) * 0.25f;
                leftArm.xRot = -0.8f;
                leftArm.yRot = 0.35f;
                head.xRot = 0.45f;
                body.xRot = 0.08f;
            }
            case Resident.G_HOSE -> {
                rightArm.xRot = -1.3f + Mth.sin(t * 0.3f) * 0.08f;
                rightArm.yRot = -0.25f + Mth.sin(t * 0.2f) * 0.2f;
                leftArm.xRot = -1.1f;
                leftArm.yRot = 0.5f + Mth.sin(t * 0.2f) * 0.2f;
                body.xRot = 0.1f;
                rightLeg.xRot = -0.3f;
                leftLeg.xRot = 0.3f;
            }
            case Resident.G_VICTORY -> {
                rightArm.xRot = -3.0f;
                rightArm.zRot = -0.15f;
                leftArm.xRot = 0.1f;
                leftArm.zRot = -0.2f;
                head.xRot = -0.25f;
            }
            case Resident.G_PETTING -> {
                rightArm.xRot = -0.9f + Mth.sin(t * 0.5f) * 0.25f;
                rightArm.yRot = -0.2f;
                head.xRot = 0.7f;
                body.xRot = 0.25f;
            }
            case Resident.G_READ -> {
                rightArm.xRot = -1.0f;
                rightArm.yRot = -0.35f;
                leftArm.xRot = -1.0f;
                leftArm.yRot = 0.35f;
                head.xRot = 0.5f;
                head.yRot *= 0.2f;
                float page = (t % 80f) / 80f;
                if (page > 0.85f) rightArm.yRot = -0.35f + Mth.sin((page - 0.85f) / 0.15f * Mth.PI) * 0.55f;
            }
            case Resident.G_SIP -> {
                float k = (Mth.sin(t * 0.12f) + 1f) / 2f;
                k = k * k;
                rightArm.xRot = Mth.lerp(k, -1.1f, -2.25f);
                rightArm.yRot = -0.5f;
                rightArm.zRot = 0f;
                head.xRot = Mth.lerp(k, 0.15f, -0.3f);
            }
            case Resident.G_HIGHFIVE -> {
                rightArm.xRot = -2.55f;
                rightArm.yRot = -0.1f;
                rightArm.zRot = -0.1f;
                head.xRot = -0.15f;
                body.xRot = -0.05f;
            }
            case Resident.G_HUG -> {
                rightArm.xRot = -1.35f;
                rightArm.yRot = -0.65f;
                rightArm.zRot = 0f;
                leftArm.xRot = -1.35f;
                leftArm.yRot = 0.65f;
                leftArm.zRot = 0f;
                body.xRot = 0.1f;
                body.zRot = Mth.sin(t * 0.18f) * 0.05f;
                head.xRot = 0.1f;
                head.zRot = 0.15f;
            }
            case Resident.G_JOG -> {
                float p = Mth.sin(t * 0.6f);
                rightArm.xRot = -0.85f + p * 0.75f;
                leftArm.xRot = -0.85f - p * 0.75f;
                rightArm.zRot = 0.1f;
                leftArm.zRot = -0.1f;
                rightArm.yRot = 0f;
                leftArm.yRot = 0f;
                body.xRot = 0.14f;
                head.xRot -= 0.08f;
            }
            case Resident.G_YOGA_TREE -> {
                float wob = Mth.sin(t * 0.21f) * 0.03f;
                rightArm.xRot = -3.05f;
                leftArm.xRot = -3.05f;
                rightArm.zRot = -0.18f + wob;
                leftArm.zRot = 0.18f + wob;
                rightArm.yRot = 0f;
                leftArm.yRot = 0f;
                rightLeg.xRot = -0.55f;
                rightLeg.zRot = 0.95f;
                leftLeg.xRot = 0f;
                leftLeg.zRot = wob;
                body.zRot = wob;
                head.xRot = 0f;
            }
            case Resident.G_YOGA_WARRIOR -> {
                float wob = Mth.sin(t * 0.17f) * 0.03f;
                rightArm.xRot = 0f;
                leftArm.xRot = 0f;
                rightArm.zRot = 1.55f + wob;
                leftArm.zRot = -1.55f - wob;
                rightArm.yRot = 0f;
                leftArm.yRot = 0f;
                rightLeg.xRot = -0.55f;
                leftLeg.xRot = 0.55f;
                rightLeg.zRot = 0f;
                leftLeg.zRot = 0f;
                head.yRot = -0.6f;
                head.xRot = 0f;
            }
            case Resident.G_SNEEZE -> {
                if (lt < 9f) {
                    float k = lt / 9f;
                    head.xRot = -0.45f * k;
                    body.xRot = -0.12f * k;
                    rightArm.xRot = -2.1f * k;
                    rightArm.yRot = -0.55f * k;
                } else {
                    float k = Mth.clamp((lt - 9f) / 3f, 0f, 1f);
                    head.xRot = Mth.lerp(k, -0.45f, 0.6f);
                    body.xRot = Mth.lerp(k, -0.12f, 0.28f);
                    rightArm.xRot = -2.3f;
                    rightArm.yRot = -0.6f;
                }
            }
            case Resident.G_FAN -> {
                rightArm.xRot = -2.0f;
                rightArm.yRot = -0.6f + Mth.sin(t * 1.3f) * 0.35f;
                rightArm.zRot = 0.1f;
                leftArm.zRot = -0.12f;
                head.xRot = -0.12f;
            }
            case Resident.G_THROW -> {
                float k = Mth.clamp((lt - 6f) / 4f, 0f, 1f);
                rightArm.xRot = Mth.lerp(k, -2.9f, -1.0f);
                rightArm.yRot = Mth.lerp(k, 0.3f, -0.2f);
                rightArm.zRot = 0f;
                leftArm.xRot = -1.2f;
                leftArm.yRot = 0.4f;
                body.yRot = Mth.lerp(k, 0.35f, -0.25f);
                body.xRot = Mth.lerp(k, -0.1f, 0.2f);
            }
            case Resident.G_CARDS -> {
                rightArm.xRot = -1.1f;
                rightArm.yRot = -0.25f;
                leftArm.xRot = -1.1f;
                leftArm.yRot = 0.25f;
                head.xRot = 0.45f;
                float play = (t % 60f) / 60f;
                if (play > 0.8f) rightArm.xRot = -1.1f - Mth.sin((play - 0.8f) / 0.2f * Mth.PI) * 0.4f;
            }
            case Resident.G_LOOKUP -> {
                head.xRot = -0.7f;
                head.yRot *= 0.3f;
                rightArm.xRot = -2.45f;
                rightArm.yRot = -0.6f;
                rightArm.zRot = 0.35f;
                body.xRot = -0.05f;
            }
            case Resident.G_HOWL -> {
                head.xRot = -1.0f;
                head.yRot = 0f;
                rightArm.xRot = -2.0f;
                rightArm.yRot = -0.5f;
                leftArm.xRot = -2.0f;
                leftArm.yRot = 0.5f;
                body.xRot = -0.18f;
            }
            case Resident.G_SIGH -> {
                float k = Mth.sin(Mth.clamp(lt / 30f, 0f, 1f) * Mth.PI);
                head.xRot = 0.2f + 0.35f * k;
                body.xRot = 0.05f + 0.1f * k;
                rightArm.xRot = 0.05f;
                leftArm.xRot = 0.05f;
                rightArm.zRot = 0.02f;
                leftArm.zRot = -0.02f;
            }
            case Resident.G_FEED -> {
                body.xRot = 0.45f;
                head.xRot = 0.6f;
                rightArm.xRot = -0.65f + Mth.sin(t * 0.3f) * 0.12f;
                rightArm.yRot = -0.1f;
                leftArm.xRot = -0.2f;
            }
            case Resident.G_PHOTO -> {
                rightArm.xRot = -1.9f;
                rightArm.yRot = -0.35f;
                leftArm.xRot = -1.9f;
                leftArm.yRot = 0.35f;
                rightArm.zRot = 0f;
                leftArm.zRot = 0f;
                head.xRot = -0.05f;
                head.yRot *= 0.2f;
            }
            case Resident.G_SING -> {
                leftArm.xRot = -1.3f;
                leftArm.yRot = 0.9f;
                leftArm.zRot = 0f;
                rightArm.xRot = -1.3f;
                rightArm.yRot = -0.2f;
                rightArm.zRot = 0.6f + Mth.sin(t * 0.1f) * 0.2f;
                head.xRot = -0.25f + Mth.sin(t * 0.2f) * 0.05f;
                head.zRot = Mth.sin(t * 0.1f) * 0.1f;
            }
            case Resident.G_ARGUE -> {
                rightArm.xRot = -1.5f + Mth.sin(t * 0.9f) * 0.25f;
                rightArm.yRot = -0.1f;
                rightArm.zRot = 0f;
                leftArm.xRot = -0.25f;
                leftArm.yRot = 0.6f;
                leftArm.zRot = -0.5f;
                head.xRot = -0.1f + Mth.sin(t * 0.9f) * 0.05f;
                body.xRot = 0.06f;
            }
            case Resident.G_WINDED -> {
                body.xRot = 0.5f + Mth.sin(t * 0.5f) * 0.05f;
                head.xRot = -0.3f;
                rightArm.xRot = -0.55f;
                leftArm.xRot = -0.55f;
                rightArm.zRot = 0.1f;
                leftArm.zRot = -0.1f;
            }
            default -> {
                String s = e.getSpeech();
                if (s != null && !s.isEmpty() && !s.startsWith("Zzz")) {
                    switch (Math.floorMod(s.hashCode(), 3)) {
                        case 0 -> {
                            head.xRot += Mth.sin(t * 0.9f) * 0.05f;
                            rightArm.xRot += Mth.sin(t * 0.45f) * 0.14f - 0.1f;
                            leftArm.xRot += Mth.cos(t * 0.4f) * 0.08f;
                        }
                        case 1 -> {
                            float o = Mth.sin(t * 0.3f);
                            rightArm.xRot = -0.75f + o * 0.18f;
                            leftArm.xRot = -0.75f - o * 0.12f;
                            rightArm.yRot = -0.3f;
                            leftArm.yRot = 0.3f;
                            head.zRot += Mth.sin(t * 0.25f) * 0.06f;
                        }
                        default -> {
                            float beat = Mth.abs(Mth.sin(t * 0.55f));
                            rightArm.xRot = -0.6f - beat * 0.45f;
                            rightArm.yRot = -0.2f;
                            head.xRot += beat * 0.06f - 0.03f;
                        }
                    }
                }
            }
        }
    }
}
