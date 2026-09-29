package com.fireheart.city;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;

public class Resident extends PathfinderMob {
    public static final int SKINS = 34;
    public static final int FIRST_SLIM = 20;
    public static final int G_NONE = 0, G_WAVE = 1, G_EAT = 2, G_GIVE = 3, G_CHEER = 4, G_ANGRY = 5, G_THINK = 6, G_PHONE = 7, G_CALL = 8,
            G_DANCE = 9, G_LAUGH = 10, G_SAD = 11, G_SHRUG = 12, G_POINT = 13, G_CLAP = 14, G_FACEPALM = 15, G_STRETCH = 16, G_YAWN = 17, G_HUGSELF = 18,
            G_THUMBS = 19, G_SURPRISED = 20, G_NOD = 21, G_HEADSHAKE = 22, G_BOW = 23, G_HEADPHONES = 24, G_REMOTE = 25, G_PETTING = 26,
            G_GUARD = 27, G_JAB_R = 28, G_JAB_L = 29, G_UPPERCUT = 30, G_KICK = 31, G_SLAM = 32, G_DASH = 33, G_TASER = 34, G_VICTORY = 35, G_AIM = 36, G_ULT = 37, G_HAMMER = 38, G_HOSE = 39, G_COOK = 40,
            G_READ = 41, G_SIP = 42, G_HIGHFIVE = 43, G_HUG = 44, G_JOG = 45, G_YOGA_TREE = 46, G_YOGA_WARRIOR = 47, G_SNEEZE = 48, G_FAN = 49, G_THROW = 50,
            G_CARDS = 51, G_LOOKUP = 52, G_HOWL = 53, G_SIGH = 54, G_FEED = 55, G_PHOTO = 56, G_SING = 57, G_ARGUE = 58, G_WINDED = 59,
            G_SHAKE = 60, G_SALUTE = 61, G_BLOW_KISS = 62, G_NAP = 63, G_WHISTLE = 64, G_CONFETTI = 65, G_KNOCK = 66, G_COUGH = 67, G_PICKUP = 68;
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(Resident.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.defineId(Resident.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> GESTURE = SynchedEntityData.defineId(Resident.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> VOICE_ID = SynchedEntityData.defineId(Resident.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> WEATHER = SynchedEntityData.defineId(Resident.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKY = SynchedEntityData.defineId(Resident.class, EntityDataSerializers.INT);
    public static final int W_UMBRELLA = 1, W_SHIVER = 2, W_SUN = 4;
    private int sunTicks;
    private long lastShiverLine = -100000;
    private long lastUmbrellaLine = -100000;

    private String profileId = "";
    private String insideKey = null;
    private int shuttleTicks = 0;
    private boolean shuttleToIsland;
    private boolean skyRider;
    private int skyFrom;
    private int skyTicks;
    private int speechTicks;
    private int gestureTicks;
    private int heldTicks;
    private int eatTicks;
    private String eatItem;
    private int fleeTicks;
    private Vec3 fleeTarget;
    private long lastConvoTick = -100000;
    private long leisureDay = -1;
    private String leisureKey = null;
    private String leisureWhy = "";
    private long lunchDay = -1;
    private String lunchKey = null;
    private long morningDay = -1;
    private String morningKey = null;
    private long lastShop = -100000;
    private long rainDay = -1;
    private int hungerAcc, socialAcc, funAcc, workAcc;
    private final Map<String, Long> lastGreet = new HashMap<>();
    private BlockPos bedTarget;
    private BlockPos seatTarget;
    private int sitTicks;
    private int seekTicks;
    private int restCooldown;
    private String sitAct = "";
    private long lastRead = -100000;
    private String partyStar = "";
    private BlockPos waterSpot;
    private long lastGiftDay = -1;
    public final Work work = new Work();
    private long shopSlot = -1;
    private static final String KEY_BANK = "bank";
    private int atmStep;
    private int listenTicks;
    private Resident listenTo;
    private long listenDay = -1;
    private int civicStep;
    private long lastCivicLine = -100000;
    private boolean bankerShift;
    private boolean musicianShift;
    private boolean deskShift;
    private boolean nightCop;
    private int bankWait;
    public Conversation convo;
    public String cSpeech = "";
    public int cSkyPhase, cSkyStart;

    public float skyTime(float pt) {
        int ph = skyPhase();
        if (ph != cSkyPhase) {
            cSkyPhase = ph;
            cSkyStart = tickCount;
        }
        return tickCount - cSkyStart + pt;
    }
    public String cPrev = "";
    public int cGest = -1, cGestPrev;
    public float cGestAt;
    public float cStart;
    public float cEnd = -100;
    public int cBlip;

    public Resident(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        ((GroundPathNavigation) this.getNavigation()).setCanPassDoors(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN, 0);
        this.entityData.define(SPEECH, "");
        this.entityData.define(GESTURE, 0);
        this.entityData.define(VOICE_ID, "");
        this.entityData.define(WEATHER, 0);
        this.entityData.define(SKY, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ConversationGoal(this));
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new MakeWayGoal(this));
        this.goalSelector.addGoal(3, new CommuteGoal(this));
        this.goalSelector.addGoal(5, new StayNearGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new MingleGoal(this));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public void bind(CityData.Profile p) {
        this.profileId = p.id;
        p.skin = Cast.skinFor(p.id, p.skin);
        this.entityData.set(SKIN, Math.floorMod(p.skin, SKINS));
        this.entityData.set(VOICE_ID, Cast.voiceFor(p.id));
        p.entity = this.getUUID();
        refreshLooks(p);
    }

    public void refreshLooks(CityData.Profile p) {
        String stars = p.tier > 0 ? " §e" + "★".repeat(p.tier) : "";
        String heart = p.partner.isEmpty() ? "" : " §c❤";
        String col = switch (Math.max(0, Math.min(3, p.level))) { case 3 -> "§6"; case 2 -> "§b"; case 1 -> "§f"; default -> "§7"; };
        boolean mayor = level() instanceof ServerLevel && Mayor.isMayor(data(), p.id);
        this.setCustomName(Component.literal((mayor ? "§6§lMayor §r" + col : col) + p.name + " · " + p.jobTitle() + stars + heart));
        this.setCustomNameVisible(true);
        if (this.isInvisible()) this.setInvisible(false);
        if (heldTicks <= 0) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(p.job.tool));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public CityData data() {
        return CityData.get((ServerLevel) this.level());
    }

    public CityData.Profile profile() {
        return data().profiles.get(profileId);
    }

    public String profileId() {
        return profileId;
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    public String getSpeech() {
        return this.entityData.get(SPEECH);
    }

    public int getGesture() {
        return this.entityData.get(GESTURE);
    }

    public int getWeather() {
        return this.entityData.get(WEATHER);
    }

    public int umbrellaColor() {
        return Math.floorMod(profileId.hashCode(), 16);
    }

    public String getVoiceId() {
        String v = this.entityData.get(VOICE_ID);
        return v == null || v.isEmpty() ? Cast.voiceFor(profileId) : v;
    }

    public void gesture(int g, int ticks) {
        this.entityData.set(GESTURE, g);
        this.gestureTicks = ticks;
        if (!(level() instanceof ServerLevel sl)) return;
        ParticleOptions fx = switch (g) {
            case G_LAUGH, G_CHEER, G_CLAP -> ParticleTypes.HAPPY_VILLAGER;
            case G_ANGRY -> ParticleTypes.ANGRY_VILLAGER;
            case G_SAD -> ParticleTypes.FALLING_WATER;
            case G_DANCE -> ParticleTypes.NOTE;
            case G_SURPRISED -> ParticleTypes.CRIT;
            case G_YAWN -> ParticleTypes.CLOUD;
            case G_PETTING -> ParticleTypes.HEART;
            default -> null;
        };
        if (fx != null) sl.sendParticles(fx, getX(), getEyeY() + 0.6, getZ(), g == G_SAD ? 3 : 2, 0.25, 0.1, 0.25, 0.02);
    }

    /** Picks a body-language gesture that fits what is being said. */
    static int emoteFor(String text) {
        String t = " " + text.toLowerCase(java.util.Locale.ROOT) + " ";
        if (t.contains("haha") || t.contains(" lol") || t.contains("hehe") || t.contains("lmao") || t.contains("crying")) return G_LAUGH;
        if (t.contains("no way") || t.contains("what?!") || t.contains("!?") || t.contains("seriously?") || t.contains("oh my") || t.contains("wow")) return G_SURPRISED;
        if (t.contains("ugh") || t.contains("oops") || t.contains("my bad") || t.contains("silly me")) return G_FACEPALM;
        if (t.contains("no idea") || t.contains("not sure") || t.contains("who knows") || t.contains("couldn't tell") || t.contains("dunno")) return G_SHRUG;
        if (t.contains("sorry to hear") || t.contains("not great") || t.contains(":(") || t.contains("aww, okay") || t.contains("miss ")) return G_SAD;
        if (t.contains("yay") || t.contains("amazing") || t.contains("woo") || t.contains("best ")) return G_CHEER;
        if (t.contains("great") || t.contains("nice") || t.contains("perfect") || t.contains("deal") || t.contains("good job")) return G_THUMBS;
        if (t.startsWith(" yes") || t.startsWith(" yeah") || t.startsWith(" sure") || t.startsWith(" okay") || t.startsWith(" yep") || t.contains("of course")) return G_NOD;
        if (t.startsWith(" no ") || t.startsWith(" nope") || t.startsWith(" nah") || t.contains("i'd rather not") || t.contains("no thanks")) return G_HEADSHAKE;
        if (t.contains("over there") || t.contains("look at") || t.contains("blocks ") || t.contains("that way")) return G_POINT;
        if (t.contains("hmm") || t.contains("let me think") || t.contains("good question")) return G_THINK;
        if (t.contains("*yawns*") || t.contains("tired") || t.contains("sleepy")) return G_YAWN;
        if (t.startsWith(" hi") || t.startsWith(" hey") || t.startsWith(" hello") || t.contains("bye")) return G_WAVE;
        return G_NONE;
    }

    public long timeOfDay() {
        return Math.floorMod(this.level().getDayTime(), 24000L);
    }

    public long day() {
        return Calendar.dayOf(this.level().getDayTime());
    }

    public long routineDay() {
        return Calendar.dayOf(this.level().getDayTime() + 1500L);
    }

    public boolean onIsland() {
        return this.getY() > 150;
    }

    public boolean isSkyRider() {
        return skyRider;
    }

    public boolean inShuttle() {
        return shuttleTicks > 0 || skyRider || getVehicle() instanceof Ferry;
    }

    private int shift() {
        return Math.floorMod(profileId.hashCode(), 8) * 330;
    }

    private long actAt = Long.MIN_VALUE, actDayTime = Long.MIN_VALUE, destAt = Long.MIN_VALUE, destDayTime = Long.MIN_VALUE;
    private String actCache;
    private Place destCache;

    public String activityName() {
        if (level().isClientSide) return computeActivity();
        long g = level().getGameTime(), dt = level().getDayTime();
        if (actCache == null || g != actAt || dt != actDayTime) {
            actCache = computeActivity();
            actAt = g;
            actDayTime = dt;
        }
        return actCache;
    }

    public void rethink() {
        actCache = null;
        destAt = Long.MIN_VALUE;
    }

    private String computeActivity() {
        long t = timeOfDay();
        if (!level().isClientSide && Health.sick(this)) return t >= 14500 && t < 23000 ? "sleep" : "evening";
        if (!level().isClientSide && Party.forcedNow((ServerLevel) level())) return "leisure";
        if (!level().isClientSide && Festival.live(profileId)) return "leisure";
        if (!level().isClientSide && Meets.due(profileId)) return "leisure";
        long workStart = (23300 + shift()) % 24000;
        boolean beforeWork = workStart > 20000 ? (t >= 22500 && t < workStart) : (t >= 22500 || t < workStart);
        if (beforeWork) return "morning";
        if (nightCop) {
            if (t >= 12500 && t < 23600) return "work";
            if (t >= 23600 || t < 1000) return "evening";
            if (t < 8500) return "sleep";
            return t < 11500 ? "leisure" : "morning";
        }
        if (deskShift) {
            if (t < 5000 || t >= 22500) return "leisure";
            if (t < 15800) return "work";
            return t < 16200 ? "evening" : "sleep";
        }
        if (musicianShift) {
            if (t < 4800 || t >= 22500) return "leisure";
            if (t < 12600) return "work";
            return t < 14000 ? "evening" : "sleep";
        }
        boolean off = Calendar.weekend(routineDay());
        if (t >= 12200 && t < Stars.END && !level().isClientSide && Stars.tonight(profileId, Calendar.dayOf(level().getDayTime()))) return "leisure";
        if (t < 5500 || t >= 22500) return off ? "leisure" : "work";
        if (t < 6500) return "lunch";
        if (t < 10500) return off ? "leisure" : "work";
        if (bankerShift && !off && t < 12300) return "work";
        if (t < 12600 + shift() / 4) return "leisure";
        return t < 14000 ? "evening" : "sleep";
    }

    public boolean workingAt(long t, long rday) {
        long workStart = (23300 + shift()) % 24000;
        boolean beforeWork = workStart > 20000 ? (t >= 22500 && t < workStart) : (t >= 22500 || t < workStart);
        if (beforeWork) return false;
        if (nightCop) return t >= 12500 && t < 23600;
        if (deskShift) return t >= 5000 && t < 15800;
        if (musicianShift) return t >= 4800 && t < 12600;
        if (Calendar.weekend(rday)) return false;
        if (t < 5500 || t >= 22500) return true;
        if (t < 6500) return false;
        if (t < 10500) return true;
        return bankerShift && t < 12300;
    }

    public static boolean asleepAt(long t) {
        return t >= 14500 && t < 22300;
    }

    public boolean weekendNow() {
        return Calendar.weekend(routineDay());
    }

    private long slot() {
        long rd = routineDay();
        if (!weekendNow()) return rd * 4 + 3;
        long t = timeOfDay();
        return rd * 4 + ((t >= 22500 || t < 5500) ? 0 : t < 10500 ? 1 : 2);
    }

    public boolean workBusy() {
        return work.target() != null && activityName().equals("work");
    }

    public boolean isFree() {
        String a = activityName();
        return !onErrand() && skyPhase() == 0 && !inShuttle() && convo == null && phoneMode != 2 && eatTicks <= 0 && listenTicks <= 0 && !a.equals("sleep") && !isSleeping() && !Elevator.isRider(this);
    }

    public boolean isEating() {
        return eatTicks > 0;
    }

    public BlockPos homePos() {
        CityData.Profile p = profile();
        Place h = p == null ? null : p.homePlace();
        return h == null ? null : h.pos;
    }

    public Place workPlace() {
        CityData.Profile p = profile();
        if (p == null) return null;
        if (p.job == Job.GARDENER) {
            int w = Calendar.weekday(routineDay());
            if (w == 1 || w == 3) return Place.get("gardens");
        }
        if (p.job == Job.CLERK) {
            int w = Calendar.weekday(routineDay());
            if (w == 1 || w == 3 || w == 5) return Place.get(TechStore.KEY);
        }
        return p.job.work();
    }

    public Place destination() {
        long g = level().getGameTime(), dt = level().getDayTime();
        if (g == destAt && dt == destDayTime) return destCache;
        Place d = computeDestination();
        destCache = d;
        destAt = g;
        destDayTime = dt;
        return d;
    }

    private Place computeDestination() {
        CityData.Profile p = profile();
        if (p == null) return null;
        String a = activityName();
        if (fleeTicks > 0) return null;
        if (!a.equals("work") && !a.equals("sleep") && !onIsland() && Bank.needsCash(data(), p, routineDay())) return Place.get(Bank.ATM);
        boolean hungry = p.hunger < 30 && Economy.foodCount(p) == 0 && p.coins >= 1;
        if (p.hunger < 12 && Economy.foodCount(p) == 0 && p.coins < 2 && Bank.savings(data(), p.id) < 2 && !a.equals("work") && !a.equals("sleep") && !Party.forcedNow((ServerLevel) level()) && p.homePlace() != null) return p.homePlace();
        if (hungry && !a.equals("work") && !a.equals("sleep")) {
            Place shop = nearestShop(p);
            if (shop != null) return shop;
        }
        switch (a) {
            case "work":
                return workPlace();
            case "lunch":
                return Place.get(lunchChoice(p));
            case "leisure":
                return Place.get(leisureChoice(p));
            case "morning":
                return Place.get(morningChoice(p));
            default:
                return p.homePlace();
        }
    }

    private Place nearestShop(CityData.Profile p) {
        Place best = nearestShop(p, true);
        return best != null ? best : nearestShop(p, false);
    }

    private Place nearestShop(CityData.Profile p, boolean sameSide) {
        CityData d = data();
        Place best = null;
        double bd = Double.MAX_VALUE;
        for (Job j : Job.values()) {
            if (!Economy.sellsFood(j)) continue;
            Place pl = j.work();
            if (sameSide && pl.island != onIsland()) continue;
            if (!Shop.hasFood((ServerLevel) level(), d, j)) continue;
            double dd = this.distanceToSqr(Vec3.atCenterOf(pl.pos));
            if (dd < bd) { bd = dd; best = pl; }
        }
        return best;
    }

    private String morningChoice(CityData.Profile p) {
        if (morningDay == routineDay() && morningKey != null) return morningKey;
        morningDay = routineDay();
        morningKey = p.home;
        if (Economy.foodCount(p) == 0 && p.coins >= 3 && getRandom().nextFloat() < 0.6f) {
            morningKey = p.livesOnIsland() ? "noodle" : "bakery";
        }
        return morningKey;
    }

    private String lunchChoice(CityData.Profile p) {
        if (lunchDay == routineDay() && lunchKey != null) return lunchKey;
        lunchDay = routineDay();
        CityData d = data();
        if (!p.livesOnIsland() && !onIsland() && Bank.open((ServerLevel) level(), d) && Math.floorMod(p.id.hashCode() + routineDay(), 3) == 0 && KEY_BANK.equals(Bank.errand(this, d, p))) {
            lunchKey = KEY_BANK;
            leisureWhy = "bank";
            return lunchKey;
        }
        boolean isleWorker = p.job.work().island;
        String[] eateries = isleWorker ? new String[]{"noodle"} : new String[]{"diner", "bakery", "market", "diner"};
        lunchKey = eateries[getRandom().nextInt(eateries.length)];
        if (p.job == Job.COOK) lunchKey = "diner";
        if (p.job == Job.NOODLE_CHEF) lunchKey = "noodle";
        if (!p.partner.isEmpty()) {
            CityData.Profile q = d.profiles.get(p.partner);
            if (q != null && q.job.work().island == isleWorker && Economy.sellsFood(q.job) && getRandom().nextFloat() < 0.5f) {
                lunchKey = q.job.workKey;
                return lunchKey;
            }
        }
        for (String fid : d.friendsOf(p.id)) {
            CityData.Profile f = d.profiles.get(fid);
            if (f != null && d.rel(p.id, fid).knowsJob && f.job.work().island == isleWorker && getRandom().nextFloat() < 0.45f && !f.job.workKey.equals(p.job.workKey)) {
                lunchKey = f.job.workKey;
                break;
            }
        }
        if (Economy.foodCount(p) > 0 && getRandom().nextFloat() < 0.25f && !level().isRaining()) lunchKey = isleWorker ? "isle_plaza" : (getRandom().nextBoolean() ? "park" : "plaza");
        return lunchKey;
    }

    private String leisureChoice(CityData.Profile p) {
        boolean starsDue = timeOfDay() > 11500 && !"stargaze".equals(leisureWhy) && Stars.tonight(profileId, day());
        if (!starsDue && !"tour".equals(leisureWhy) && Tours.onTour(data(), profileId, day())) starsDue = true;
        if (!starsDue && !"worship".equals(leisureWhy) && Festival.live(profileId)) starsDue = true;
        if (!starsDue && !"meet".equals(leisureWhy) && Meets.due(profileId)) starsDue = true;
        if (!starsDue && !"fireworks".equals(leisureWhy) && timeOfDay() > Fireworks.SHOW_START - 1500 && timeOfDay() < Fireworks.SHOW_END && leisureDay == slot()) {
            for (CityData.Plan q : data().plansFor(p.id, day())) if (q.what.equals("fireworks")) starsDue = true;
        }
        if (leisureDay == slot() && leisureKey != null && !starsDue) return leisureKey;
        leisureDay = slot();
        leisureReason = "";
        CityData d = data();
        boolean rain = level().isRaining();
        List<CityData.Plan> plans = d.plansFor(p.id, day());
        if (timeOfDay() <= 11500) plans.removeIf(x -> x.what.equals("stargaze"));
        if (!Festival.live(profileId)) plans.removeIf(x -> x.what.equals("worship"));
        if (!Meets.due(profileId)) plans.removeIf(x -> x.what.equals("meet"));
        if (timeOfDay() <= Fireworks.SHOW_START - 1500 || timeOfDay() > Fireworks.SHOW_END) plans.removeIf(x -> x.what.equals("fireworks"));
        if (!plans.isEmpty()) {
            CityData.Plan pl = plans.get(0);
            for (CityData.Plan q : plans) if (q.what.equals("dance")) { pl = q; break; }
            boolean forcedDance = pl.what.equals("dance") && Party.forcedNow((ServerLevel) level());
            if (!forcedDance) for (CityData.Plan q : plans) if (q.what.equals("speech") || q.what.equals("lottery") || q.what.equals("fireworks") && timeOfDay() > Fireworks.SHOW_START - 1500 || q.what.equals("stargaze") && timeOfDay() > 11500 || q.what.equals("tour") || q.what.equals("worship") && Festival.live(profileId) || q.what.equals("meet")) { pl = q; break; }
            for (CityData.Plan q : plans) if (q.what.equals("party")) { pl = q; break; }
            for (CityData.Plan q : plans) if (q.what.equals("meet") && Meets.due(profileId) && q.place.equals(Meets.active(profileId).place)) { pl = q; break; }
            leisureKey = pl.place;
            leisureWhy = pl.what;
            partyStar = pl.what.equals("party") && !pl.who.isEmpty() ? pl.who.get(0) : "";
            return leisureKey;
        }
        String errand = Bank.errand(this, d, p);
        if (errand != null) {
            leisureKey = errand;
            leisureWhy = "bank";
            bankWait = 0;
            atmStep = 0;
            return leisureKey;
        }
        return thinkLeisure(p, d, rain);
    }

    private String leisureReason = "";
    private java.util.UUID seekPlayer;
    private boolean seekFollow;
    private long seekUntil;

    private String thinkLeisure(CityData.Profile p, CityData d, boolean rain) {
        Mind mind = p.mind;
        long dd = day();
        boolean isle = p.livesOnIsland();
        boolean saving = mind.intentKind.equals("save") && mind.intentDay == dd;
        boolean resting = mind.intentKind.equals("rest") && mind.intentDay == dd;
        java.util.List<Mind.Choice> opts = new java.util.ArrayList<>();
        java.util.Map<Mind.Choice, Runnable> after = new java.util.HashMap<>();
        if (p.coins >= 4) {
            Place shop = Economy.foodCount(p) == 0 ? nearestShop(p) : null;
            String[] shops = isle ? new String[]{"arcade", "noodle"} : new String[]{"market", "supply", "bakery", "library", "park", "diner"};
            String key = shop != null ? shop.key : shops[getRandom().nextInt(shops.length)];
            double sc = (weekendNow() && p.coins >= 8 ? 0.5 : -0.4) + (shop != null ? 1.6 : 0) - (saving ? 2.5 : 0);
            opts.add(new Mind.Choice(key, "shopping", shop != null ? "I need food" : "I fancy a bit of shopping", sc));
        }
        if (!p.wantDevice.isEmpty() && TechStore.price(p.wantDevice) > 0 && TechStore.stock(d, TechStore.stockKey(p.wantDevice)) > 0 && (!p.wantDevice.equals("phone2") || Extras.launched(d, dd)) && p.coins + Bank.savings(d, p.id) >= TechStore.price(p.wantDevice)) {
            opts.add(new Mind.Choice(TechStore.KEY, "tech", p.wantDevice.equals("repair") ? "my phone screen needs fixing" : "I want a new " + TechStore.deviceName(p.wantDevice), 2.2 + (weekendNow() ? 0.5 : 0) - (saving ? 1.5 : 0) + (p.wantDevice.equals("repair") ? 1.0 : 0)));
        }
        for (String fid : d.friendsOf(p.id)) {
            CityData.Profile f = d.profiles.get(fid);
            if (f == null || !f.ownsPC || f.pcPos == null || !d.rel(p.id, fid).knowsHome || !d.plansFor(fid, dd).isEmpty() || f.livesOnIsland() != isle) continue;
            Mind.Choice c = new Mind.Choice(f.home, "gaming", "gaming night at " + f.name + "'s place", -0.3 + (p.fun < 40 ? 0.8 : 0.2) + (p.trait == Trait.ADVENTUROUS || p.trait == Trait.TALKATIVE ? 0.4 : 0));
            opts.add(c);
            after.put(c, () -> d.addPlan(dd, f.home, "gaming", p.id, fid));
            break;
        }
        if (!p.partner.isEmpty()) {
            CityData.Profile q = d.profiles.get(p.partner);
            if (q != null && d.plansFor(q.id, dd).isEmpty()) {
                String spot = rain ? (isle ? "noodle" : "diner") : bestOf(p, d, Place.DATE_SPOTS);
                CityData.Rel rr = d.rel(p.id, q.id);
                Mind.Choice c = new Mind.Choice(spot, "date", "date night with " + q.name, 0.9 + rr.romance / 60.0 + (p.social < 40 ? 0.5 : 0));
                opts.add(c);
                after.put(c, () -> d.addPlan(dd, spot, "date", p.id, q.id));
            }
        }
        for (String fid : d.friendsOf(p.id)) {
            CityData.Profile f = d.profiles.get(fid);
            if (f == null || !d.rel(p.id, fid).knowsHome || !d.plansFor(fid, dd).isEmpty()) continue;
            boolean planned = mind.intentKind.equals("friend") && mind.intentTarget.equals(fid) && mind.intentDay == dd;
            Mind.Choice c = new Mind.Choice(f.home, "visit", planned ? "I promised myself I'd see " + f.name : "dropping by " + f.name + "'s place", 0.1 + (p.social < 40 ? 1.0 : 0.3) + (planned ? 2.0 : 0) + d.rel(p.id, fid).aff / 80.0);
            opts.add(c);
            after.put(c, () -> {
                d.addPlan(dd, f.home, "visit", p.id, fid);
                d.news(dd, p.name + " dropped by " + f.name + "'s place for the evening.");
            });
        }
        if (!rain && timeOfDay() < 11000 && !level().isThundering()) {
            String[] spots = isle ? new String[]{"isle_plaza", "memorial", "gardens"} : new String[]{"pier", "boardwalk", "park", "plaza"};
            String k = bestOf(p, d, spots);
            opts.add(new Mind.Choice(k, "sunbathe", "the sun's out", -0.2 + (weekendNow() ? 0.5 : 0) + (p.trait == Trait.DREAMY ? 0.5 : 0)));
        }
        if (!rain && timeOfDay() < 9000) {
            String k = isle ? "gardens" : getRandom().nextBoolean() ? "park" : "boardwalk";
            double sc = -0.5 + (p.trait == Trait.ADVENTUROUS || p.trait == Trait.CHEERFUL ? 0.9 : p.trait == Trait.LAIDBACK || p.trait == Trait.GRUMPY ? -0.6 : 0.2) + (p.fun < 40 ? 0.3 : 0) - (resting ? 2 : 0);
            opts.add(new Mind.Choice(k, "jog", "a good run clears my head", sc));
        }
        if (!rain && (timeOfDay() < 3500 || timeOfDay() > 22500)) {
            String k = isle ? "gardens" : "park";
            double sc = -0.4 + (p.trait == Trait.DREAMY || p.trait == Trait.LAIDBACK ? 0.9 : p.trait == Trait.GRUMPY ? -0.6 : 0.1) + (p.fun < 40 ? 0.2 : 0) + (resting ? 0.6 : 0);
            opts.add(new Mind.Choice(k, "yoga", "morning yoga sets me up for the day", sc));
        }
        if (!rain && !isle) opts.add(new Mind.Choice(getRandom().nextFloat() < 0.7f ? "boardwalk" : "pier", "fishing", "the fish are biting", -0.1 + (p.trait == Trait.SHY ? 0.8 : 0) + (weekendNow() ? 0.3 : 0)));
        String boardKey = Gazette.nearestBoardKey(d, isle, blockPosition(), 200);
        if (boardKey != null) opts.add(new Mind.Choice(boardKey, "news", "catching up on the news", -0.6 + (p.trait == Trait.CURIOUS ? 0.6 : 0)));
        if (!rain) {
            String[] far = isle ? Place.CITY_HANGOUTS : Place.ISLE_HANGOUTS;
            String k = bestOf(p, d, far);
            StringBuilder why = new StringBuilder();
            double ps = Mind.placeScore(this, d, p, k, dd, why);
            Mind.Choice c = new Mind.Choice(k, "trip", why.length() > 0 ? why.toString() : (isle ? "a trip down to the city" : "a trip up to Neon Heights"), p.trait.skyChance * 3.0 + (p.fun < 35 ? 0.5 : 0) + ps - 0.4);
            opts.add(c);
            after.put(c, () -> d.news(dd, p.name + " went " + (isle ? "down to the city" : "up to Neon Heights") + " after work."));
        }
        if (!isle && Place.get(SkyTower.KEY) != null && !rain && timeOfDay() < 11500 && !Long.valueOf(dd).equals(mind.lastVisit.get("skydive"))) {
            double sc = -0.6 + (p.trait == Trait.ADVENTUROUS ? 1.6 : p.trait == Trait.CURIOUS || p.trait == Trait.CHEERFUL ? 0.6 : p.trait == Trait.SHY || p.trait == Trait.GRUMPY ? -0.8 : 0) + (p.fun < 40 ? 0.5 : 0) + (weekendNow() ? 0.4 : 0) - (resting ? 2 : 0);
            opts.add(new Mind.Choice(SkyTower.KEY, "skydive", "I want to go skydiving!", sc));
        }
        opts.add(new Mind.Choice(p.home, "home", resting ? "I need a quiet day" : "a quiet night in", -0.3 + (resting ? 2.5 : 0) + (p.fun > 85 ? 0.6 : 0) + (p.trait == Trait.GRUMPY || p.trait == Trait.SHY ? 0.4 : 0)));
        String[] local;
        if (rain) local = isle ? Place.ISLE_INDOOR : Place.CITY_INDOOR;
        else if (p.fun < 40) local = isle ? Place.ISLE_FUN : Place.CITY_FUN;
        else local = isle ? Place.ISLE_HANGOUTS : Place.CITY_HANGOUTS;
        for (String k : local) {
            StringBuilder why = new StringBuilder();
            double sc = 0.5 + Mind.placeScore(this, d, p, k, dd, why) - (resting ? 0.8 : 0);
            opts.add(new Mind.Choice(k, "hangout", why.length() > 0 ? why.toString() : "just hanging out", sc));
        }
        Mind.Choice c = Mind.decide(this, d, p, opts, getRandom());
        if (c == null) {
            leisureKey = p.home;
            leisureWhy = "home";
            return leisureKey;
        }
        Runnable r = after.get(c);
        if (r != null) r.run();
        leisureKey = c.key;
        leisureWhy = c.why;
        leisureReason = c.reason;
        if (c.why.equals("fishing")) waterSpot = null;
        return leisureKey;
    }

    private String bestOf(CityData.Profile p, CityData d, String[] keys) {
        String best = keys[0];
        double bs = -1e9;
        for (String k : keys) {
            StringBuilder sb = new StringBuilder();
            double sc = Mind.placeScore(this, d, p, k, day(), sb) + getRandom().nextDouble() * 0.8;
            if (sc > bs) { bs = sc; best = k; }
        }
        return best;
    }

    public String leisureReason() {
        return activityName().equals("leisure") ? leisureReason : "";
    }

    public BlockPos emergencyTarget() {
        CityData.Profile p = profile();
        if (p == null || level().isClientSide) return null;
        if (p.job == Job.FIREFIGHTER) return FireDept.target(this);
        if (p.job == Job.POLICE) return Police.target(this);
        if (p.job == Job.REPAIR) return Repair.target(this);
        BlockPos stove = Kitchen.target(this);
        if (stove != null) return stove;
        Vec3 haggle = Traders.target(this);
        if (haggle != null) return BlockPos.containing(haggle);
        return null;
    }

    public BlockPos errandTarget;
    public long errandUntil;
    public double errandSpeed = 1.0;
    public String errandKind = "";

    /** Sends the resident somewhere for a while (games, guiding, visits, tidying), overriding their routine. */
    public void errand(BlockPos to, int ticks, double speed, String kind) {
        errandTarget = to;
        errandUntil = level().getGameTime() + ticks;
        errandSpeed = speed;
        errandKind = kind;
        rethink();
    }

    public boolean onErrand() {
        if (errandTarget == null) return false;
        if (level().getGameTime() >= errandUntil) {
            errandTarget = null;
            errandKind = "";
            return false;
        }
        return true;
    }

    public void endErrand() {
        errandTarget = null;
        errandKind = "";
    }

    public BlockPos navTarget() {
        if (Elevator.controls(this)) return null;
        BlockPos emergency = emergencyTarget();
        if (emergency != null) return emergency;
        if (onErrand()) return errandTarget;
        if (Festival.live(profileId) && !onIsland() && activityName().equals("leisure")) return Festival.target(this);
        if (pcUsing != null && level() instanceof ServerLevel sl1 && Computers.isPc(sl1, pcUsing)) return pcUsing.relative(sl1.getBlockState(pcUsing).getValue(ComputerBlock.FACING));
        if (seekPlayer != null && level() instanceof ServerLevel sl0) {
            Entity e = sl0.getEntity(seekPlayer);
            if (e instanceof Player sp && level().getGameTime() < seekUntil && distanceTo(sp) < 24 && isFree() && !(seekFollow && activityName().equals("work"))) return sp.blockPosition();
            seekPlayer = null;
        }
        if (level() instanceof ServerLevel sl2) {
            BlockPos q = Extras.queueSpot(this, CityData.get(sl2));
            if (q != null) return q;
        }
        BlockPos clerkSpot = Reception.clerkTarget(this);
        if (clerkSpot != null) return clerkSpot;
        BlockPos desk = Reception.guestSpot(this);
        if (desk != null) return desk;
        Place dest = destination();
        if (dest == null) return null;
        int myF = Elevator.floorOfEntity(this);
        if (dest.island != onIsland()) {
            if (myF > 0) return Elevator.spotFor(this, myF);
            return ferryTarget();
        }
        if (insideKey != null && !insideKey.equals(dest.key)) return null;
        int dF = Elevator.floorOfPos(dest.pos);
        if (myF > 0 && dF != myF) return Elevator.spotFor(this, myF);
        if (dF > 0 && myF != dF) return Elevator.spotFor(this, 0);
        if (activityName().equals("work") && work.target() != null && (dest.entrance == null || dest.key.equals(insideKey)) && (work.target().getY() > 150) == onIsland()) return work.target();
        if (dest.entrance != null && !dest.key.equals(insideKey)) return dest.entrance;
        return arrivalSpot(dest);
    }

    private String spotKey;
    private BlockPos spot;

    private boolean spreads(Place dest) {
        String a = activityName();
        if (!(a.equals("leisure") || a.equals("lunch") || a.equals("morning"))) return false;
        if (Elevator.floorOfPos(dest.pos) >= 0) return false;
        CityData.Profile p = profile();
        if (p != null && dest.key.equals(p.home)) return false;
        return !(dest.key.equals(Bank.ATM) || dest.key.equals(Bank.KEY) || dest.key.equals(TechStore.KEY) || dest.key.equals(SkyTower.KEY) || dest.key.equals(Party.KEY) || dest.key.equals("skyport") || dest.key.equals("isle_pad"));
    }

    /** A personal, walkable spot near the place so residents spread out instead of stacking on one block. */
    private BlockPos arrivalSpot(Place dest) {
        if (!(level() instanceof ServerLevel sl) || !spreads(dest)) return dest.pos;
        if (dest.key.equals(spotKey) && spot != null) {
            if (tickCount % 100 != 0 || spot.equals(dest.pos) || !sl.isLoaded(spot) || Nav.walkable(sl, spot)) return spot;
            spotKey = null;
        }
        if (!sl.isLoaded(dest.pos)) return dest.pos;
        java.util.Random r = new java.util.Random(profileId.hashCode() * 31L + dest.key.hashCode());
        BlockPos found = dest.pos;
        for (int i = 0; i < 14; i++) {
            double a = r.nextDouble() * Math.PI * 2, rad = 1.5 + r.nextDouble() * 2.2;
            BlockPos c = dest.pos.offset((int) Math.round(Math.cos(a) * rad), 0, (int) Math.round(Math.sin(a) * rad));
            BlockPos s = Nav.standable(sl, c);
            if (s != null && Nav.clear(sl, dest.pos, s, this)) {
                found = s;
                break;
            }
        }
        spotKey = dest.key;
        spot = found;
        return found;
    }

    private long companionAt = -1000;
    private Resident companion;

    /** A partner or friend walking to the same place nearby, so the two can walk together. */
    public Resident companion() {
        long now = level().getGameTime();
        if (now - companionAt < 40 && (companion == null || !companion.isRemoved())) return companion;
        companionAt = now;
        Resident prev = companion;
        companion = null;
        CityData.Profile p = profile();
        Place dest = destination();
        if (p == null || dest == null || convo != null || emergencyTarget() != null) return null;
        CityData d = data();
        double best = Double.MAX_VALUE;
        for (Resident o : level().getEntitiesOfClass(Resident.class, getBoundingBox().inflate(10, 3, 10), x -> x != this && x.profile() != null && x.convo == null && !x.inShuttle())) {
            Place od = o.destination();
            if (od == null || !od.key.equals(dest.key) || o.getNavigation().isDone()) continue;
            boolean partner = o.profileId.equals(p.partner);
            CityData.Rel r = d.peekRel(p.id, o.profileId);
            if (!partner && (r == null || !r.friend())) continue;
            double sc = distanceToSqr(o) - (partner ? 100 : 0);
            if (sc < best) {
                best = sc;
                companion = o;
            }
        }
        if (companion != null && companion != prev && getRandom().nextFloat() < 0.15f && !crowded(4)) {
            CityData.Profile cp = companion.profile();
            say(pick("Wait up, " + cp.name + "!", "Walk with me, " + cp.name + "?", "Oh, you're going to " + dest.label + " too?", "Race you there, " + cp.name + "!"), 50);
        }
        return companion;
    }

    public long boostUntil;

    public String leisureWhy() {
        return leisureWhy;
    }

    /** Personal walking pace: some residents stride, some amble. */
    public double gait() {
        CityData.Profile p = profile();
        double g = 0.92 + Math.floorMod(profileId.hashCode(), 17) / 17.0 * 0.16;
        if (level().getGameTime() < boostUntil) g += 0.06;
        if (p == null) return g;
        if (p.trait == Trait.ADVENTUROUS || p.trait == Trait.CHEERFUL) g += 0.04;
        if (p.trait == Trait.LAIDBACK || p.trait == Trait.DREAMY) g -= 0.05;
        if (p.hunger < 20) g -= 0.08;
        String a = activityName();
        if (a.equals("evening") || a.equals("sleep")) g -= 0.08;
        if (a.equals("work") && !work.busy()) {
            Place wp = workPlace();
            if (wp != null && distanceToSqr(Vec3.atCenterOf(wp.pos)) > 40 * 40) g += 0.12;
        }
        return g;
    }

    public boolean jogging() {
        if (!"jog".equals(leisureWhy) || !activityName().equals("leisure") || fleeTicks > 0) return false;
        Place dest = destination();
        return dest != null && dest.island == onIsland() && distanceToSqr(Vec3.atCenterOf(dest.pos)) < 22 * 22;
    }

    public boolean idleHere() {
        if (!isFree() || !getNavigation().isDone() || pcUsing != null || phoneMode > 0 || sunTicks > 0 || dancing() || listenTicks > 0 || eatTicks > 0 || fleeTicks > 0) return false;
        if (isFollowing() || emergencyTarget() != null || Elevator.isQueued(this)) return false;
        String a = activityName();
        return a.equals("leisure") || a.equals("lunch") || a.equals("morning") || a.equals("evening");
    }

    private long lastFidget = -100000;

    private void fidget(CityData.Profile p, long now) {
        if (now - lastFidget < 900 || gestureTicks > 0 || speechTicks > 0 || !idleHere() || getRandom().nextFloat() > 0.18f) return;
        lastFidget = now;
        long tod = timeOfDay();
        boolean late = tod > 12500 || activityName().equals("evening");
        boolean early = tod > 22500 || tod < 1500;
        int w = getWeather();
        float r = getRandom().nextFloat();
        if (r < 0.15f) {
            String th = thought(p);
            if (th != null) {
                gesture(G_THINK, 50);
                say("(" + th + ")", 80);
                return;
            }
        }
        if ((w & W_SHIVER) != 0) {
            gesture(G_HUGSELF, 60);
        } else if ((late || early) && r < 0.45f) {
            gesture(G_YAWN, 50);
        } else if (p.ownsPhone && phoneCooldown <= 0 && r < 0.4f && !isSeated()) {
            usePhone(1, 80 + getRandom().nextInt(120), pick("scrolling SolFeed on their phone", "texting on their phone", "checking their phone"), null);
        } else if (r < 0.6f) {
            gesture(isSeated() ? G_THINK : G_STRETCH, 50);
        } else if (p.trait == Trait.CHEERFUL || p.trait == Trait.LAIDBACK) {
            gesture(G_DANCE, 40);
        } else {
            getLookControl().setLookAt(getX() + getRandom().nextGaussian() * 4, getEyeY() + 3 + getRandom().nextInt(6), getZ() + getRandom().nextGaussian() * 4);
            gesture(G_THINK, 40);
        }
    }

    /** A passing inner thought shown in the speech bubble. */
    private String thought(CityData.Profile p) {
        Mind m = p.mind;
        CityData d = data();
        if (!m.intent.isEmpty() && m.intentDay == day() && getRandom().nextBoolean()) return "I really want to " + m.intent + " today...";
        if (p.hunger < 35) return "I could really go for some " + Economy.label(Memory.favourite(p)) + "...";
        if (p.social < 30) return "It's been ages since I talked to anyone properly.";
        if (p.fun < 30) return "So... bored...";
        CityData.Profile q = p.partner.isEmpty() ? null : d.profiles.get(p.partner);
        if (q != null && getRandom().nextBoolean()) return "I wonder what " + q.name + " is up to right now.";
        if (!p.goal.isEmpty() && getRandom().nextBoolean()) return "Only a bit more saving and I can get " + p.goal + ".";
        return switch (getRandom().nextInt(5)) {
            case 0 -> "Did I leave the stove on?";
            case 1 -> "What should I have for dinner...";
            case 2 -> "I should call my family more.";
            case 3 -> "This city really is something.";
            default -> null;
        };
    }

    private long lastSorry = -100000;

    public void excuseMe(Player pl) {
        long now = level().getGameTime();
        if (now - lastSorry < 600 || isSpeaking() || getRandom().nextFloat() > 0.35f) return;
        lastSorry = now;
        getLookControl().setLookAt(pl, 30, 30);
        CityData.Profile p = profile();
        boolean met = p != null && data().playerRel(p.id, pl.getName().getString()).met;
        say(met ? pick("Oh, sorry " + pl.getName().getString() + "!", "After you!", "Whoops, in your way again?") : pick("Oh, excuse me!", "Sorry, after you.", "Pardon me!"), 40);
    }

    public String status() {
        CityData.Profile p = profile();
        if (p == null) return "";
        switch (skyPhase()) {
            case Skydive.LAUNCH -> { return "blasting up the Sky Launch"; }
            case Skydive.FREEFALL -> { return "skydiving over Solaris"; }
            case Skydive.CHUTE -> { return "floating down under a parachute"; }
            case Skydive.LANDED -> { return "just landed a skydive"; }
            default -> {}
        }
        if (getVehicle() instanceof Ferry fr) return fr.isPilot(this) ? "flying the Sky Ferry" + (fr.dockedAt() < 0 ? "" : " (boarding)") : (onIsland() == (fr.dockedAt() == Ferry.ISLE) && fr.dockedAt() >= 0 ? "waiting aboard the Sky Ferry" : "riding the Sky Ferry" + (fr.dockedAt() < 0 ? "" : ""));
        if (skyRider) return skyFrom == Skyliner.CITY ? "aboard the Neon Skyliner to Neon Heights" : "aboard the Neon Skyliner to the city";
        if (inShuttle()) return shuttleToIsland ? "riding the shuttle up to Neon Heights" : "riding the shuttle down to the city";
        if (Elevator.isRider(this)) return "riding the Ember Heights elevator";
        if (Elevator.isQueued(this)) return "waiting for the elevator (#" + Elevator.position(this) + " in line)";
        if (eatTicks > 0 && eatItem != null) return "eating " + Economy.label(eatItem);
        if (pcUsing != null) return pcStatus();
        if (phoneMode > 0 && !phoneWhat.isEmpty()) return phoneWhat;
        if (tvTicks > 0) return "watching SolTube on the TV at home";
        if (musicTicks > 0 && convo == null) return "listening to music on their SolBeats headphones";
        if (convo != null) {
            Resident o = convo.partnerOf(this);
            if (o.profile() != null) return "chatting with " + o.profile().name;
        }
        if (fleeTicks > 0) return "running away from a monster";
        if (isSleeping()) return "asleep in bed";
        Place dest = destination();
        if (isSeated()) return "sitting down" + (dest == null ? "" : " at " + dest.label);
        if (leisureWhy.equals("news") && dest != null && dest.key.startsWith("board") && blockPosition().closerThan(dest.pos, 6)) return "reading the Solaris Gazette";
        String label = dest == null ? "somewhere" : dest.label;
        boolean there = dest != null && (dest.key.equals(insideKey) || this.blockPosition().closerThan(dest.pos, 6));
        if (dest != null && dest.island != onIsland() && blockPosition().closerThan(Ferry.pad(onIsland() ? Ferry.ISLE : Ferry.CITY), 8)) return "waiting for the Sky Ferry to " + (onIsland() ? "the city" : "Neon Heights") + " (off to " + label + ")";
        String rs = leisureReason.isEmpty() ? "" : " - " + leisureReason;
        switch (activityName()) {
            case "morning": return there ? (dest.key.equals(p.home) ? "getting ready for the day at home" : "grabbing breakfast at " + label) : "heading to " + label + " for breakfast";
            case "work": return work.doing() != null ? "working at " + label + " (" + work.doing() + ")" : (there ? "working at " : "heading to work at ") + label;
            case "lunch": return (there ? "having lunch at " : "going for lunch at ") + label;
            case "leisure":
                String star = "";
                if (leisureWhy.equals("party")) {
                    CityData.Profile sp = data().profiles.get(partyStar);
                    star = sp == null ? "a" : sp.id.equals(p.id) ? "their own" : sp.name + "'s";
                }
                String why = switch (leisureWhy) {
                    case "visit" -> "visiting a friend at ";
                    case "trip" -> "on a trip at ";
                    case "home" -> "relaxing at ";
                    case "date" -> "on a date at ";
                    case "shopping" -> "shopping at ";
                    case "fishing" -> "fishing at ";
                    case "jog" -> "jogging around ";
                    case "yoga" -> "doing morning yoga at ";
                    case "lantern" -> "sending up lanterns at ";
                    case "wedding" -> "at a wedding at ";
                    case "quiz" -> "at quiz night at ";
                    case "karaoke" -> "singing karaoke at ";
                    case "movie" -> "watching a movie at ";
                    case "funrun" -> "at the fun run start at ";
                    case "sunbathe" -> sunTicks > 0 ? "sunbathing at " : "enjoying the sunshine at ";
                    case "party" -> "at " + star + " birthday party at ";
                    case "dance" -> "dancing at the Sky Organ party at ";
                    case "news" -> "reading the news at ";
                    case "lottery" -> "waiting for the lottery draw at ";
                    case "fireworks" -> "watching the fireworks at ";
                    case "stargaze" -> "stargazing at ";
                    case "tour" -> "on ";
                    case "worship" -> "honouring the Founder at ";
                    case "speech" -> Mayor.isMayor(data(), p.id) ? "giving a speech at " : "listening to the mayor's speech at ";
                    case "campaign" -> "campaigning for mayor at ";
                    case "bank" -> dest.key.equals(Bank.ATM) ? "using " : "doing some banking at ";
                    case "tech" -> "shopping for a new gadget at ";
                    default -> "hanging out at ";
                };
                Place want = leisureKey == null ? null : Place.get(leisureKey);
                if (dest != null && want != null && dest != want) {
                    if (dest.key.equals(p.home)) return "heading home for something to eat";
                    if (dest.key.equals(Bank.ATM)) return (there ? "using " : "popping over to ") + label + " for cash";
                    return (there ? "grabbing food at " : "on the way to get food at ") + label;
                }
                if (there) return why + label + rs;
                return switch (leisureWhy) {
                    case "trip" -> "heading off on a trip to ";
                    case "party" -> "on the way to " + star + " birthday party at ";
                    case "dance" -> "on the way to the Sky Organ party at ";
                    case "fishing" -> "going fishing at ";
                    case "jog" -> "jogging over to ";
                    case "yoga" -> "heading to morning yoga at ";
                    case "lantern" -> "on the way to Lantern Night at ";
                    case "wedding" -> "on the way to a wedding at ";
                    case "quiz" -> "on the way to quiz night at ";
                    case "karaoke" -> "on the way to karaoke at ";
                    case "movie" -> "on the way to movie night at ";
                    case "funrun" -> "heading to the fun run at ";
                    case "date" -> "on the way to a date at ";
                    case "bank" -> "popping over to ";
                    case "lottery" -> "heading to the lottery draw at ";
                    case "fireworks" -> "on the way to watch the fireworks at ";
                    case "stargaze" -> "on the way to stargaze at ";
                    case "tour" -> "on the way to join ";
                    case "worship" -> "on the way to the Festival of the Founder at ";
                    case "speech" -> "on the way to the mayor's speech at ";
                    case "campaign" -> "off to campaign at ";
                    default -> "on the way to ";
                } + label + rs;
            case "evening": return there ? "winding down at home" : "heading home";
            default: return there ? "asleep" : "heading home to bed";
        }
    }

    public static boolean sayLog;

    private static final java.util.Map<String, Long> ADDRESSED = new java.util.HashMap<>();
    private boolean priority;

    public static boolean mayAddress(net.minecraft.world.level.Level l, String player) {
        Long t = ADDRESSED.get(player);
        return t == null || l.getGameTime() - t > 200 || l.getGameTime() < t;
    }

    public static void addressed(net.minecraft.world.level.Level l, String player) {
        ADDRESSED.put(player, l.getGameTime());
    }

    public boolean isSpeaking() {
        return speechTicks > 0;
    }

    public boolean crowded(double r) {
        Resident partner = convo == null ? null : convo.partnerOf(this);
        for (Resident o : level().getEntitiesOfClass(Resident.class, getBoundingBox().inflate(r), x -> x != this && x.isSpeaking()))
            if (o != partner) return true;
        return false;
    }

    public void sayTo(String text, int ticks) {
        priority = true;
        try {
            say(text, ticks);
        } finally {
            priority = false;
        }
    }

    private boolean convoLine;
    private final java.util.Map<String, Long> recentSaid = new java.util.LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> e) {
            return size() > 24;
        }
    };

    /** Speaks a scripted conversation line (the only speech allowed while in a conversation). */
    public void sayLine(String text, int ticks) {
        convoLine = true;
        try {
            sayTo(text, ticks);
        } finally {
            convoLine = false;
        }
    }

    public void say(String text, int ticks) {
        if (convo != null && !convoLine && !priority) return;
        if (!priority && convo == null && crowded(5.0)) return;
        long gt = level().getGameTime();
        if (!convoLine) {
            Long last = recentSaid.get(text);
            if (last != null && gt - last < 2400 && gt >= last) return;
        }
        recentSaid.put(text, gt);
        if (sayLog) FireheartCity.LOG.info("[Say] " + profileId + ": " + text);
        if (Bank.debug && (Bank.inBuilding(blockPosition()) || blockPosition().closerThan(Bank.ATM_SPOT, 4))) FireheartCity.LOG.info("[Bank] " + profileId + ": " + text);
        this.entityData.set(SPEECH, text);
        this.speechTicks = Math.max(ticks, Math.min(200, 30 + text.length() * 2));
        if (gestureTicks <= 0 && eatTicks <= 0 && !isSleeping() && !text.startsWith("\u260e")) {
            int eg = emoteFor(text);
            if (eg != G_NONE) gesture(eg, 40);
        }
        CityData d = data();
        if (!d.chatter) return;
        CityData.Profile p = profile();
        String name = p == null ? "Resident" : p.name;
        for (ServerPlayer pl : ((ServerLevel) level()).players()) {
            if (pl.distanceToSqr(this) < 10 * 10 && pl.hasLineOfSight(this)) pl.sendSystemMessage(Component.literal("§b" + name + "§8: §f" + text));
        }
    }

    public void hush() {
        this.entityData.set(SPEECH, "");
        this.speechTicks = 0;
    }

    public void showItem(String id, int ticks) {
        this.setItemSlot(EquipmentSlot.MAINHAND, Economy.stack(id));
        this.heldTicks = ticks;
    }

    public void particles(ParticleOptions type, int n) {
        ((ServerLevel) level()).sendParticles(type, getX(), getY() + 2.1, getZ(), n, 0.3, 0.2, 0.3, 0.02);
    }

    public void startEating(String item) {
        CityData.Profile p = profile();
        if (p == null || item == null || p.count(item) <= 0) return;
        this.eatItem = item;
        this.eatTicks = 64;
        this.getNavigation().stop();
        showItem(item, 70);
        gesture(G_EAT, 64);
    }

    private void eatingTick() {
        ServerLevel sl = (ServerLevel) level();
        if (eatTicks % 4 == 0) {
            Vec3 look = this.getLookAngle();
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Economy.stack(eatItem)), getX() + look.x * 0.4, getEyeY() - 0.15, getZ() + look.z * 0.4, 4, 0.08, 0.08, 0.08, 0.05);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5f, 0.9f + getRandom().nextFloat() * 0.2f);
        }
        if (--eatTicks > 0) return;
        CityData.Profile p = profile();
        if (p == null) return;
        Economy.Food f = Economy.FOOD.get(eatItem);
        p.add(eatItem, -1);
        if (f != null) p.hunger = Math.min(100, p.hunger + f.restore());
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.4f, 1.2f);
        if (getRandom().nextFloat() < 0.6f) say(pick("Mmm, " + Economy.label(eatItem) + "!", "That hit the spot.", "Delicious!", "Ahh, much better.", p.hunger > 90 ? "I'm stuffed!" : "Yum."), 60);
        heldTicks = 8;
        data().setDirty();
    }

    public String pick(String... opts) {
        return Lines.pick(getRandom(), opts);
    }

    private static int errors;

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        try {
            cityTick();
        } catch (Throwable t) {
            if (errors++ < 20) FireheartCity.LOG.error("Resident tick failed for " + profileId, t);
            if (convo != null) {
                Conversation c = convo;
                convo = null;
                try { c.abort(); } catch (Throwable ignored) {}
            }
        }
    }

    private void cityTick() {
        CityData.Profile p = profile();
        if (p == null || (p.entity != null && !p.entity.equals(this.getUUID()))) {
            if (this.tickCount > 40) this.discard();
            return;
        }
        if (skyPhase() > 0) {
            skyTick(p);
            return;
        }
        if ("skydive".equals(leisureWhy) && activityName().equals("leisure") && tickCount % 10 == 3) {
            Place sp = Place.get(SkyTower.KEY);
            BlockPos pad = SkyTower.pad(data());
            if (sp != null && pad != null && blockPosition().closerThan(sp.pos, 4) && isFree()) startSkydive(pad);
        }
        bankerShift = p.job == Job.BANKER;
        if (tickCount % 200 == 5) {
            int sk = Cast.skinFor(p.id, p.skin);
            if (p.skin != sk) p.skin = sk;
            if (getSkin() != Math.floorMod(sk, SKINS)) this.entityData.set(SKIN, Math.floorMod(sk, SKINS));
        }
        musicianShift = p.job == Job.MUSICIAN;
        deskShift = p.job == Job.RECEPTIONIST;
        nightCop = Cast.nightShift(p.id);
        if (speechTicks > 0 && --speechTicks == 0) this.entityData.set(SPEECH, "");
        if (gestureTicks > 0 && --gestureTicks == 0) this.entityData.set(GESTURE, 0);
        if (heldTicks > 0 && --heldTicks == 0) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(p.job.tool));
        if (ringTicks > 0) ringTick();
        if (phoneCooldown > 0) phoneCooldown--;
        if (eatTicks > 0) eatingTick();
        if (p.job == Job.POLICE && Police.tick(this, p)) return;
        if (p.job == Job.FIREFIGHTER && FireDept.tick(this, p)) return;
        if (p.job == Job.REPAIR && Repair.tick(this, p)) return;
        if (Traders.tick(this, p)) return;
        if (Kitchen.tick(this, p)) return;
        if (fleeTicks > 0) {
            fleeTicks--;
            if (fleeTarget != null && fleeTicks % 10 == 0) getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 1.35);
        }
        if (shuttleTicks > 0) {
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            if (--shuttleTicks == 0) arriveShuttle();
            return;
        }
        if (skyRider) {
            skyTick();
            return;
        }
        long now = this.level().getGameTime();
        int phase = Math.floorMod(this.getId(), 40);
        if (now % 10 == 0) {
            if (fallWatch != null && fallWatch.y > 170 && getY() < 172 && !isPassenger() && getY() < fallWatch.y - 3 && Math.abs(getX() - fallWatch.x) + Math.abs(getZ() - fallWatch.z) < 8) FireheartCity.LOG.warn("[Fall] " + p.name + " fell from " + BlockPos.containing(fallWatch).toShortString() + " to " + blockPosition().toShortString() + " vehicle=" + getVehicle() + " status=" + status());
            fallWatch = position();
        }
        if (now % 20 == phase % 20) elevatorCheck(now);
        if (now % 40 == phase) life(p, now);
        if (now % 40 == phase) p.doing = status();
        if (getVehicle() instanceof Ferry fr) {
            phoneTick(p, now, phase);
            ferryRideTick(fr, p, now);
            return;
        }
        if (convo == null && now % 10 == phase % 10 && pilotTick(p)) return;
        if (convo != null) {
            convo.tick(this);
            return;
        }
        if (Elevator.controls(this)) return;
        if (listenTicks > 0) {
            listenTick(p);
            return;
        }
        if (pcTick(p, now, phase)) return;
        if (phoneTick(p, now, phase)) return;
        if (tvTicks > 0) {
            if (--tvTicks == 0) {
                p.fun = Math.min(100, p.fun + 15);
                p.log(routineDay()).once("tv");
            }
            if (now % 60 == phase && getRandom().nextFloat() < 0.15f) say(pick("Haha, this show!", "One more episode...", "Ooh, a new SolTube video!"), 40);
            getNavigation().stop();
            return;
        }
        if (musicTicks > 0) {
            musicTicks--;
            if (now % 30 == phase % 30) particles(net.minecraft.core.particles.ParticleTypes.NOTE, 1);
        }
        if (now % 40 == phase && "tech".equals(leisureWhy) && activityName().equals("leisure") && !p.wantDevice.isEmpty() && blockPosition().closerThan(TechStore.CENTER, 5)) TechStore.arrive(this, p);
        if (rest(now, phase)) {
            if (Furniture.onSeat(this) && now % 40 == phase) perceive(now);
            return;
        }
        if (now % 40 == phase) perceive(now);
        if (now % 20 == phase % 20) routeLogic();
        if (activityName().equals("work")) {
            Place wp = workPlace();
            double range = p.job == Job.POSTMAN ? 170 : 70;
            if (wp != null && wp.island == onIsland() && distanceToSqr(Vec3.atCenterOf(wp.pos)) < range * range && fleeTicks <= 0) work.tick(this, data(), p);
        } else if (work.target() != null) work.reset();
        if (now % 100 == phase) catchUp();
        if (now % 600 == phase && activityName().equals("work") && getRandom().nextFloat() < 0.35f && level().getNearestPlayer(this, 12) != null) say(shout(p), 70);
        if (now % 20 == phase % 20) weatherTick(p, now);
        if (now % 10 == phase % 10) danceTick(p, now);
        if (now % 40 == (phase + 20) % 40) fidget(p, now);
        if (activityName().equals("sleep") && now % 400 == phase && getRandom().nextFloat() < 0.3f && (isSleeping() || homePos() != null && blockPosition().closerThan(homePos(), 4))) {
            this.entityData.set(SPEECH, Pastimes.dream(this, p));
            this.speechTicks = 360;
        }
    }

    private String shout(CityData.Profile p) {
        String job = p.job.title.toLowerCase();
        return switch (Math.max(1, Math.min(3, p.level))) {
            case 3 -> pick(p.job.shout, "Head " + job + " here - nothing leaves this place unless it's perfect.", "Twenty years of practice in every bit of this work!", "Let me show the new folks how it's done.", "As head " + job + ", I say: quality first, always.");
            case 2 -> pick(p.job.shout, "Senior " + job + ", reporting for duty!", "I could do this with my eyes closed now.", "Getting faster every day at this!");
            default -> pick(p.job.shout, "Still learning the ropes, but I'm getting there!", "First week jitters... I've got this!", p.job.shout);
        };
    }

    private int danceStep;
    private long lastDanceLine = -100000;

    public boolean dancing() {
        if (!leisureWhy.equals("dance") || !activityName().equals("leisure") || convo != null || eatTicks > 0 || sunTicks > 0) return false;
        Place st = Place.get(Party.KEY);
        return st != null && onIsland() && distanceToSqr(Vec3.atCenterOf(st.pos)) < 16 * 16 && !Furniture.onSeat(this);
    }

    private void danceTick(CityData.Profile p, long now) {
        if (!dancing()) return;
        ServerLevel sl = (ServerLevel) level();
        boolean music = Party.playing(sl);
        danceStep++;
        if (danceStep % 12 == 0 || getNavigation().isDone() && danceStep % 4 == 0) {
            BlockPos spot = Party.danceSpot(new java.util.Random(getRandom().nextLong()));
            getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.7);
        }
        if (!music) return;
        if (onGround() && danceStep % 24 == (getId() % 24) && getRandom().nextFloat() < 0.5f) jumpFromGround();
        if (getNavigation().isDone()) {
            setYRot(getYRot() + net.minecraft.util.Mth.sin(danceStep * 0.12f + getId()) * 4f);
            yBodyRot = getYRot();
        }
        if (danceStep % 40 == 0) gesture(getRandom().nextFloat() < 0.25f ? G_CHEER : G_DANCE, 80);
        if (getRandom().nextFloat() < 0.3f) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, getX(), getY() + 2.2, getZ(), 1, 0.3, 0.1, 0.3, getRandom().nextDouble());
        if (now - lastDanceLine > 1600 && getRandom().nextFloat() < 0.08f) {
            lastDanceLine = now;
            say(pick("I love this song!", "Everybody dance!", "Look at my moves!", "Best party ever!", "Turn it up!", "Woo! Neon Heights!", "Whoever set up this organ is a genius."), 50);
        }
        p.fun = Math.min(100, p.fun + 1);
        p.social = Math.min(100, p.social + 1);
        DayLog lg = p.log(routineDay());
        if (lg.once("danced")) lg.note("I danced at the Sky Organ party");
    }

    private boolean outdoors() {
        return level().canSeeSky(blockPosition().above());
    }

    private void weatherTick(CityData.Profile p, long now) {
        int w = 0;
        boolean awake = !isSleeping() && !inShuttle() && !activityName().equals("sleep");
        boolean out = awake && outdoors();
        boolean sharing = companion != null && !companion.isRemoved() && companion.distanceToSqr(this) < 2.4 * 2.4 && getId() > companion.getId();
        if (out && level().isRaining() && !Furniture.onSeat(this) && !sharing) {
            w |= W_UMBRELLA;
            if (now - lastUmbrellaLine > 4000 && getRandom().nextFloat() < 0.08f && convo == null) {
                lastUmbrellaLine = now;
                say(pick("Good thing I brought my umbrella!", "Rain, rain, go away...", "Splish splash!", "My shoes are soaked."), 50);
            }
        }
        long tod = timeOfDay();
        boolean night = tod > 12800 && tod < 23200;
        boolean cold = level().getBiome(blockPosition()).value().coldEnoughToSnow(blockPosition()) || level().isThundering() || (onIsland() && night) || (level().isRaining() && night);
        if (out && cold && sunTicks <= 0) {
            w |= W_SHIVER;
            if (now - lastShiverLine > 3000 && getRandom().nextFloat() < 0.1f && convo == null) {
                lastShiverLine = now;
                say(pick("Brrr! It's freezing out here.", "Should've brought a jacket...", "My teeth are chattering!", onIsland() ? "It gets so cold up here at night!" : "Is it winter already?"), 50);
                ((ServerLevel) level()).sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.8, getZ(), 6, 0.3, 0.2, 0.3, 0.01);
            }
        }
        if (sharing && out && level().isRaining() && now - lastUmbrellaLine > 6000 && getRandom().nextFloat() < 0.2f && convo == null && companion.profile() != null) {
            lastUmbrellaLine = now;
            say(pick("Mind if I squeeze under your umbrella, " + companion.profile().name + "?", "Umbrella buddies!", "Scoot over, I'm getting soaked!"), 50);
        }
        if (sunTicks > 0) {
            w |= W_SUN;
            sunTicks -= 20;
            getNavigation().stop();
            if (sunTicks <= 0 || !activityName().equals("leisure") || level().isRaining() || convo != null || fleeTicks > 0) stopSunbathing();
        } else if (awake && out && leisureWhy.equals("sunbathe") && activityName().equals("leisure") && !level().isRaining() && isFree() && tod < 12000) {
            Place dest = destination();
            if (dest != null && blockPosition().closerThan(dest.pos, 5) && getRandom().nextFloat() < 0.15f) {
                sunTicks = 1200 + getRandom().nextInt(1200);
                getNavigation().stop();
                setPose(net.minecraft.world.entity.Pose.SLEEPING);
                say(pick("Ahh, perfect sunbathing weather.", "Just five minutes in the sun...", "Nothing beats this."), 60);
                p.log(routineDay()).note("I sunbathed at " + dest.label);
            }
        }
        if (w != getWeather()) this.entityData.set(WEATHER, w);
    }

    private void stopSunbathing() {
        sunTicks = 0;
        if (getPose() == net.minecraft.world.entity.Pose.SLEEPING && !isSleeping()) setPose(net.minecraft.world.entity.Pose.STANDING);
    }

    public boolean sunbathing() {
        return sunTicks > 0;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isSleeping() || sunTicks > 0;
    }

    private void checkPromotion(CityData.Profile p, CityData d) {
        int want = p.xp >= 40 ? 3 : p.xp >= 15 ? 2 : Math.max(p.level, 1);
        if (want <= p.level) return;
        p.level = want;
        p.rep += 3;
        refreshLooks(p);
        d.event(day(), "career", p.name + " was promoted to " + p.jobTitle(), blockPosition(), p.id);
        gesture(G_CHEER, 60);
        particles(ParticleTypes.HAPPY_VILLAGER, 12);
        say(pick("I got promoted! " + p.jobTitle() + ", baby!", "They made me " + p.jobTitle() + "! Can you believe it?", "Promotion day! Treats are on me."), 80);
    }

    private void fishTick(CityData.Profile p, CityData d) {
        ServerLevel sl = (ServerLevel) level();
        if (waterSpot == null || !sl.getFluidState(waterSpot).is(net.minecraft.tags.FluidTags.WATER) || !waterSpot.closerThan(blockPosition(), 7)) {
            waterSpot = null;
            BlockPos me = blockPosition();
            double best = Double.MAX_VALUE;
            for (BlockPos q : BlockPos.betweenClosed(me.offset(-5, -6, -5), me.offset(5, 0, 5))) {
                if (!sl.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER) || !sl.getBlockState(q.above()).isAir()) continue;
                double dd = q.distSqr(me);
                if (dd < best && dd > 2) { best = dd; waterSpot = q.immutable(); }
            }
            if (waterSpot == null) return;
        }
        getLookControl().setLookAt(waterSpot.getX() + 0.5, waterSpot.getY() + 1.0, waterSpot.getZ() + 0.5);
        if (heldTicks <= 20) showItem("minecraft:fishing_rod", 80);
        if (getRandom().nextFloat() < 0.35f) sl.sendParticles(ParticleTypes.FISHING, waterSpot.getX() + 0.5, waterSpot.getY() + 1.0, waterSpot.getZ() + 0.5, 2, 0.2, 0.0, 0.2, 0.0);
        if (getRandom().nextFloat() < 0.09f) {
            sl.sendParticles(ParticleTypes.SPLASH, waterSpot.getX() + 0.5, waterSpot.getY() + 1.0, waterSpot.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.1);
            sl.playSound(null, waterSpot, net.minecraft.sounds.SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6f, 1.0f);
            boolean big = getRandom().nextFloat() < 0.15f;
            p.add("minecraft:cooked_cod", 1);
            p.fish++;
            p.fun = Math.min(100, p.fun + (big ? 8 : 3));
            gesture(G_CHEER, 40);
            say(big ? pick("WHOA, look at the size of this one!", "A monster fish! Nobody's gonna believe me!") : pick("Got one!", "Dinner's sorted!", "Fish on!", "Another one for the bucket."), 60);
            if (p.fish % 10 == 0) d.event(day(), "fishing", p.name + " has caught " + p.fish + " fish off the docks", blockPosition(), p.id);
            else if (big) d.event(day(), "fishing", p.name + " landed a huge fish at " + (destination() == null ? "the docks" : destination().label), blockPosition(), p.id);
            heldTicks = 30;
            showItem("minecraft:cod", 30);
        }
    }

    private void partyTick(CityData.Profile p, CityData d) {
        ServerLevel sl = (ServerLevel) level();
        if (partyStar.isEmpty()) return;
        if (getRandom().nextFloat() < 0.12f) sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 2.2, getZ(), 1, 0.3, 0.2, 0.3, 1.0);
        if (p.id.equals(partyStar)) {
            if (heldTicks <= 20) showItem("minecraft:cake", 80);
            if (getRandom().nextFloat() < 0.12f) {
                gesture(G_CHEER, 40);
                say(pick("Thanks for coming, everyone!", "Best birthday ever!", "Who wants cake?", "I can't believe you all came!"), 60);
            }
            return;
        }
        Resident star = null;
        for (Resident o : sl.getEntitiesOfClass(Resident.class, getBoundingBox().inflate(10), r -> r.profileId.equals(partyStar))) star = o;
        CityData.Profile sp = d.profiles.get(partyStar);
        if (star == null || sp == null) return;
        getLookControl().setLookAt(star, 30f, 30f);
        if (lastGiftDay != day() && getRandom().nextFloat() < 0.2f) {
            lastGiftDay = day();
            String gift = pick("minecraft:poppy", "minecraft:dandelion", "minecraft:cookie", "minecraft:pumpkin_pie");
            showItem(gift, 50);
            gesture(G_GIVE, 40);
            say(pick("Happy birthday, " + sp.name + "!", "Happy birthday! This is for you, " + sp.name + ".", "Many happy returns, " + sp.name + "!"), 70);
            sp.add(gift, 1);
            sp.fun = Math.min(100, sp.fun + 6);
            sp.rep++;
            CityData.Rel r = d.rel(p.id, sp.id), r2 = d.rel(sp.id, p.id);
            r.aff = Math.min(100, r.aff + 4);
            r2.aff = Math.min(100, r2.aff + 4);
            d.setDirty();
        } else if (getRandom().nextFloat() < 0.08f) {
            gesture(G_CHEER, 30);
            say(pick("Woo! Party!", "Great party!", "Cake time?"), 40);
        }
    }

    private void readBoard(CityData d, CityData.Profile me, BlockPos board) {
        getLookControl().setLookAt(board.getX() + 0.5, board.getY() + 1.0, board.getZ() + 0.5);
        gesture(G_THINK, 60);
        CityData.Event fresh = null;
        int learned = 0;
        for (int i = d.events.size() - 1; i >= 0 && learned < 2; i--) {
            CityData.Event e = d.events.get(i);
            if (me.known.contains(e.id)) continue;
            me.learn(e.id);
            if (fresh == null) fresh = e;
            learned++;
        }
        d.setDirty();
        if (fresh != null) say("*reads the Gazette* Says here " + fresh.text + "!", 90);
        else say(pick("Nothing new in the Gazette today.", "Old news, old news...", "*flips through the Gazette*"), 60);
    }

    private boolean rest(long now, int phase) {
        ServerLevel sl = (ServerLevel) level();
        String act = activityName();
        if (restCooldown > 0) restCooldown--;
        if (isSleeping()) {
            BlockPos bp = getSleepingPos().orElse(null);
            boolean bedOk = bp != null && sl.getBlockState(bp).getBlock() instanceof BedBlock;
            if (!act.equals("sleep") || fleeTicks > 0 || !bedOk || Elevator.isRider(this)) {
                stopSleeping();
                bedTarget = null;
                restCooldown = 200;
                return false;
            }
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            return true;
        }
        if (Furniture.onSeat(this) && act.equals("work")) {
            stopRiding();
            sitAct = "";
            restCooldown = 200;
            return false;
        }
        if (Furniture.onSeat(this)) {
            if (sitAct.isEmpty()) {
                sitAct = act;
                sitTicks = 200;
            }
            Place dest = destination();
            boolean away = dest != null && getVehicle().blockPosition().distSqr(dest.pos) > 12 * 12;
            if (--sitTicks <= 0 || !act.equals(sitAct) || fleeTicks > 0 || away || act.equals("sleep") || Elevator.isRider(this) || Elevator.isQueued(this) || isFollowing() || onErrand()) {
                stopRiding();
                sitTicks = 0;
                sitAct = "";
                seatTarget = null;
                restCooldown = 600 + getRandom().nextInt(600);
                return false;
            }
            getNavigation().stop();
            return true;
        }
        sitAct = "";
        if (convo != null || fleeTicks > 0 || inShuttle() || eatTicks > 0 || onErrand()) {
            seatTarget = null;
            return false;
        }
        if (act.equals("sleep")) {
            seatTarget = null;
            BlockPos home = homePos();
            if (home == null || Math.abs(getY() - home.getY()) > 2.5 || horizontalDist(home) > 9) {
                bedTarget = null;
                return false;
            }
            if (restCooldown > 0) return false;
            if (bedTarget == null || !Furniture.isFreeBedHead(sl, bedTarget)) {
                if (now % 100 != phase) return false;
                bedTarget = Furniture.findBed(sl, home, 7);
                seekTicks = 0;
                if (bedTarget == null) {
                    restCooldown = 1200;
                    return false;
                }
            }
            seekTicks++;
            double d = distanceToSqr(Vec3.atBottomCenterOf(bedTarget));
            if (d < 2.5 * 2.5 || seekTicks > 200 && d < 4.5 * 4.5) {
                getNavigation().stop();
                setDeltaMovement(Vec3.ZERO);
                hush();
                startSleeping(bedTarget);
                return true;
            }
            if (seekTicks > 400) {
                bedTarget = null;
                restCooldown = 1200;
                return false;
            }
            if (seekTicks % 20 == 1) getNavigation().moveTo(bedTarget.getX() + 0.5, bedTarget.getY(), bedTarget.getZ() + 0.5, 0.7);
            return true;
        }
        boolean relaxing = act.equals("leisure") || act.equals("evening") || act.equals("lunch") || act.equals("morning");
        if (act.equals("leisure") && (leisureWhy.equals("campaign") || leisureWhy.equals("speech") && Mayor.isMayor(data(), profileId))) relaxing = false;
        if (!relaxing || restCooldown > 0 || Elevator.isQueued(this) || isFollowing()) {
            seatTarget = null;
            return false;
        }
        Place dest = destination();
        if (dest == null || !blockPosition().closerThan(dest.pos, 8)) {
            seatTarget = null;
            return false;
        }
        if (seatTarget == null) {
            if (now % 40 != phase || getRandom().nextFloat() > 0.2f || Elevator.isQueued(this) || Elevator.isRider(this)) return false;
            seatTarget = Furniture.findSeat(sl, dest.pos, 7, blockPosition(), 10);
            seekTicks = 0;
            if (seatTarget == null) {
                restCooldown = 1200;
                return false;
            }
        }
        if (++seekTicks > 240 || !Furniture.seatFree(sl, seatTarget)) {
            seatTarget = null;
            restCooldown = 600;
            return false;
        }
        double d = distanceToSqr(Vec3.atBottomCenterOf(seatTarget));
        if (d < 1.8 * 1.8) {
            getNavigation().stop();
            if (Furniture.sit(sl, seatTarget, this)) {
                sitTicks = 600 + getRandom().nextInt(1200);
                sitAct = act;
                return true;
            }
            seatTarget = null;
            restCooldown = 600;
            return false;
        }
        if (seekTicks % 20 == 1) getNavigation().moveTo(seatTarget.getX() + 0.5, seatTarget.getY(), seatTarget.getZ() + 0.5, 0.7);
        return true;
    }

    private double horizontalDist(BlockPos p) {
        double dx = getX() - (p.getX() + 0.5), dz = getZ() - (p.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    public boolean isSeated() {
        return Furniture.onSeat(this);
    }

    private void elevatorCheck(long now) {
        if (Elevator.isRider(this)) return;
        if (Reception.holding(this)) {
            if (Elevator.isQueued(this)) Elevator.cancel(this);
            return;
        }
        Place dest = destination();
        if (dest == null) return;
        int myF = Elevator.floorOfEntity(this);
        int want;
        if (dest.island != onIsland()) want = 0;
        else {
            int dF = Elevator.floorOfPos(dest.pos);
            want = Math.max(dF, 0);
        }
        if (myF >= 0 && want != myF && (blockPosition().closerThan(Elevator.callSpot(myF), 7) || Elevator.isQueued(this))) Elevator.request(this, myF, want, now);
        else if (Elevator.isQueued(this)) Elevator.cancel(this);
    }

    private void life(CityData.Profile p, long now) {
        CityData d = data();
        String act = activityName();
        boolean asleep = act.equals("sleep");
        hungerAcc += asleep ? 1 : 2;
        if (hungerAcc >= 15) { hungerAcc = 0; p.hunger = Math.max(0, p.hunger - 1); }
        if (convo == null && ++socialAcc >= (asleep ? 30 : 10)) { socialAcc = 0; p.social = Math.max(0, p.social - 1); }
        Place dest = destination();
        boolean there = dest != null && this.blockPosition().closerThan(dest.pos, 7);
        if (act.equals("leisure") && there) {
            if (++funAcc >= 2) { funAcc = 0; p.fun = Math.min(100, p.fun + (leisureWhy.equals("trip") || leisureWhy.equals("date") || dest.key.equals("arcade") ? 2 : 1)); }
        } else if (++funAcc >= 10) { funAcc = 0; p.fun = Math.max(0, p.fun - 1); }
        if (act.equals("leisure") && there && convo == null && !isSeated() && eatTicks <= 0) {
            if (leisureWhy.equals("fishing")) fishTick(p, d);
            else if (leisureWhy.equals("party")) partyTick(p, d);
        }
        if (act.equals("leisure") && there && convo == null && eatTicks <= 0 && listenTicks <= 0) civicTick(p, d, dest, now);
        if (jogging() && convo == null && eatTicks <= 0) Pastimes.jogTick(this, p, dest);
        if (act.equals("leisure") && there && "yoga".equals(leisureWhy) && convo == null && eatTicks <= 0 && !isSeated()) Hobbies.yogaTick(this, p, dest);
        long tod = timeOfDay();
        if ((tod > 13000 && tod < 23000) && !asleep && !isSleeping() && eatTicks <= 0 && heldTicks <= 40 && level().canSeeSky(blockPosition())) showItem("minecraft:lantern", 80);

        long rd = routineDay();
        ServerLevel sl = (ServerLevel) level();
        boolean atWork = act.equals("work") && (there || work.target() != null);
        if (atWork) {
            if (p.workDay != rd) { p.workDay = rd; p.workTicks = 0; }
            p.workTicks += 40;
            workAcc++;
            if (workAcc % 60 == 0) {
                p.xp++;
                checkPromotion(p, d);
            }
            if (p.hunger < 40 && eatTicks <= 0 && convo == null && Economy.sellsFood(p.job) && Shop.hasFood(sl, d, p.job)) {
                String item = Shop.choose(sl, d, p.job, true, null);
                if (item != null && Shop.take(sl, d, p.job, item)) {
                    p.add(item, 1);
                    say("Quick snack break!", 40);
                    startEating(item);
                }
            }
        }
        if ((act.equals("leisure") || act.equals("evening") || asleep) && p.workDay == rd && p.paidDay != rd && p.workTicks >= 1200) payday(p, d, rd);
        if (Calendar.weekday(rd) == 6 && p.rentDay != rd && !asleep && (act.equals("leisure") || act.equals("lunch"))) payRent(p, d, rd);
        if (dest != null && (dest.key.equals(Bank.KEY) || dest.key.equals(Bank.ATM)) && isFree() && !isSeated() && fleeTicks <= 0 && p.job != Job.BANKER) bankTick(p, d, dest, now);
        if (there && dest != null && !dest.key.equals(p.lastPlace)) {
            Mind mind = p.mind;
            boolean newSpot = !mind.lastVisit.containsKey(dest.key) && Mind.NEW_PLACES.contains(dest.key) && !dest.key.equals(p.job.workKey);
            p.lastPlace = dest.key;
            mind.visit(dest.key, day());
            if (newSpot) {
                mind.remember(p, day(), (int) timeOfDay(), "new_place", "I discovered " + dest.label + " for the first time", dest.key, 2, 5);
                if (isFree() && getRandom().nextFloat() < 0.7f) say(pick("So this is " + dest.label + "! Wow.", "I've never been here before - it's lovely!", "Ooh, " + dest.label + ". I should come here more often."), 70);
            }
        } else if (there && dest != null) p.mind.visit(dest.key, day());
        if (act.equals("leisure") && there && dest != null) {
            Memory.attend(d, p, dest.key, day());
            DayLog lg = p.log(rd);
            if (lg.once("l:" + dest.key + ":" + leisureWhy)) {
                String note = switch (leisureWhy) {
                    case "fishing" -> "I went fishing at " + dest.label;
                    case "trip" -> "I took a trip to " + dest.label;
                    case "shopping" -> "I went shopping at " + dest.label;
                    case "news" -> "I read the Gazette at " + dest.label;
                    case "hangout" -> dest.key.equals("observatory") && tod > 12500 ? "I watched the stars from the Neon Heights Observatory" : dest.key.equals("gardens") ? "I had a peaceful stroll through the Sky Gardens" : "I hung out at " + dest.label;
                    default -> null;
                };
                lg.note(note);
            }
            if (leisureWhy.equals("shopping") && shopSlot != slot() && isFree()) {
                shopSlot = slot();
                for (Job j : Job.values()) {
                    if (!j.workKey.equals(dest.key) || Shop.stock(sl, d, j) <= 0) continue;
                    CityData.Profile vendor = null;
                    Resident vr = findVendor(j);
                    if (vr != null) vendor = vr.profile();
                    String item = Shop.sell(sl, d, p, j, false, vendor);
                    if (item != null) {
                        showItem(item, 60);
                        gesture(G_GIVE, 30);
                        say(pick("Ooh, " + Economy.label(item) + " - I'll take it!", "Treating myself to " + Economy.label(item) + ".", "Paid " + Economy.price(item) + " coins for " + Economy.label(item) + ". Worth it!"), 70);
                        sl.playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.3f, 1.4f);
                        if (item.equals("minecraft:emerald")) p.fun = Math.min(100, p.fun + 15);
                        if (item.equals("minecraft:book")) p.fun = Math.min(100, p.fun + 8);
                    }
                    break;
                }
            }
        }

        if (p.hunger < 30 && Economy.foodCount(p) == 0 && "dance".equals(leisureWhy) && isFree() && eatTicks <= 0 && Place.get(Party.KEY) != null && blockPosition().closerThan(Place.get(Party.KEY).pos, 20) && getRandom().nextInt(200) == 0) {
            say(pick("Free party snacks? Don't mind if I do!", "Ooh, someone brought snacks!", "I was starving - bless whoever brought food."), 60);
            p.add("minecraft:cookie", 2);
            startEating("minecraft:cookie");
        }
        Place hp = p.homePlace();
        if (p.hunger < 12 && Economy.foodCount(p) == 0 && isFree() && eatTicks <= 0 && hp != null && blockPosition().closerThan(hp.pos, 8)) {
            if (p.coins >= 2 || Bank.savings(d, p.id) >= 2) {
                String payer = p.coins >= 2 ? p.id : Bank.sav(p.id);
                d.pay(payer, "biz:market", 2, "Groceries from the pantry", routineDay(), (int) timeOfDay(), false);
                say(pick("Right, I'm making myself something from the cupboard.", "Toast it is. I'm starving!", "Emergency sandwich time."), 60);
                p.log(routineDay()).note("I was so hungry I raided my own kitchen cupboard");
            } else {
                say(pick("Thank goodness for the city food bank parcel...", "Down to my last food bank bread. Payday can't come soon enough."), 60);
                if (p.log(routineDay()).once("foodbank")) p.mind.remember(p, day(), (int) timeOfDay(), "money", "I was broke and had to live on a food bank parcel", "", -2, 5);
            }
            p.add("minecraft:bread", 1);
            startEating("minecraft:bread");
        }
        if (isFree() && eatTicks <= 0 && !Elevator.isQueued(this)) {
            boolean mealTime = act.equals("morning") || act.equals("lunch") || act.equals("evening") || act.equals("leisure");
            String food = Economy.bestFood(p);
            if (food != null && (p.hunger < 25 || mealTime && p.hunger < 60)) {
                startEating(food);
            } else if (now - lastShop > 600 && (p.hunger < 60 || Economy.foodCount(p) < 2) && p.coins > 0) {
                for (Job j : Job.values()) {
                    if (!Economy.sellsFood(j) || !blockPosition().closerThan(j.work().pos, 5) || j == p.job) continue;
                    lastShop = now;
                    Resident vendor = findVendor(j);
                    if (vendor != null && vendor != this && vendor.isFree() && canSee(vendor, 8)) {
                        startConversation(vendor, now, d, day(), "shop");
                    } else if (Economy.sellsFood(j)) {
                        String item = Shop.sell((ServerLevel) level(), d, p, j, true, null);
                        if (item != null) {
                            showItem(item, 40);
                            say(pick("I'll just grab " + Economy.label(item) + ".", "Self-service it is!", "Paid for " + Economy.label(item) + " at the counter."), 60);
                            if (p.hunger < 60) startEating(item);
                        } else if (getRandom().nextFloat() < 0.25f) say("Sold out?! Aw, man.", 50);
                    }
                    break;
                }
            }
        }

        if (now % 400 < 40) {
            int score = d.statusScore(p);
            int tier = CityData.tierOf(score);
            if (tier != p.tier) {
                boolean up = tier > p.tier;
                p.tier = tier;
                refreshLooks(p);
                if (up && tier >= 2) {
                    d.event(day(), "status", p.name + " is now " + CityData.TIERS[tier].toLowerCase() + " around town", blockPosition(), p.id);
                    gesture(G_CHEER, 40);
                    particles(ParticleTypes.HAPPY_VILLAGER, 10);
                }
            }
        }

        if (level().isRaining() && rainDay != day() && isFree() && level().canSeeSky(blockPosition())) {
            rainDay = day();
            say(level().isThundering() ? pick("Whoa, that thunder!", "Storm's coming - better get inside!") : pick("Ugh, it's raining!", "Where's my umbrella...", "Rain again? Seriously?"), 60);
        }

        if (now % 400 < 40) refreshLooks(p);
        if (!asleep && fleeTicks <= 0 && !Elevator.isRider(this) && p.job != Job.POLICE && p.job != Job.FIREFIGHTER) {
            List<Mob> monsters = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(9), m -> m instanceof Enemy && m.isAlive() && canSee(m, 9));
            if (!monsters.isEmpty()) {
                Mob m = monsters.get(0);
                for (Mob x : monsters) if (x.distanceToSqr(this) < m.distanceToSqr(this)) m = x;
                if (convo != null) leaveConversation("Monster! Run!");
                flee(m.position(), 80, 1.35);
                String what = m.getType().getDescription().getString().toLowerCase();
                say(pick("A " + what + "! Run!", "Aaah! A " + what + "!", "Nope, nope, nope!"), 50);
                gesture(G_SURPRISED, 30);
                Events.sighting(d, this, what);
                DayLog scare = p.log(routineDay());
                if (scare.once("scare")) scare.note("A " + what + " gave me a real fright near " + Dialogue.here(this));
                Vec3 threat = m.position();
                for (Resident o : sl.getEntitiesOfClass(Resident.class, getBoundingBox().inflate(10, 3, 10), x -> x != this && x.fleeTicks <= 0 && x.isFree() && x.profile() != null && x.profile().job != Job.POLICE && x.profile().job != Job.FIREFIGHTER)) {
                    if (getRandom().nextFloat() > 0.7f || !o.hasLineOfSight(this)) continue;
                    o.flee(threat, 60, 1.25);
                    if (getRandom().nextFloat() < 0.4f) o.say(o.pick("Wait, what? Run!", "Where?! Where is it?", "Everybody inside!", "Not again!"), 40);
                    o.gesture(G_SURPRISED, 30);
                }
            }
        }
        d.setDirty();
    }

    public boolean speaking() {
        if (!leisureWhy.equals("speech") || !activityName().equals("leisure")) return false;
        long t = timeOfDay();
        return t >= Mayor.SPEECH_START - 300 && t <= Mayor.SPEECH_END && Mayor.isMayor(data(), profileId);
    }

    public boolean listeningTo(Resident r) {
        return listenTicks > 0 && listenTo == r;
    }

    public boolean listening() {
        return listenTicks > 0;
    }

    public boolean playingMusic() {
        return work.playing() && activityName().equals("work");
    }

    private void startListening(Resident musician) {
        listenTo = musician;
        listenTicks = Math.min(400, Math.max(60, musician.work.musicLeft()));
        listenDay = day();
        getNavigation().stop();
        if (getRandom().nextFloat() < 0.4f) say(pick("Ooh, is that " + musician.work.doing().replaceFirst("^play ", "") + "?", "Music! Let me listen for a bit.", "I love this song!"), 50);
    }

    private void listenTick(CityData.Profile p) {
        listenTicks--;
        Resident m = listenTo;
        if (m == null || m.isRemoved() || fleeTicks > 0) { listenTicks = 0; listenTo = null; return; }
        getNavigation().stop();
        getLookControl().setLookAt(m, 30, 30);
        if (listenTicks % 60 == 30 && getRandom().nextFloat() < 0.35f) {
            gesture(getRandom().nextBoolean() ? G_CHEER : G_WAVE, 30);
            ((ServerLevel) level()).sendParticles(ParticleTypes.NOTE, getX(), getY() + 2.2, getZ(), 1, 0.2, 0.1, 0.2, getRandom().nextDouble());
        }
        if (listenTicks > 0 && m.playingMusic()) return;
        listenTicks = 0;
        listenTo = null;
        p.fun = Math.min(100, p.fun + 12);
        CityData d = data();
        CityData.Profile mp = m.profile();
        if (mp == null) return;
        ServerLevel sl = (ServerLevel) level();
        int tip = p.coins >= 6 && getRandom().nextFloat() < 0.65f ? 1 + getRandom().nextInt(p.trait == Trait.FRIENDLY || p.trait == Trait.CHEERFUL ? 3 : 2) : 0;
        if (tip > 0 && d.pay(p.id, mp.id, tip, "Tip for " + mp.name + "'s music", routineDay(), (int) timeOfDay())) {
            gesture(G_GIVE, 30);
            showItem("minecraft:gold_nugget", 30);
            say(pick("Bravo! Here's " + tip + " for you, " + mp.name + "!", "Beautiful! Keep playing!", "Encore! Encore!"), 60);
            sl.playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.3f, 1.6f);
            mp.log(m.routineDay()).note(p.name + " tipped me " + tip + " coins");
            d.rel(p.id, mp.id).aff += 2;
            d.rel(mp.id, p.id).aff += 2;
        } else say(pick("Lovely tune.", "That was great!", "Nice one, " + mp.name + "."), 50);
        p.log(routineDay()).note("I listened to " + mp.name + " play at the plaza");
        d.setDirty();
    }

    private void civicTick(CityData.Profile p, CityData d, Place dest, long now) {
        ServerLevel sl = (ServerLevel) level();
        long tod = timeOfDay();
        switch (leisureWhy) {
            case "campaign" -> {
                if (heldTicks <= 20) showItem("minecraft:paper", 80);
                if (now - lastCivicLine > 500 && getRandom().nextFloat() < 0.5f) {
                    lastCivicLine = now;
                    gesture(G_CHEER, 40);
                    say(Mayor.campaignLine(d, p, getRandom()), 90);
                    for (Resident o : sl.getEntitiesOfClass(Resident.class, getBoundingBox().inflate(8), x -> x != this && x.isFree() && x.profile() != null)) {
                        if (getRandom().nextFloat() > 0.3f) continue;
                        CityData.Profile op = o.profile();
                        boolean fan = p.id.equals(Mayor.preference(d, op, getRandom()));
                        o.getLookControl().setLookAt(this, 30, 30);
                        o.say(fan ? o.pick("You've got my vote, " + p.name + "!", "Go " + p.name + "!", Mayor.policyName(Mayor.platform(p)) + "! Yes please!") : o.pick("Hmm, I'm still deciding.", "Not convinced yet, " + p.name + ".", "What about the other candidates?"), 60);
                        o.gesture(fan ? G_CHEER : G_THINK, 40);
                        break;
                    }
                }
            }
            case "speech" -> {
                boolean mayor = Mayor.isMayor(d, p.id);
                if (tod < Mayor.SPEECH_START || tod > Mayor.SPEECH_END) return;
                if (mayor) {
                    if (now - lastCivicLine > 140) {
                        lastCivicLine = now;
                        gesture(civicStep % 3 == 0 ? G_WAVE : G_CHEER, 50);
                        sayTo(Mayor.speechLine(d, p, civicStep++, getRandom()), 130);
                        if (civicStep % 7 == 0) {
                            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.4f, 1.2f);
                            p.log(routineDay()).note("I gave my mayor's speech at the plaza");
                        }
                    }
                } else {
                    CityData.Profile m = Mayor.mayor(d);
                    if (m != null && m.entity != null && sl.getEntity(m.entity) instanceof Resident mr && mr.distanceToSqr(this) < 20 * 20) {
                        getLookControl().setLookAt(mr, 30, 30);
                        if (getRandom().nextFloat() < 0.08f) {
                            CityData.Rel rel = d.rel(p.id, m.id);
                            gesture(rel.rival ? G_ANGRY : G_CHEER, 30);
                            if (getRandom().nextFloat() < 0.4f) say(rel.rival ? pick("Boo!", "Says you!") : pick("Hear, hear!", "Go Mayor " + m.name + "!", "Woo!"), 40);
                        }
                    }
                }
            }
            case "fireworks" -> {
                if (Fireworks.showing(sl, d)) {
                    getLookControl().setLookAt(Fireworks.PIER_SKY.getX() + 0.5, Fireworks.PIER_SKY.getY() + 25, Fireworks.PIER_SKY.getZ() + 0.5);
                    if (getRandom().nextFloat() < 0.12f) {
                        gesture(G_CHEER, 30);
                        say(pick("Ooooh!", "Aaaah!", "Wow, look at that one!", "So pretty!", "Best show yet!"), 40);
                    }
                    DayLog lg = p.log(routineDay());
                    if (lg.once("fireworks")) lg.note("I watched the fireworks over the pier");
                    p.fun = Math.min(100, p.fun + 1);
                } else if (now - lastCivicLine > 900 && getRandom().nextFloat() < 0.2f) {
                    lastCivicLine = now;
                    say(pick("Got a good spot for the fireworks!", "When do the fireworks start?", "I hope it doesn't rain on the show."), 50);
                }
            }
            case "lottery" -> {
                if (heldTicks <= 20 && Lottery.tickets(d, p.id) > 0) showItem("minecraft:paper", 80);
                if (now - lastCivicLine > 800 && getRandom().nextFloat() < 0.2f && d.civic.drawDay != day()) {
                    lastCivicLine = now;
                    say(pick("Come on, lucky ticket!", "The jackpot's " + Lottery.pot(d) + " coins this week!", "If I win, I'm buying " + (p.goal.isEmpty() ? "something amazing" : p.goal) + "!", "Fingers crossed!"), 60);
                }
            }
            default -> {}
        }
    }

    public boolean waitingForTeller() {
        Place dest = destination();
        return dest != null && dest.key.equals(Bank.KEY) && convo == null && Bank.inBuilding(blockPosition());
    }

    public void doneErrand() {
        rethink();
        leisureDay = -1;
        leisureKey = null;
        leisureWhy = "";
        if (KEY_BANK.equals(lunchKey) || Bank.ATM.equals(lunchKey)) lunchKey = null;
        bankWait = 0;
        atmStep = 0;
        CityData.Profile p = profile();
        if (p != null) p.cashDay = routineDay();
    }

    private void bankTick(CityData.Profile p, CityData d, Place dest, long now) {
        ServerLevel sl = (ServerLevel) level();
        if (dest.key.equals(Bank.ATM)) {
            Vec3 spot = Vec3.atBottomCenterOf(Bank.ATM_SPOT);
            if (distanceToSqr(spot) > 1.6 * 1.6) {
                if (distanceToSqr(spot) < 2.5 * 2.5) getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 0.6);
                else if (distanceToSqr(spot) < 5 * 5) getNavigation().moveTo(spot.x, spot.y, spot.z, 0.7);
                if (++bankWait > 20) doneErrand();
                return;
            }
            getNavigation().stop();
            getLookControl().setLookAt(Bank.ATM_SCREEN.getX() + 0.5, Bank.ATM_SCREEN.getY() + 0.5, Bank.ATM_SCREEN.getZ() + 0.5);
            if (atmStep++ == 0) {
                showItem("minecraft:paper", 60);
                gesture(G_THINK, 40);
                swing(InteractionHand.MAIN_HAND);
                sl.playSound(null, Bank.ATM_SCREEN, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5f, 1.8f);
                return;
            }
            swing(InteractionHand.MAIN_HAND);
            sl.playSound(null, Bank.ATM_SCREEN, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5f, 1.2f);
            String res = Bank.atm(this, d, p);
            showItem("minecraft:gold_nugget", 40);
            if (level().getNearestPlayer(this, 16) != null || getRandom().nextFloat() < 0.3f) say(res, 90);
            doneErrand();
            return;
        }
        if (!Bank.open(sl, d)) {
            if (!Bank.needs(d, p, routineDay(), false).isEmpty()) {
                leisureKey = Bank.ATM;
                rethink();
                say(pick("Oh, the teller's closed. Cash machine it is.", "Hugo's gone home already? Never mind."), 50);
            } else doneErrand();
            return;
        }
        if (!Bank.inBuilding(blockPosition())) return;
        Vec3 win = Vec3.atBottomCenterOf(Bank.WINDOW);
        Resident hugo = Bank.bankerEntity(sl, d);
        boolean free = hugo != null && Bank.atCounter(hugo) && hugo.convo == null && hugo.isFree();
        if (distanceToSqr(win) > 0.9 * 0.9) {
            Resident first = null;
            for (Resident o : sl.getEntitiesOfClass(Resident.class, getBoundingBox().inflate(4), x -> x != this && x.convo == null && x.waitingForTeller() && x.distanceToSqr(win) < 0.9 * 0.9)) first = o;
            if (first == null) {
                if (distanceToSqr(win) < 2.5 * 2.5) getMoveControl().setWantedPosition(win.x, win.y, win.z, 0.6);
                else getNavigation().moveTo(win.x, win.y, win.z, 0.7);
            }
            if (++bankWait > 30) {
                leisureKey = Bank.ATM;
                rethink();
                say(pick("This queue is taking forever. I'll use the machine outside.", "Too busy in here - cash machine it is."), 50);
            } else if (bankWait % 8 == 4 && getRandom().nextFloat() < 0.4f) say(pick("Busy in here today!", "Still waiting my turn...", "Is the bank always this popular?"), 40);
            return;
        }
        getNavigation().stop();
        if (free && distanceToSqr(hugo) < 3.2 * 3.2) {
            startConversation(hugo, now, d, day(), "bank");
            return;
        }
        if (++bankWait > 30) {
            leisureKey = Bank.ATM;
            rethink();
            say(pick("I'll come back another time.", "Never mind, I'll use the machine outside."), 50);
        } else if (hugo != null && bankWait == 3 && getRandom().nextFloat() < 0.6f) {
            getLookControl().setLookAt(hugo, 30, 30);
            say(pick("Excuse me, " + (d.rel(p.id, hugo.profileId()).met ? "Hugo" : "sir") + "?", "*waits at the counter*", "Hello? Anyone at the counter?"), 50);
        }
    }

    public Resident findVendor(Job j) {
        CityData d = data();
        for (CityData.Profile q : d.profiles.values()) {
            if (q.job != j || q.entity == null) continue;
            Entity e = ((ServerLevel) level()).getEntity(q.entity);
            if (e instanceof Resident r && r.activityName().equals("work") && r.blockPosition().closerThan(j.work().pos, 8)) return r;
        }
        return null;
    }

    private void payday(CityData.Profile p, CityData d, long rd) {
        int full = Economy.wage(p);
        int wage = Math.max(1, (int) Math.round(full * Math.min(1.0, p.workTicks / 7000.0)));
        String biz = Economy.business(p.job);
        int tod = (int) timeOfDay();
        int fromBiz = Math.min(wage, Math.max(0, d.balance(biz)));
        if (fromBiz > 0) d.pay(biz, p.id, fromBiz, "Wages - " + p.jobTitle(), rd, tod);
        int fromBank = Math.min(wage - fromBiz, Math.max(0, d.balance(Bank.sav(biz))));
        if (fromBank > 0) d.pay(Bank.sav(biz), p.id, fromBank, "Wages - " + p.jobTitle(), rd, tod);
        if (wage - fromBiz - fromBank > 0) d.pay(CityData.CITY, p.id, wage - fromBiz - fromBank, "Wages - " + p.jobTitle(), rd, tod);
        if ("wages".equals(d.civic.policy)) {
            int bonus = Math.max(1, wage / 5);
            d.pay(CityData.CITY, p.id, bonus, "Fair Wages bonus (Mayor's policy)", rd, tod);
            wage += bonus;
        }
        p.paidDay = rd;
        if (Calendar.weekday(rd) == 4) p.log(rd).note("I got paid " + wage + " coins, and it's almost the weekend");
        if (level().getNearestPlayer(this, 12) != null && convo == null) {
            say(pick("Payday! " + wage + " coins, thank you very much.", "Just got paid - " + wage + " coins!", "Clocking out. " + wage + " coins earned today."), 70);
            showItem("minecraft:gold_nugget", 40);
        }
        d.setDirty();
    }

    private void payRent(CityData.Profile p, CityData d, long rd) {
        p.rentDay = rd;
        int rent = Economy.rent(p);
        Place h = p.homePlace();
        String where = h == null ? "home" : h.label;
        if (p.coins < rent && Bank.savings(d, p.id) >= rent - p.coins) d.pay(Bank.sav(p.id), p.id, rent - p.coins, "Standing order - rent", rd, (int) timeOfDay(), false);
        if (d.pay(p.id, Economy.rentAccount(p), rent, "Weekly rent - " + where, rd, (int) timeOfDay())) {
            p.log(rd).note("I paid " + rent + " coins rent");
        } else {
            p.log(rd).note("I couldn't afford the rent this week");
            d.event(day(), "money", p.name + " couldn't pay the rent this week", blockPosition(), p.id);
            if (Bank.loan(d, p.id) == null) {
                p.loanWish = rent * 3;
                p.loanWhy = "rent";
            }
        }
        d.setDirty();
    }

    private void routeLogic() {
        Place dest = destination();
        if (dest == null) return;
        if (insideKey != null && !insideKey.equals(dest.key)) {
            Place in = Place.get(insideKey);
            insideKey = null;
            if (in != null && in.entrance != null) this.teleportTo(in.entrance.getX() + 0.5, in.entrance.getY(), in.entrance.getZ() + 0.5);
            return;
        }
        if (dest.island != onIsland()) {
            if (Elevator.floorOfEntity(this) > 0) return;
            ferryApproach();
            return;
        }
        if (dest.entrance != null && !dest.key.equals(insideKey) && this.blockPosition().closerThan(dest.entrance, 2.5)) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 0.6f, 1.0f);
            this.getNavigation().stop();
            this.teleportTo(dest.pos.getX() + 0.5, dest.pos.getY(), dest.pos.getZ() + 0.5);
            insideKey = dest.key;
        }
    }

    private int ferryWait;
    private BlockPos pcUsing;
    private int pcTicks, pcPlay, pcCooldown;
    private String pcGame = "snake", pcApp = "game";
    private boolean pcPublic;
    private Vec3 fallWatch;

    public boolean wantsFerry() {
        if (isPassenger()) return false;
        Place dest = destination();
        return dest != null && dest.island != onIsland();
    }

    public String currentLeisureKey() {
        return leisureDay == slot() ? leisureKey : null;
    }

    private BlockPos ferryTarget() {
        int side = onIsland() ? Ferry.ISLE : Ferry.CITY;
        Ferry f = level() instanceof ServerLevel sl ? Ferry.find(sl) : null;
        if (f != null && f.dockedAt() == side) return Ferry.pad(side);
        return ferryWaitSpot(side);
    }

    private BlockPos ferryWaitSpot(int side) {
        BlockPos pad = Ferry.pad(side);
        int h = Math.floorMod(profileId.hashCode(), 14);
        return pad.offset(-(h % 2) + (side == Ferry.ISLE ? 1 : 0), 0, h / 2 - 3);
    }

    public boolean forcePc(String game) {
        CityData.Profile p = profile();
        if (p == null || !p.ownsPC || p.pcPos == null) return false;
        pcUsing = p.pcPos;
        pcPublic = false;
        pcTicks = 0;
        pcPlay = 300;
        if (game.startsWith("app:")) {
            pcApp = game.substring(4);
        } else {
            pcApp = "game";
            pcGame = game;
        }
        return true;
    }

    public boolean usingPc() {
        return pcUsing != null;
    }

    private int tvTicks, musicTicks;

    public void watchTv(int ticks) {
        tvTicks = ticks;
    }

    public void setMusic(int ticks) {
        musicTicks = ticks;
    }

    public boolean atHome() {
        CityData.Profile p = profile();
        return p != null && p.homePlace() != null && blockPosition().closerThan(p.homePlace().pos, 8);
    }

    private void stopPc(ServerLevel sl) {
        if (pcUsing != null && !Computers.OPEN.containsValue(pcUsing)) Computers.setScreen(sl, pcUsing, 0);
        pcUsing = null;
        pcCooldown = 2400 + getRandom().nextInt(2400);
    }

    private boolean pcTick(CityData.Profile p, long now, int phase) {
        ServerLevel sl = (ServerLevel) level();
        if (pcCooldown > 0) pcCooldown--;
        String act = activityName();
        boolean homeTime = act.equals("leisure") && leisureWhy.equals("home") || act.equals("evening");
        if (pcUsing != null) {
            if (!Computers.isPc(sl, pcUsing) || convo != null || fleeTicks > 0 || act.equals("sleep") || !(homeTime || act.equals("leisure"))) {
                stopPc(sl);
                return false;
            }
            Direction f = sl.getBlockState(pcUsing).getValue(ComputerBlock.FACING);
            BlockPos front = pcUsing.relative(f);
            Vec3 stand = Vec3.atBottomCenterOf(front);
            pcTicks++;
            if (distanceToSqr(stand) > 1.2 * 1.2) {
                if (pcTicks % 20 == 1) getNavigation().moveTo(stand.x, stand.y, stand.z, 0.7);
                if (pcTicks > 500) stopPc(sl);
                return true;
            }
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            getLookControl().setLookAt(pcUsing.getX() + 0.5, pcUsing.getY() + 0.55, pcUsing.getZ() + 0.5, 30, 30);
            if (!Computers.OPEN.containsValue(pcUsing)) Computers.setScreen(sl, pcUsing, pcApp.equals("game") ? 2 : pcApp.equals("tube") ? 3 : 1);
            if (pcTicks % 120 == 0 && getRandom().nextFloat() < 0.4f && !crowded(4)) {
                String line = Computers.appChatter(this, p, pcApp);
                if (line != null) say(line, 50);
            }
            if (!pcApp.equals("game") && --pcPlay <= 0) {
                Computers.appDone(this, p, pcApp, pcPublic);
                stopPc(sl);
                return true;
            }
            if (pcApp.equals("game") && --pcPlay <= 0) {
                int score = Computers.residentScore(getRandom(), p, pcGame);
                CityData d = data();
                int before = d.scores.getOrDefault(pcGame, java.util.Map.of()).getOrDefault(p.name, 0);
                Computers.record(d, pcGame, p.name, score);
                String gn = Computers.gameName(pcGame);
                if (score > before && before > 0) {
                    gesture(G_CHEER, 50);
                    say("YES! New high score on " + gn + ": " + score + "!", 70);
                } else say(pick("Scored " + score + " on " + gn + ". Not bad!", "Only " + score + " on " + gn + "... rematch tomorrow.", gn + ": " + score + ". I'll take it!"), 60);
                p.fun = Math.min(100, p.fun + 12);
                DayLog lg = p.log(routineDay());
                if (lg.once("pc:" + pcGame)) lg.note("I played " + gn + " on " + (pcPublic ? "the public computer" : "my PC") + " and scored " + score);
                if (pcPublic) p.mind.remember(p, day(), (int) timeOfDay(), "pc", "I tried the computer at " + Dialogue.here(this), p.lastPlace, 1, 3);
                stopPc(sl);
            }
            return true;
        }
        if (pcCooldown > 0 || now % 40 != phase || !isFree() || eatTicks > 0 || isSeated()) return false;
        CityData d = data();
        Place dest = destination();
        if (homeTime && p.ownsPC && p.pcPos != null && Computers.isPc(sl, p.pcPos) && blockPosition().closerThan(p.pcPos, 14) && getRandom().nextFloat() < 0.3f) {
            pcUsing = p.pcPos;
            pcPublic = false;
        } else if (act.equals("leisure") && dest != null && blockPosition().closerThan(dest.pos, 8) && getRandom().nextFloat() < 0.12f) {
            BlockPos pub = Computers.publicPcNear(sl, d, blockPosition(), 10);
            if (pub == null) return false;
            pcUsing = pub;
            pcPublic = true;
        } else return false;
        pcTicks = 0;
        pcApp = Computers.chooseApp(this, p, pcPublic);
        pcPlay = pcApp.equals("game") ? 600 + getRandom().nextInt(900) : 300 + getRandom().nextInt(400);
        pcGame = Computers.GAMES[getRandom().nextInt(Computers.GAMES.length)];
        return true;
    }

    private String pcStatus() {
        String where = pcPublic ? "a public computer" : "their PC";
        return switch (pcApp) {
            case "feed" -> "scrolling SolFeed on " + where;
            case "news" -> "reading the news on " + where;
            case "tube" -> "watching SolTube on " + where;
            case "bank" -> "checking their bank balance on " + where;
            case "msg" -> "messaging friends on " + where;
            case "shop" -> "browsing the SolTech store online";
            case "notes" -> "writing in their diary on " + where;
            default -> "playing " + Computers.gameName(pcGame) + " on " + where;
        };
    }

    /* ---------------------------------------------------------------- FirePhone */

    private int phoneMode, phoneTicks, phoneCooldown = 200, ringTicks;
    private String phoneWhat = "";
    private Runnable phoneDone;

    public int phoneMode() { return phoneMode; }

    public boolean onPhone() { return phoneMode > 0; }

    public boolean inCall() { return phoneMode == 2; }

    public int phoneCooldown() { return phoneCooldown; }

    public String phoneWhat() { return phoneWhat; }

    public void setPhoneWhat(String w) { phoneWhat = w; }

    public void replan() {
        leisureDay = -1;
        rethink();
    }

    public int skyPhase() {
        return this.entityData.get(SKY);
    }

    private int skyT;
    private BlockPos skyTarget;
    private int skyApex;

    private void setSky(int phase) {
        this.entityData.set(SKY, phase);
        skyT = 0;
    }

    public void startSkydive(BlockPos pad) {
        CityData.Profile p = profile();
        if (p == null || skyPhase() > 0) return;
        if (convo != null) leaveConversation("Sorry, gotta go - I'm skydiving!");
        if (isPassenger()) stopRiding();
        if (isSleeping()) stopSleeping();
        putPhoneAway();
        Elevator.cancel(this);
        if (pad != null) teleportTo(pad.getX() + 0.5, pad.getY() + 0.2, pad.getZ() + 0.5);
        int top = pad == null ? (int) getY() : data().skyTop;
        skyApex = Math.max(230, top + 90) + getRandom().nextInt(30);
        BlockPos base = pad == null ? blockPosition() : pad;
        double a = getRandom().nextDouble() * Math.PI * 2, r = 7 + getRandom().nextDouble() * 8;
        int tx = base.getX() + (int) (Math.cos(a) * r), tz = base.getZ() + SkyTower.PLAZA + 4 + (int) (Math.sin(a) * r * 0.5);
        skyTarget = new BlockPos(tx, level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz), tz);
        getNavigation().stop();
        setNoGravity(true);
        setSky(Skydive.LAUNCH);
        say(pick("Here goes nothing!", "3... 2... 1... WOOOO!", "Wish me luck!", "To the sky!"), 60);
        gesture(G_CHEER, 40);
        ServerLevel sl = (ServerLevel) level();
        sl.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 2f, 0.7f);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, 0.4, 0.2, 0.4, 0);
        Skydive.cheer(sl, blockPosition(), p.name);
        mind(p).lastVisit.put("skydive", day());
    }

    private static Mind mind(CityData.Profile p) {
        return p.mind;
    }

    private void skyTick(CityData.Profile p) {
        ServerLevel sl = (ServerLevel) level();
        skyT++;
        getNavigation().stop();
        setZza(0);
        setXxa(0);
        fallDistance = 0;
        int ph = skyPhase();
        Vec3 v;
        double gy = skyTarget == null ? 70 : skyTarget.getY();
        double dx = skyTarget == null ? 0 : skyTarget.getX() + 0.5 - getX(), dz = skyTarget == null ? 0 : skyTarget.getZ() + 0.5 - getZ();
        double dist = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        switch (ph) {
            case Skydive.LAUNCH -> {
                double speed = Math.min(2.4, 0.4 + skyT * 0.12);
                if (getY() > skyApex - 25) speed = Math.max(0.15, (skyApex - getY()) * 0.09);
                v = new Vec3(0, speed, 0);
                if (skyT % 2 == 0) sl.sendParticles(ParticleTypes.FIREWORK, getX(), getY() - 0.3, getZ(), 3, 0.2, 0.2, 0.2, 0.05);
                if (getY() >= skyApex - 1 || skyT > 260) {
                    setSky(Skydive.FREEFALL);
                    say(pick("WOOOOOOO!", "I'm FLYING!", "Look at the city from up here!!", "AAAAAAAH!"), 70);
                }
            }
            case Skydive.FREEFALL -> {
                double fall = Math.min(1.25, 0.2 + skyT * 0.05);
                double tx = dx / dist, tz = dz / dist;
                double h = Math.min(0.55, dist * 0.02 + 0.1);
                v = new Vec3(tx * h - tz * 0.25 * Math.sin(skyT * 0.03), -fall, tz * h + tx * 0.25 * Math.sin(skyT * 0.03));
                if (skyT % 3 == 0) sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 1, 0.4, 0.4, 0.4, 0.02);
                if (getY() - gy < 32) {
                    setSky(Skydive.CHUTE);
                    sl.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.NEUTRAL, 1.5f, 0.7f);
                    say(pick("Chute's open!", "Phew, parachute!", "Ahh, so peaceful up here...", "Here I come!"), 60);
                }
            }
            case Skydive.CHUTE -> {
                double fall = Math.max(0.16, 0.9 - skyT * 0.08);
                double h = Math.min(0.22, dist * 0.03);
                v = new Vec3(dx / dist * h, -fall, dz / dist * h);
                if (onGround() || getY() <= gy + 0.05 || isInWater() || skyT > 600) {
                    setSky(Skydive.LANDED);
                    setNoGravity(false);
                    gesture(G_CHEER, 50);
                    say(pick("Nailed it!", "That was INCREDIBLE!", "Again! Again!", "My legs are jelly...", "Best. Day. Ever."), 80);
                    particles(ParticleTypes.HAPPY_VILLAGER, 8);
                }
            }
            default -> {
                v = Vec3.ZERO;
                if (skyT > 40) {
                    setSky(0);
                    setNoGravity(false);
                    long day = day();
                    p.fun = Math.min(100, p.fun + 35);
                    p.mind.remember(p, day, (int) timeOfDay(), "fun", "I went skydiving from the Sky Launch!", SkyTower.KEY, 4, 8);
                    p.log(routineDay()).note("I went skydiving from the Sky Launch");
                    data().news(day, p.name + " went skydiving from the Sky Launch!");
                    data().event(day, "fun", p.name + " went skydiving from the Sky Launch", blockPosition(), p.id);
                    if ("skydive".equals(leisureWhy)) {
                        leisureKey = "plaza";
                        leisureWhy = "hangout";
                        leisureReason = "still buzzing from my skydive";
                        rethink();
                    }
                }
            }
        }
        setDeltaMovement(v);
        hasImpulse = true;
        if (v.horizontalDistanceSqr() > 0.001) {
            float yaw = (float) (Math.toDegrees(Math.atan2(-v.x, v.z)));
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }
    }

    public void follow(Player pl, int ticks) {
        comeTo(pl, ticks);
        seekFollow = true;
    }

    public void comeTo(Player pl, int ticks) {
        seekFollow = false;
        seekPlayer = pl.getUUID();
        seekUntil = level().getGameTime() + ticks;
    }

    public boolean following(Player pl) {
        return seekFollow && seekPlayer != null && seekPlayer.equals(pl.getUUID()) && level().getGameTime() < seekUntil;
    }

    public boolean isFollowing() {
        return seekFollow && seekPlayer != null && level().getGameTime() < seekUntil;
    }

    public void stopSeeking() { seekPlayer = null; seekFollow = false; }

    public boolean seeking() {
        return seekPlayer != null && level().getGameTime() < seekUntil;
    }

    public void usePhone(int mode, int ticks, String what, Runnable done) {
        CityData.Profile p = profile();
        if (p == null) return;
        if (mode == 1 && phoneMode == 2) return;
        phoneMode = mode;
        phoneTicks = ticks;
        phoneWhat = what == null ? "" : what;
        phoneDone = done;
        if (eatTicks <= 0) {
            setItemSlot(EquipmentSlot.MAINHAND, PhoneItem.make(p.phoneColor));
            heldTicks = 4;
        }
        gesture(mode == 2 ? G_CALL : G_PHONE, 30);
        if (mode == 2) ringTicks = 0;
    }

    public void extendPhone(int ticks) {
        if (phoneMode > 0) phoneTicks = Math.max(phoneTicks, ticks);
    }

    public void putPhoneAway() {
        phoneWalk(false);
        phoneMode = 0;
        phoneTicks = 0;
        phoneWhat = "";
        phoneDone = null;
        phoneCooldown = 300 + getRandom().nextInt(400);
        if (heldTicks > 0) heldTicks = 1;
        if (getGesture() == G_PHONE || getGesture() == G_CALL) gesture(G_NONE, 1);
    }

    public void ringPhone(int ticks) {
        ringTicks = ticks;
    }

    private void ringTick() {
        ringTicks--;
        boolean muted = activityName().equals("work") || "library".equals(insideKey) || "library".equals(profile() == null ? "" : profile().lastPlace);
        if (muted) {
            if (ringTicks == 30) say("*bzzz* (phone on silent)", 30);
            return;
        }
        if (ringTicks % 12 == 0) level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.7f, ringTicks % 24 == 0 ? 1.5f : 1.3f);
        if (ringTicks == 30 && getRandom().nextFloat() < 0.6f) say(pick("Oh! My phone!", "Who's calling?", "One sec, my phone's ringing."), 40);
    }

    private boolean phoneTick(CityData.Profile p, long now, int phase) {
        if (phoneMode == 0) return false;
        if (!p.ownsPhone || convo != null || eatTicks > 0 || fleeTicks > 0 || isSleeping() || Elevator.isRider(this)) {
            if (phoneMode == 2) {
                Phones.Call c = null;
                for (Phones.Call x : Phones.PLAYER_CALLS.values()) if (profileId.equals(x.res)) c = x;
                for (Phones.Call x : Phones.RES_CALLS) if (profileId.equals(x.a) || profileId.equals(x.b)) c = x;
                if (c != null) Phones.hangup((ServerLevel) level(), data(), c, p.name + " had to go.", false);
            }
            putPhoneAway();
            return false;
        }
        heldTicks = Math.max(heldTicks, 3);
        if (!(getMainHandItem().getItem() instanceof PhoneItem)) setItemSlot(EquipmentSlot.MAINHAND, PhoneItem.make(p.phoneColor));
        if (now % 20 == phase % 20) gesture(phoneMode == 2 ? G_CALL : G_PHONE, 30);
        if (--phoneTicks <= 0) {
            Runnable done = phoneDone;
            putPhoneAway();
            if (done != null) {
                try { done.run(); } catch (Throwable t) { FireheartCity.LOG.error("Phone action failed for " + profileId, t); }
            }
            return false;
        }
        if (isPassenger() || isSeated() || Elevator.isQueued(this)) return false;
        if (phoneMode == 1 && !getNavigation().isDone()) {
            phoneWalk(true);
            return false;
        }
        phoneWalk(false);
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        return true;
    }

    private static final java.util.UUID PHONE_WALK = java.util.UUID.fromString("5b0f3c1e-8a2d-4c7e-9f41-2e6d8b1a7c30");

    private void phoneWalk(boolean on) {
        var att = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (att == null) return;
        boolean has = att.getModifier(PHONE_WALK) != null;
        if (on && !has) att.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(PHONE_WALK, "Texting while walking", -0.45, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        else if (!on && has) att.removeModifier(PHONE_WALK);
    }

    private void ferryApproach() {
        ServerLevel sl = (ServerLevel) level();
        int side = onIsland() ? Ferry.ISLE : Ferry.CITY;
        BlockPos pad = Ferry.pad(side);
        if (!blockPosition().closerThan(pad, 8)) return;
        Ferry f = Ferry.find(sl);
        if (f != null && f.dockedAt() == side && f.hasSeat()) {
            ferryWait = 0;
            if (distanceTo(f) < 5.2) {
                if (f.board(this, false)) {
                    getNavigation().stop();
                    hush();
                    if (getRandom().nextFloat() < 0.6f) say(side == Ferry.CITY ? pick("Neon Heights, here I come!", "Ooh, a window seat!", "Room for one more?") : pick("Back down to the city!", "Home time.", "Scoot over, I'm coming!"), 50);
                }
            } else getNavigation().moveTo(f.getX() - 3.5, f.getY(), f.getZ(), 0.8);
            return;
        }
        Ferry.call(sl, side);
        ferryWait++;
        if (ferryWait % 25 == 6 && getRandom().nextFloat() < 0.45f && !crowded(4.0)) say(pick("Come on, Sky Ferry...", "I can see the ferry coming!", "Any minute now...", "I hope there's a seat left.", "Is it just me or is the ferry slow today?"), 50);
        if (f == null && ferryWait > 60) {
            ferryWait = 0;
            int other = side == Ferry.ISLE ? Ferry.CITY : Ferry.ISLE;
            BlockPos to = ferryWaitSpot(other);
            if (sl.isPositionEntityTicking(to)) teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        }
    }

    private boolean pilotTick(CityData.Profile p) {
        if (p.job != Job.PILOT || !activityName().equals("work")) return false;
        ServerLevel sl = (ServerLevel) level();
        Ferry f = Ferry.find(sl);
        if (f == null) return false;
        int side = onIsland() ? Ferry.ISLE : Ferry.CITY;
        if (f.dockedAt() != side || distanceTo(f) > 40) return false;
        if (convo != null) return true;
        if (distanceTo(f) < 5.2 && f.hasSeat()) {
            if (f.board(this, true)) {
                getNavigation().stop();
                sayTo(pick("Captain Jet reporting for duty!", "Right, let's get this bird in the air.", "Pre-flight checks done. All aboard!"), 70);
            }
            return true;
        }
        if (tickCount % 20 == 0) getNavigation().moveTo(f.getX() - 3.5, f.getY(), f.getZ(), 0.9);
        return true;
    }

    private void ferryRideTick(Ferry f, CityData.Profile p, long now) {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (f.isPilot(this)) {
            if (!activityName().equals("work") && f.dockedAt() >= 0) {
                f.pilotOff(this);
                say(pick("That's my shift done. Autopilot's on!", "Clocking off - the ferry flies itself now."), 60);
            } else if (f.dockedAt() < 0 && now % 20 == 0) {
                Vec3 ahead = f.local(0, 2, 20);
                getLookControl().setLookAt(ahead.x, ahead.y, ahead.z);
            }
            return;
        }
        Place dest = destination();
        if (f.dockedAt() >= 0 && dest != null && dest.island == (f.dockedAt() == Ferry.ISLE)) {
            stopRiding();
            BlockPos w = ferryWaitSpot(f.dockedAt());
            teleportTo(w.getX() + 0.5, w.getY(), w.getZ() + 0.5);
            return;
        }
        if (f.dockedAt() < 0 && now % 60 == Math.floorMod(getId(), 60)) {
            Vec3 v = f.local(getRandom().nextBoolean() ? -12 : 12, -4 - getRandom().nextInt(8), getRandom().nextInt(20) - 6);
            getLookControl().setLookAt(v.x, v.y, v.z);
        }
    }

    public void ferryChatter(boolean up) {
        CityData.Profile p = profile();
        if (p == null) return;
        gesture(G_THINK, 40);
        say(up ? pick("Look, you can see the whole city from up here!", "I'll never get tired of this view.", "Is that the Sky Organ I can hear?", "The island looks amazing from here!", "Whoa, the clouds are so close!")
                : pick("There's Ember Heights! I can see my window!", "The marina looks tiny from up here.", "Home sweet city.", "I think I can see the clock tower!", "Look at all those lights down there."), 70);
    }

    public void leftFerry(boolean toIsland) {
        CityData.Profile p = profile();
        if (p == null) return;
        DayLog lg = p.log(routineDay());
        if (lg.once("ferry:" + toIsland)) lg.note(toIsland ? "I flew the Sky Ferry up to Neon Heights and loved the view" : "I rode the Sky Ferry back down to the city");
        if (getRandom().nextFloat() < 0.4f) say(toIsland ? pick("What a view up here!", "Neon Heights! I love this place.") : pick("Home sweet city!", "That flight never gets old."), 60);
        ferryWait = 0;
    }

    private void boardShuttle(boolean toIsland) {
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 25, 0.4, 0.8, 0.4, 0.05);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.7f, 1.6f);
        say(toIsland ? "Shuttle to Neon Heights, here I come!" : "Heading back down to the city!", 50);
        gesture(G_WAVE, 40);
        shuttleToIsland = toIsland;
        shuttleTicks = 700;
        this.setInvisible(true);
        this.setCustomNameVisible(false);
        this.getNavigation().stop();
    }

    private void boardSkyliner(int here) {
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 15, 0.4, 0.8, 0.4, 0.03);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 0.7f, 1.2f);
        say(here == Skyliner.CITY ? pick("All aboard the Skyliner!", "Neon Heights, here I come!") : pick("Back down to the city!", "Next stop, Solaris!"), 50);
        gesture(G_WAVE, 30);
        skyRider = true;
        skyFrom = here;
        skyTicks = 0;
        Skyliner.board(this);
        this.setInvisible(true);
        this.setCustomNameVisible(false);
        this.getNavigation().stop();
    }

    private void skyTick() {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        Skyliner.board(this);
        ServerLevel sl = (ServerLevel) level();
        int st = Skyliner.state(sl);
        int dest = skyFrom == Skyliner.CITY ? Skyliner.ISLE : Skyliner.CITY;
        if (st == dest || st < 0 || ++skyTicks > 3600) arriveSkyliner(dest == Skyliner.ISLE);
    }

    private void arriveSkyliner(boolean toIsland) {
        BlockPos to = toIsland ? Place.ISLE_PORT : Place.CITY_PORT;
        ServerLevel sl = (ServerLevel) level();
        if (!sl.isPositionEntityTicking(to)) return;
        Skyliner.leave(this);
        skyRider = false;
        this.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        this.setInvisible(false);
        this.setCustomNameVisible(true);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 15, 0.4, 0.8, 0.4, 0.03);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.7f, 1.2f);
        if (getRandom().nextFloat() < 0.5f) say(toIsland ? pick("What a view up here!", "Neon Heights! I love this place.") : pick("Home sweet city!", "That flight never gets old."), 60);
    }

    private void arriveShuttle() {
        BlockPos to = shuttleToIsland ? Place.ISLE_PORT : Place.CITY_PORT;
        ServerLevel sl = (ServerLevel) level();
        if (!sl.isPositionEntityTicking(to)) {
            shuttleTicks = 100;
            return;
        }
        this.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        this.setInvisible(false);
        this.setCustomNameVisible(true);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 25, 0.4, 0.8, 0.4, 0.05);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.7f, 1.6f);
    }

    private void catchUp() {
        if (Elevator.isRider(this) || Elevator.isQueued(this)) return;
        BlockPos t = navTarget();
        Place dest = destination();
        if (dest == null) return;
        if (level().getNearestPlayer(this, 48) != null) return;
        ServerLevel sl = (ServerLevel) level();
        if (dest.island != onIsland()) {
            if (Elevator.floorOfEntity(this) > 0) return;
            if (Ferry.strict || data().ferryStrict || FhcConfig.ferryStrict() || level().getNearestPlayer(this, 160) != null) return;
            BlockPos far = dest.entrance != null ? dest.entrance : dest.pos;
            if (Elevator.floorOfPos(far) > 0) far = Elevator.callSpot(0);
            BlockPos land = sl.isPositionEntityTicking(far) ? far : (dest.island ? Place.ISLE_PORT : Place.CITY_PORT);
            if (sl.isPositionEntityTicking(land)) {
                insideKey = null;
                this.teleportTo(land.getX() + 0.5, land.getY(), land.getZ() + 0.5);
            }
            return;
        }
        if (t == null) return;
        if (this.blockPosition().distSqr(t) > 40 * 40 && sl.isPositionEntityTicking(t)) {
            this.teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
        }
    }

    public boolean stuckRescue() {
        if (Elevator.isRider(this) || isPassenger()) return false;
        BlockPos t = navTarget();
        if (t == null || watched(this, 32)) return false;
        ServerLevel sl = (ServerLevel) level();
        if (!sl.isPositionEntityTicking(t)) return false;
        BlockPos land = Nav.standable(sl, t);
        if (land == null) land = t;
        if (watchedAt(Vec3.atBottomCenterOf(land), 32)) return false;
        this.teleportTo(land.getX() + 0.5, land.getY(), land.getZ() + 0.5);
        getNavigation().stop();
        return true;
    }

    /** True when a player is close by or can actually see this spot, so hidden fixes like teleports would be noticed. */
    private boolean watched(Entity e, double range) {
        for (Player pl : level().players()) {
            if (pl.isSpectator()) continue;
            double d = pl.distanceToSqr(e);
            if (d < 12 * 12 || d < range * range && pl.hasLineOfSight(e)) return true;
        }
        return false;
    }

    private boolean watchedAt(Vec3 v, double range) {
        for (Player pl : level().players()) {
            if (pl.isSpectator()) continue;
            double d = pl.distanceToSqr(v);
            if (d < 12 * 12) return true;
            if (d < range * range && Nav.sees(level(), pl.getEyePosition(), v.add(0, 1, 0), pl)) return true;
        }
        return false;
    }

    /** Runs to a reachable spot away from the threat, falling back to a straight line if no path is found. */
    public void flee(Vec3 threat, int ticks, double speed) {
        if (isSleeping() || isPassenger() || Elevator.isRider(this)) return;
        fleeTicks = ticks;
        Vec3 away = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(this, 12, 5, threat);
        if (away == null) away = position().subtract(threat).normalize().scale(10).add(position());
        fleeTarget = away;
        getNavigation().moveTo(away.x, away.y, away.z, speed);
    }

    public boolean fleeing() {
        return fleeTicks > 0;
    }

    public boolean canSee(Entity o, double range) {
        return this.distanceToSqr(o) <= range * range && Math.abs(this.getY() - o.getY()) < 2.5D && this.hasLineOfSight(o);
    }

    private void perceive(long now) {
        CityData d = data();
        CityData.Profile me = profile();
        long day = day();
        List<Resident> near = level().getEntitiesOfClass(Resident.class, this.getBoundingBox().inflate(8), r -> r != this && r.profile() != null && !r.inShuttle() && canSee(r, 8));
        for (Resident o : near) {
            CityData.Rel r = d.rel(me.id, o.profileId);
            if (r.seenDay != day) { r.seenDay = day; r.seenToday = 0; }
            if (r.seenToday < 6) { r.seenToday++; r.fam = Math.min(100, r.fam + 1); d.setDirty(); }
            CityData.Profile op = o.profile();
            if (r.met && !r.knowsJob && o.activityName().equals("work") && o.blockPosition().closerThan(op.job.work().pos, 6)) r.knowsJob = true;
            if (op.id.equals(me.partner) && r.seenToday == 1 && canSee(o, 5)) {
                particles(ParticleTypes.HEART, 3);
                o.particles(ParticleTypes.HEART, 3);
            }
        }
        Events.notice(d, this);
        Pets.notice(this);
        boolean festive = Festival.live(profileId);
        if (isFree() && !festive && now - lastConvoTick > 1200 && !Elevator.isRider(this)) {
            for (Resident o : near) {
                if (!o.isFree() || o.onPhone() || now - o.lastConvoTick < 1200 || !canSee(o, 5)) continue;
                if (Reception.busy(o) || Reception.busy(this) || Bank.onDuty(o) || Bank.onDuty(this) || o.waitingForTeller() || waitingForTeller() || o.playingMusic() || playingMusic() || o.speaking() || speaking()) continue;
                CityData.Rel r = d.rel(me.id, o.profileId);
                if (!pairMayChat(me.id, o.profileId, now, day, r.friend() || o.profileId.equals(me.partner) ? 3 : 1)) continue;
                double chance = me.trait.chattiness * 0.12;
                String act = activityName(), oact = o.activityName();
                boolean customer = (act.equals("lunch") || act.equals("morning") || leisureWhy.equals("shopping")) && oact.equals("work") && Economy.sellsFood(o.profile().job) && o.blockPosition().closerThan(o.profile().job.work().pos, 6);
                if (act.equals("work") && oact.equals("work")) chance *= 0.25;
                if (customer) chance = 0.7;
                if (r.friend()) chance *= 1.8;
                if (o.profileId.equals(me.partner)) chance *= 2.5;
                if (r.rival) chance *= 0.25;
                Mind mi = me.mind;
                if (mi.intentDay == day && mi.intentTarget.equals(o.profileId) && (mi.intentKind.equals("friend") || mi.intentKind.equals("makeup"))) chance = Math.max(chance, 0.6);
                if (!r.met) chance *= 0.9;
                if (me.social < 35) chance *= 1.5;
                if (Elevator.isQueued(this) && Elevator.isQueued(o)) chance *= 2.0;
                if (!customer && level().getEntitiesOfClass(Resident.class, getBoundingBox().inflate(6), x -> x.convo != null).size() > 0) continue;
                if (getRandom().nextFloat() < chance) {
                    startConversation(o, now, d, day, customer ? "shop" : null);
                    return;
                }
            }
        }
        if (!isFree()) return;
        String act = activityName();
        if (listenDay != day && !act.equals("work") && !act.equals("sleep") && !isSeated() && me.job != Job.MUSICIAN && !leisureWhy.equals("bank")) {
            for (Resident m : level().getEntitiesOfClass(Resident.class, getBoundingBox().inflate(12), x -> x != this && x.playingMusic())) {
                if (canSee(m, 12) && getRandom().nextFloat() < 0.6f) {
                    startListening(m);
                    return;
                }
            }
        }
        if (now - lastRead > 2400 && getRandom().nextFloat() < 0.35f) {
            BlockPos board = Gazette.boardNear(d, blockPosition(), 4);
            if (board != null) {
                lastRead = now;
                readBoard(d, me, board);
                return;
            }
        }
        Player seen = level().getNearestPlayer(this, 16);
        if (seen != null && !seen.isSpectator() && hasLineOfSight(seen)) {
            String sn = seen.getName().getString();
            Mind.sawPlayer(me, sn, day, me.lastPlace);
            Mind mi = me.mind;
            if (mi.intentKind.equals("player") && mi.intentTarget.equals(sn) && mi.intentDay == day && mi.greetedIntentDay != day && d.playerRel(me.id, sn).met && seekPlayer == null) {
                seekPlayer = seen.getUUID();
                seekUntil = now + 400;
            }
        }
        Player pl = level().getNearestPlayer(this, 6);
        if (pl != null && canSee(pl, 6)) {
            String pn = pl.getName().getString();
            Events.seePlayer(d, this, pn);
            CityData.Rel pr = d.playerRel(me.id, pn);
            Long last = lastGreet.get(pn);
            int trust = me.mind.trustIn(pn);
            if (seekPlayer != null && !seekFollow && seekPlayer.equals(pl.getUUID()) && me.mind.greetedIntentDay != day) {
                me.mind.greetedIntentDay = day;
                seekPlayer = null;
                lastGreet.put(pn, now);
                addressed(level(), pn);
                this.getLookControl().setLookAt(pl, 30, 30);
                gesture(G_WAVE, 50);
                String extra = Mind.playerLine(me, pn, day, getRandom());
                sayTo("There you are, " + pn + "! I was hoping I'd run into you today." + (extra != null ? " " + extra : ""), 120);
                me.mind.remember(me, day, (int) timeOfDay(), "player", "I went looking for {P} and we had a nice chat", me.lastPlace, 2, 4, "@" + pn);
                particles(ParticleTypes.HAPPY_VILLAGER, 5);
            } else if ((last == null || now - last > 3000) && trust <= -30 && pr.met && mayAddress(level(), pn) && getRandom().nextFloat() < 0.5f) {
                lastGreet.put(pn, now);
                addressed(level(), pn);
                this.getLookControl().setLookAt(pl, 30, 30);
                gesture(G_ANGRY, 30);
                String extra = Mind.playerLine(me, pn, day, getRandom());
                sayTo(pick("Oh. It's you.", "Hmph. " + pn + ".", "Keep your distance, " + pn + ".") + (extra != null ? " " + extra : ""), 80);
            } else if ((last == null || now - last > 3000) && mayAddress(level(), pn)) {
                if (pr.met && pl instanceof ServerPlayer sp && Favours.maybeAsk((ServerLevel) level(), d, this, me, sp, getRandom())) {
                    lastGreet.put(pn, now);
                    addressed(level(), pn);
                } else if (pr.met && pr.facts.containsKey("paid")) {
                    lastGreet.put(pn, now);
                    addressed(level(), pn);
                    this.getLookControl().setLookAt(pl, 30, 30);
                    gesture(G_CHEER, 40);
                    sayTo("Oh, " + pn + "! Thank you for the " + pr.facts.remove("paid") + " coins you sent me through the bank!", 90);
                    particles(ParticleTypes.HAPPY_VILLAGER, 6);
                } else if (pr.met && getRandom().nextFloat() < 0.5f) {
                    lastGreet.put(pn, now);
                    addressed(level(), pn);
                    this.getLookControl().setLookAt(pl, 30, 30);
                    boolean sweet = me.id.equals(Romance.sweetheart(d, pn));
                    gesture(sweet ? G_BLOW_KISS : G_WAVE, 40);
                    if (sweet) particles(ParticleTypes.HEART, 3);
                    String extra = pr.fam > 40 ? " Good to see you again." : "";
                    String bonus = Quests.birthdayLine(this, pn);
                    if (bonus == null) bonus = Bonds.maybeNickname(this, me, pn, pr);
                    if (bonus == null) bonus = Pastimes.greetExtra(this, me, pl, pr);
                    CityData.Event fresh = Events.freshestUnshared(d, me, pn);
                    String memo = Mind.playerLine(me, pn, day, getRandom());
                    String postSeen = Extras.postLine(d, me, pn, day);
                    if (bonus != null) extra = " " + bonus;
                    else if (postSeen != null) extra = " " + postSeen;
                    else if (memo != null && getRandom().nextFloat() < 0.6f) extra = " " + memo;
                    else if (fresh != null && getRandom().nextBoolean()) extra = " Did you hear? " + Events.sentence(fresh.text) + "!";
                    sayTo(Dialogue.greeting(timeOfDay()) + ", " + Bonds.callName(d, me, pn, getRandom()) + "!" + extra, 80);
                    pr.fam = Math.min(100, pr.fam + 1);
                } else if (!pr.met && getRandom().nextFloat() < 0.25f) {
                    lastGreet.put(pn, now);
                    addressed(level(), pn);
                    this.getLookControl().setLookAt(pl, 30, 30);
                    gesture(G_WAVE, 40);
                    sayTo("Oh, hello! I don't think we've met.", 60);
                }
            }
        }
    }

    private static final java.util.Map<String, long[]> PAIRS = new java.util.HashMap<>();
    public static final long PAIR_GAP = 6000;

    static String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    /** Two residents may chat again only after a few minutes, and only a few times a day. */
    static boolean pairMayChat(String a, String b, long now, long day, int perDay) {
        long[] v = PAIRS.get(pairKey(a, b));
        if (v == null) return true;
        if (now < v[0]) return true;
        if (now - v[0] < PAIR_GAP) return false;
        return v[1] != day || v[2] < perDay;
    }

    static void pairChatted(String a, String b, long now, long day) {
        long[] v = PAIRS.computeIfAbsent(pairKey(a, b), k -> new long[]{0, day, 0});
        if (v[1] != day) { v[1] = day; v[2] = 0; }
        v[0] = now;
        v[2]++;
    }

    public static void resetPairs() {
        PAIRS.clear();
    }

    public void startConversation(Resident o, long now, CityData d, long day, String kind) {
        if (profile() != null && o.profile() != null) pairChatted(profileId, o.profileId, now, day);
        Dialogue.Script s = Dialogue.build(this, o, d, day, kind);
        Conversation c = new Conversation(this, o, s);
        this.convo = c;
        o.convo = c;
        this.lastConvoTick = now;
        o.lastConvoTick = now;
    }

    void endConversation() {
        this.convo = null;
        this.lastConvoTick = this.level().getGameTime();
        CityData.Profile p = profile();
        if (p != null) p.social = Math.min(100, p.social + 12);
    }

    public void leaveConversation(String line) {
        if (convo == null) return;
        Conversation c = convo;
        Resident o = c.partnerOf(this);
        c.abort();
        say(line, 50);
        o.hush();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide || hand != InteractionHand.MAIN_HAND) return InteractionResult.sidedSuccess(this.level().isClientSide);
        try {
            return interact(player);
        } catch (Throwable t) {
            if (errors++ < 20) FireheartCity.LOG.error("Resident interaction failed", t);
            return InteractionResult.PASS;
        }
    }

    private InteractionResult interact(Player player) {
        CityData d = data();
        CityData.Profile p = profile();
        if (p == null) return InteractionResult.PASS;
        String pn = player.getName().getString();
        addressed(level(), pn);
        CityData.Rel pr = d.playerRel(p.id, pn);
        if (player instanceof ServerPlayer sp5 && Errands.found(sp5, this)) return InteractionResult.SUCCESS;
        if (p.job == Job.BANKER && Bank.onDuty(this) && convo == null && player instanceof ServerPlayer sp) {
            pr.met = true;
            pr.fam = Math.min(100, pr.fam + 2);
            d.setDirty();
            Bank.tellerInteract(sp, this);
            return InteractionResult.SUCCESS;
        }
        if (p.job == Job.CLERK && activityName().equals("work") && blockPosition().closerThan(TechStore.CENTER, 9) && player instanceof ServerPlayer sp3 && player.getMainHandItem().isEmpty() && Computers.isPc((ServerLevel) level(), TechStore.KIOSK)) {
            pr.met = true;
            getLookControl().setLookAt(player, 30, 30);
            gesture(G_WAVE, 30);
            sayTo(pick("Welcome to SolTech, " + pn + "! Have a look.", "Hi " + pn + "! Looking for a new phone?", "Welcome! The " + Phones.colorName(Phones.playerPhoneColor(sp3) + 2) + " SolPhone is flying off the shelves."), 70);
            Computers.openFor(sp3, TechStore.KIOSK);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp4 && Perks.deliver(sp4, this, d)) return InteractionResult.SUCCESS;
        if (pr.met && player instanceof ServerPlayer sp2) {
            this.getLookControl().setLookAt(player, 30, 30);
            if (Favours.tryComplete((ServerLevel) level(), d, this, p, sp2)) return InteractionResult.SUCCESS;
            if (tryGift(player, d, p, pn)) return InteractionResult.SUCCESS;
            if (Favours.maybeAsk((ServerLevel) level(), d, this, p, sp2, getRandom())) return InteractionResult.SUCCESS;
        }
        this.getLookControl().setLookAt(player, 30, 30);
        gesture(pr.met ? G_WAVE : G_SHAKE, 40);
        if (!pr.met) {
            pr.met = true;
            pr.fam = 10;
            d.setDirty();
             sayTo("Hi! I'm " + p.name + ", the " + p.job.title.toLowerCase() + " at " + p.job.work().label + ". And you're... " + pn + "? Nice to meet you!", 120);
            d.news(day(), p.name + " met " + pn + ".");
            return InteractionResult.SUCCESS;
        }
        pr.fam = Math.min(100, pr.fam + 2);
        d.setDirty();
        List<String> friends = d.friendsOf(p.id);
        StringBuilder sb = new StringBuilder("Hey " + pn + "! I'm " + status() + ".");
        float roll = getRandom().nextFloat();
        String memo = Mind.playerLine(p, pn, day(), getRandom());
        Mind mnd = p.mind;
        if (memo != null && roll < 0.3f) {
            sb.append(" ").append(memo);
            roll = 2f;
        } else if (!mnd.intent.isEmpty() && mnd.intentDay == day() && roll < 0.45f) {
            sb.append(" Today I'm hoping to ").append(mnd.intent).append(".");
            roll = 2f;
        } else if (!mnd.thought.isEmpty() && mnd.reflectDay == day() - 1 && roll < 0.55f && timeOfDay() < 6000) {
            sb.append(" Yesterday? ").append(Events.sentence(mnd.thought));
            roll = 2f;
        }
        CityData.Event fresh = Events.freshestUnshared(d, p, pn);
        if (roll > 1.5f) {
            sb.append("");
        } else if (fresh != null && roll < 0.35f) {
            sb.append(" Did you hear? ").append(Events.sentence(fresh.text)).append("!");
            Events.markTold(d, p, pn, fresh);
        } else if (!p.partner.isEmpty() && roll < 0.55f) {
            CityData.Profile q = d.profiles.get(p.partner);
            if (q != null) sb.append(" ").append(q.name).append(" and I are together now, by the way!");
        } else if (p.hunger < 30) {
            sb.append(" I'm starving, though.");
        } else if (roll < 0.62f && (Bank.savings(d, p.id) > 0 || Bank.loan(d, p.id) != null)) {
            Bank.Loan ln = Bank.loan(d, p.id);
            if (ln != null) sb.append(" Still paying off my bank loan - ").append(ln.owed).append(" coins to go.");
            else sb.append(" I've got ").append(Bank.savings(d, p.id)).append(" coins in the bank, saving up for ").append(p.goal).append("!");
        } else if (!p.log(routineDay()).empty() && roll < 0.65f) {
            sb.append(" My day so far? ").append(p.log(routineDay()).story(p, weekendNow()));
        } else if (!friends.isEmpty() && roll < 0.75f) {
            CityData.Profile f = d.profiles.get(friends.get(getRandom().nextInt(friends.size())));
            sb.append(" My friend ").append(f.name).append(" works at ").append(f.job.work().label).append(".");
        } else {
            int known = 0;
            for (CityData.Profile o : d.profiles.values()) {
                CityData.Rel r = d.peekRel(p.id, o.id);
                if (r != null && r.met) known++;
            }
            sb.append(known == 0 ? " I don't really know anyone here yet." : " I've met " + known + " people in town so far.");
        }
         sayTo(sb.toString(), 130);
        int known = 0;
        StringBuilder fr = new StringBuilder();
        for (CityData.Profile o : d.profiles.values()) {
            CityData.Rel r = d.peekRel(p.id, o.id);
            if (r != null && r.met) known++;
            if (r != null && r.friend()) fr.append(fr.length() == 0 ? "" : ", ").append(o.name);
        }
        CityData.Profile partner = p.partner.isEmpty() ? null : d.profiles.get(p.partner);
        player.displayClientMessage(Component.literal("§6" + Calendar.name(routineDay()) + "§7 · " + p.name + " · " + CityData.TIERS[p.tier] + " · " + p.job.title + " · " + p.coins + " coins · " + Bank.savings(d, p.id) + " saved" + (Bank.loan(d, p.id) != null ? " · loan " + Bank.loan(d, p.id).owed : "") + " · hunger " + p.hunger + "% · mood " + p.mood() + "% · knows " + known + (partner != null ? " · ❤ " + partner.name : "") + (fr.length() > 0 ? " · friends: " + fr : "")), true);
        return InteractionResult.SUCCESS;
    }

    private long lastHitTick = -1000;
    private int hitStreak;
    private long giftDay = -1;
    private int giftsToday;

    void resetGiftsForTest() {
        giftsToday = 0;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Snowball && source.getEntity() instanceof Player sp) {
            try {
                Hobbies.snowballedBy(this, sp);
            } catch (Throwable t) {
                if (errors++ < 20) FireheartCity.LOG.error("Snowball reaction failed", t);
            }
            return false;
        }
        if (!level().isClientSide && source.getEntity() instanceof Player pl) {
            try {
                onPunched(pl);
            } catch (Throwable t) {
                if (errors++ < 20) FireheartCity.LOG.error("Punch reaction failed", t);
            }
        }
        return super.hurt(source, amount);
    }

    void testHit(Player pl) {
        lastHitTick -= 20;
        onPunched(pl);
    }

    private void onPunched(Player pl) {
        long now = level().getGameTime();
        if (now - lastHitTick < 8) return;
        hitStreak = now - lastHitTick < 200 ? hitStreak + 1 : 1;
        lastHitTick = now;
        CityData d = data();
        CityData.Profile p = profile();
        if (p == null) return;
        String pn = pl.getName().getString();
        addressed(level(), pn);
        getLookControl().setLookAt(pl, 30, 30);
        if (convo != null) leaveConversation("Hey! Excuse me a second...");
        if (hitStreak == 1) {
            sayTo(pick("Ow! Hey, watch it!", "Oof! Careful, " + pn + "!", "Hey! That hurt!", "Ouch! What was that for?"), 50);
            gesture(G_ANGRY, 30);
            Mind.playerEvent(d, p, pn, day(), "{P} whacked me", -1, 2);
        } else {
            sayTo(pick("Stop hitting me, " + pn + "!", "What is WRONG with you?!", "I'm telling everyone about this!", "Leave me alone!"), 70);
            gesture(G_ANGRY, 50);
            particles(ParticleTypes.ANGRY_VILLAGER, 5);
            Mind.playerEvent(d, p, pn, day(), "{P} kept hitting me", -3, 5 + Math.min(3, hitStreak));
            d.playerRel(p.id, pn).aff -= 4;
            flee(pl.position(), 60, 1.3);
            if (hitStreak == 3) d.event(day(), "social", pn + " was seen hitting " + p.name + " near " + Dialogue.here(this), blockPosition(), p.id);
        }
        d.setDirty();
    }

    private static boolean giftable(ItemStack st) {
        String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()));
        return st.isEdible() || Economy.isFood(id) || st.is(net.minecraft.tags.ItemTags.FLOWERS) || st.is(net.minecraft.world.item.Items.CAKE) || st.is(net.minecraft.world.item.Items.DIAMOND)
                || st.is(net.minecraft.world.item.Items.EMERALD) || st.is(net.minecraft.world.item.Items.BOOK) || st.is(net.minecraft.world.item.Items.GOLD_INGOT) || st.is(net.minecraft.world.item.Items.MUSIC_DISC_CAT);
    }

    private boolean tryGift(Player player, CityData d, CityData.Profile p, String pn) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof PhoneItem && !player.isShiftKeyDown()) {
            getLookControl().setLookAt(player, 30, 30);
            if (p.ownsPhone) {
                sayTo(pick("I've already got a SolPhone, but thank you!", "Ooh, nice colour! I've got mine though."), 60);
                return true;
            }
            int col = PhoneItem.color(held);
            if (!player.getAbilities().instabuild) held.shrink(1);
            Phones.give((ServerLevel) level(), d, p, col, " - a gift from " + pn + "!");
            Mind.playerEvent(d, p, pn, day(), "{P} gave me a SolPhone", 5, 8);
            gesture(G_CHEER, 60);
            sayTo(pick("A SolPhone?! For me?! Thank you, " + pn + "!", "No way! My very own SolPhone! I love the " + Phones.colorName(col) + "!"), 80);
            usePhone(1, 100, "setting up their new SolPhone", () -> Phones.browse(this, p, false));
            d.news(day(), pn + " gave " + p.name + " a SolPhone.");
            return true;
        }
        if (held.isEmpty() || !giftable(held) || player.isShiftKeyDown()) return false;
        if (held.is(net.minecraft.world.item.Items.GOLD_NUGGET) || held.is(net.minecraft.world.item.Items.GOLD_BLOCK)) return false;
        long dd = day();
        if (giftDay != dd) { giftDay = dd; giftsToday = 0; }
        getLookControl().setLookAt(player, 30, 30);
        if (giftsToday >= 3) {
            sayTo(pick("You're too kind, but I really couldn't take another thing today!", "Save some for the others, " + pn + "!"), 70);
            return true;
        }
        giftsToday++;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        boolean fav = id.equals(Memory.favourite(p));
        boolean precious = held.is(net.minecraft.world.item.Items.DIAMOND) || held.is(net.minecraft.world.item.Items.EMERALD) || held.is(net.minecraft.world.item.Items.GOLD_INGOT) || held.is(net.minecraft.world.item.Items.CAKE);
        String label = Economy.label(id);
        if (!player.getAbilities().instabuild) held.shrink(1);
        p.add(id, 1);
        showItem(id, 60);
        CityData.Rel pr = d.playerRel(p.id, pn);
        pr.aff = Math.min(100, pr.aff + (fav ? 8 : precious ? 6 : 3));
        if (Letters.lucky(d, pn, dd, p.id)) {
            pr.aff = Math.min(100, pr.aff + (fav ? 8 : precious ? 6 : 3));
            particles(ParticleTypes.HAPPY_VILLAGER, 6);
        }
        pr.fam = Math.min(100, pr.fam + 2);
        int emo = fav || precious ? 4 : 2;
        Mind.playerEvent(d, p, pn, dd, "{P} gave me " + label + (fav ? ", my favourite" : ""), emo, fav || precious ? 7 : 4);
        if (fav) {
            gesture(G_CHEER, 60);
            particles(ParticleTypes.HEART, 6);
            sayTo(pick("No way - " + label + " is my absolute favourite! How did you know?!", "My favourite! " + pn + ", you're the best!"), 100);
        } else if (precious) {
            gesture(G_CHEER, 50);
            particles(ParticleTypes.HAPPY_VILLAGER, 8);
            sayTo(pick("For me?! That's so generous, " + pn + "!", "Wow... I don't know what to say. Thank you!"), 90);
        } else {
            gesture(G_GIVE, 40);
            particles(ParticleTypes.HEART, 2);
            int t = p.mind.trustIn(pn);
            sayTo(t < -20 ? pick("...Thanks, I guess. Doesn't make up for everything.", "Hmph. Fine. Thank you.") : pick("Aww, thank you, " + pn + "!", "That's really sweet of you!", "Ooh, " + label + "! Thanks!"), 80);
        }
        d.news(dd, pn + " gave " + p.name + " " + label + ".");
        Quests.bump(d, pn, "gift");
        Health.onGift(this, p, id, player);
        if (fav || precious) Letters.thankYou((ServerLevel) level(), d, pn, p, "the " + label);
        d.setDirty();
        return true;
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Profile", profileId);
        tag.putInt("Skin", getSkin());
        if (insideKey != null) tag.putString("Inside", insideKey);
        tag.putInt("Shuttle", shuttleTicks);
        tag.putBoolean("ShuttleUp", shuttleToIsland);
        tag.putBoolean("SkyRider", skyRider);
        tag.putInt("SkyFrom", skyFrom);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        profileId = tag.getString("Profile");
        this.entityData.set(SKIN, tag.getInt("Skin"));
        this.entityData.set(VOICE_ID, Cast.voiceFor(profileId));
        insideKey = tag.contains("Inside") ? tag.getString("Inside") : null;
        if (insideKey != null && insideKey.startsWith("apt")) insideKey = null;
        shuttleTicks = 0;
        shuttleToIsland = tag.getBoolean("ShuttleUp");
        skyRider = false;
        this.setInvisible(false);
        skyFrom = tag.getInt("SkyFrom");
    }
}
