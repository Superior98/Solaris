package com.fireheart.city;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(FireheartCity.MODID)
public class FireheartCity {
    public static final String MODID = "fireheartcity";
    public static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> STEAM = PARTICLES.register("steam", () -> new net.minecraft.core.particles.SimpleParticleType(false) {});

    public static final RegistryObject<ComputerBlock> COMPUTER = BLOCKS.register("computer",
            () -> new ComputerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)
                    .strength(1.5F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(st -> st.getValue(ComputerBlock.SCREEN) > 0 ? 7 : 0)));
    public static final RegistryObject<net.minecraft.world.item.Item> COMPUTER_ITEM = ITEMS.register("computer",
            () -> new net.minecraft.world.item.BlockItem(COMPUTER.get(), new net.minecraft.world.item.Item.Properties()));

    public static final RegistryObject<TvBlock> TV = BLOCKS.register("tv",
            () -> new TvBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(1.2F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(st -> st.getValue(TvBlock.ON) ? 6 : 0)));
    public static final RegistryObject<net.minecraft.world.item.Item> TV_ITEM = ITEMS.register("tv",
            () -> new net.minecraft.world.item.BlockItem(TV.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<ConsoleBlock> SOLBOX = BLOCKS.register("solbox",
            () -> new ConsoleBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(1.2F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(st -> st.getValue(ConsoleBlock.ON) ? 4 : 0)));
    public static final RegistryObject<net.minecraft.world.item.Item> SOLBOX_ITEM = ITEMS.register("solbox",
            () -> new net.minecraft.world.item.BlockItem(SOLBOX.get(), new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<net.minecraft.world.item.Item> REMOTE = ITEMS.register("remote", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<net.minecraft.world.item.Item> CONTROLLER = ITEMS.register("controller", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<MailboxBlock> MAILBOX = BLOCKS.register("mailbox",
            () -> new MailboxBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLUE).strength(1.5F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final RegistryObject<net.minecraft.world.item.Item> MAILBOX_ITEM = ITEMS.register("mailbox",
            () -> new net.minecraft.world.item.BlockItem(MAILBOX.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<ParcelBlock> PARCEL = BLOCKS.register("delivery_box",
            () -> new ParcelBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(0.4F)
                    .sound(net.minecraft.world.level.block.SoundType.WOOL).noOcclusion()));
    public static final RegistryObject<net.minecraft.world.item.Item> PARCEL_ITEM = ITEMS.register("delivery_box",
            () -> new net.minecraft.world.item.BlockItem(PARCEL.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<CeilingLightBlock> LIGHT_PANEL = BLOCKS.register("ceiling_panel",
            () -> new CeilingLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.SNOW).strength(0.5F)
                    .sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion()
                    .lightLevel(st -> st.getValue(CeilingLightBlock.LIT) ? 15 : 0), "panel"));
    public static final RegistryObject<net.minecraft.world.item.Item> LIGHT_PANEL_ITEM = ITEMS.register("ceiling_panel",
            () -> new net.minecraft.world.item.BlockItem(LIGHT_PANEL.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<CeilingLightBlock> LIGHT_ROUND = BLOCKS.register("ceiling_round",
            () -> new CeilingLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.SNOW).strength(0.5F)
                    .sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion()
                    .lightLevel(st -> st.getValue(CeilingLightBlock.LIT) ? 15 : 0), "round"));
    public static final RegistryObject<net.minecraft.world.item.Item> LIGHT_ROUND_ITEM = ITEMS.register("ceiling_round",
            () -> new net.minecraft.world.item.BlockItem(LIGHT_ROUND.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<CeilingLightBlock> LIGHT_SPOT = BLOCKS.register("ceiling_spot",
            () -> new CeilingLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.SNOW).strength(0.5F)
                    .sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion()
                    .lightLevel(st -> st.getValue(CeilingLightBlock.LIT) ? 15 : 0), "spot"));
    public static final RegistryObject<net.minecraft.world.item.Item> LIGHT_SPOT_ITEM = ITEMS.register("ceiling_spot",
            () -> new net.minecraft.world.item.BlockItem(LIGHT_SPOT.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<CeilingLightBlock> LIGHT_PENDANT = BLOCKS.register("ceiling_pendant",
            () -> new CeilingLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.SNOW).strength(0.5F)
                    .sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion()
                    .lightLevel(st -> st.getValue(CeilingLightBlock.LIT) ? 15 : 0), "pendant"));
    public static final RegistryObject<net.minecraft.world.item.Item> LIGHT_PENDANT_ITEM = ITEMS.register("ceiling_pendant",
            () -> new net.minecraft.world.item.BlockItem(LIGHT_PENDANT.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<LaunchPadBlock> LAUNCH_PAD = BLOCKS.register("launch_pad",
            () -> new LaunchPadBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(st -> 12)));
    public static final RegistryObject<net.minecraft.world.item.Item> LAUNCH_PAD_ITEM = ITEMS.register("launch_pad",
            () -> new net.minecraft.world.item.BlockItem(LAUNCH_PAD.get(), new net.minecraft.world.item.Item.Properties()));
    public static final RegistryObject<DeviceItem> TABLET = ITEMS.register("tablet", () -> new DeviceItem("tablet", new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<DeviceItem> CONSOLE = ITEMS.register("console", () -> new DeviceItem("console", new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<DeviceItem> WATCH = ITEMS.register("watch", () -> new DeviceItem("watch", new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<Dishes.DishItem> DISH = ITEMS.register("dish", () -> new Dishes.DishItem(new net.minecraft.world.item.Item.Properties().stacksTo(16).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationMod(0.6f).build())));
    public static final RegistryObject<net.minecraft.world.item.Item> SIDEARM = ITEMS.register("sidearm", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<DeviceItem> HEADPHONES = ITEMS.register("headphones", () -> new DeviceItem("headphones", new net.minecraft.world.item.Item.Properties().stacksTo(1)));

    public static final RegistryObject<PhoneItem> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)));

    public static final RegistryObject<EntityType<Resident>> RESIDENT = ENTITIES.register("resident",
            () -> EntityType.Builder.<Resident>of(Resident::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(MODID + ":resident"));

    public static final RegistryObject<EntityType<Ferry>> FERRY = ENTITIES.register("sky_ferry",
            () -> EntityType.Builder.<Ferry>of(Ferry::new, MobCategory.MISC)
                    .sized(3.0F, 2.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(MODID + ":sky_ferry"));

    public static final RegistryObject<EntityType<Vehicle>> VEHICLE = ENTITIES.register("vehicle",
            () -> EntityType.Builder.<Vehicle>of(Vehicle::new, MobCategory.MISC)
                    .sized(1.9F, 1.4F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(MODID + ":vehicle"));
    public static final RegistryObject<Vehicles.Key> CAR_KEY = ITEMS.register("car_key", () -> new Vehicles.Key(Vehicle.CAR, new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<Vehicles.Key> BIKE_KEY = ITEMS.register("bike_key", () -> new Vehicles.Key(Vehicle.BIKE, new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    public static final RegistryObject<Vehicles.Key> BOAT_KEY = ITEMS.register("boat_key", () -> new Vehicles.Key(Vehicle.BOAT, new net.minecraft.world.item.Item.Properties().stacksTo(1)));

    public FireheartCity() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        PARTICLES.register(modBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, FhcConfig.SPEC);
        BankNet.register();
        PcNet.register();
        modBus.addListener(this::onTabs);
        modBus.addListener(this::onAttributes);
        MinecraftForge.EVENT_BUS.addListener(this::onCommands);
        MinecraftForge.EVENT_BUS.addListener(Events::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(Skydive::onFall);
        MinecraftForge.EVENT_BUS.addListener(Events::onBreak);
        MinecraftForge.EVENT_BUS.addListener(Events::onPlace);
        MinecraftForge.EVENT_BUS.addListener(Events::onExplosion);
        MinecraftForge.EVENT_BUS.addListener(Events::onLogin);
        MinecraftForge.EVENT_BUS.addListener(Tour::onJoin);
        MinecraftForge.EVENT_BUS.addListener(Kitchen::onJoin);
        MinecraftForge.EVENT_BUS.addListener(Events::onStopped);
        MinecraftForge.EVENT_BUS.addListener(ParrotLove::onInteract);
        MinecraftForge.EVENT_BUS.addListener(Bank::onClick);
        MinecraftForge.EVENT_BUS.addListener(Chat::onChat);
        MinecraftForge.EVENT_BUS.addListener(Quests::onFished);
        MinecraftForge.EVENT_BUS.addListener(Letters::onBreak);
        MinecraftForge.EVENT_BUS.addListener(Letters::onJoin);
    }

    private void onTabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS || e.getTabKey() == net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS) e.accept(COMPUTER_ITEM);
        if (e.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(COMPUTER_ITEM);
            for (int c = 0; c < Phones.COLORS.length; c++) e.accept(PhoneItem.make(c));
            for (int c = 0; c < Phones.COLORS.length; c++) e.accept(PhoneItem.make(c, 2));
            e.accept(TV_ITEM);
            e.accept(TABLET);
            e.accept(CONSOLE);
            e.accept(SOLBOX_ITEM);
            e.accept(MAILBOX_ITEM);
            e.accept(LIGHT_PANEL_ITEM);
            e.accept(LIGHT_ROUND_ITEM);
            e.accept(LIGHT_SPOT_ITEM);
            e.accept(LIGHT_PENDANT_ITEM);
            e.accept(REMOTE);
            e.accept(CONTROLLER);
            e.accept(WATCH);
            e.accept(HEADPHONES);
            e.accept(LAUNCH_PAD_ITEM);
        }
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(RESIDENT.get(), Resident.createAttributes().build());
    }

    private void onCommands(RegisterCommandsEvent event) {
        CityCommand.register(event.getDispatcher());
        CityCommand.registerBank(event.getDispatcher());
        CityCommand.registerCivic(event.getDispatcher());
    }
}
