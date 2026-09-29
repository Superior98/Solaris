package com.fireheart.city;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum Job {
    BAKER("Baker", "bakery", Items.BREAD, "Fresh bread, straight from the oven!", "One loaf of your best bread, please!", "Here you go - still warm!"),
    COOK("Cook", "diner", Items.COOKED_BEEF, "Order up!", "I'll have the usual burger, please.", "Coming right up, extra crispy!"),
    CLERK("Clerk", "supply", Items.PAPER, "Shafts, cogs, belts - we've got it all!", "Got any cogwheels in stock?", "Sure, how many do you need?"),
    GROCER("Grocer", "market", Items.CARROT, "Fresh veggies today!", "Are the tomatoes fresh today?", "Picked this morning!"),
    MECHANIC("Mechanic", "garage", Items.IRON_INGOT, "Who left the car in bay two again?", "Can you check my engine sometime?", "Bring it round, I'll take a look."),
    FACTORY_WORKER("Factory Worker", "factory", Items.IRON_NUGGET, "Another batch of iron sheets done.", "How's the belt line running?", "Smooth as ever today."),
    QUARRY_WORKER("Quarry Worker", "gravel", Items.FLINT, "Gravel, gravel and more gravel.", "Need any gravel hauled?", "Always, we've got mountains of it."),
    CRANE_OPERATOR("Crane Operator", "port", Items.CHAIN, "Container number nine, coming through!", "Busy day at the port?", "Three ships unloaded already!"),
    DOCKMASTER("Dockmaster", "marina", Items.FISHING_ROD, "Calm waters today.", "Is the boat free this afternoon?", "She's all yours, just bring her back!"),
    GARDENER("Gardener", "park", Items.POPPY, "These cherry trees won't prune themselves.", "The flowers look lovely today!", "Thanks, I just watered them."),
    ATTENDANT("Station Attendant", "fuel", Items.BUCKET, "Diesel's flowing today!", "Fill her up, please.", "Diesel, coming right up."),
    CLOCKKEEPER("Clock Keeper", "clock", Items.CLOCK, "Right on time, as always.", "What time is it?", "Time for a break, if you ask me!"),
    NOODLE_CHEF("Noodle Chef", "noodle", Items.BOWL, "Noodles, hot and neon!", "One bowl of neon ramen, please!", "Extra spicy, just how you like it."),
    ARCADE_KEEPER("Arcade Keeper", "arcade", Items.EMERALD, "New high score on cabinet three!", "Any new games in the arcade?", "Try the dance floor, it's glowing tonight!"),
    GUIDE("Sky Guide", "isle_plaza", Items.SPYGLASS, "Welcome to Neon Heights!", "What's the best view up here?", "The spire deck, hands down."),
    PILOT("Sky Ferry Pilot", "ferry_city", Items.FEATHER, "Shuttle departs on time, every time.", "When's the next shuttle?", "Just hop on, I'll get you up there."),
    LIBRARIAN("Librarian", "library", Items.BOOK, "Shh... new books just came in!", "Got any good books for me?", "Try the mystery shelf, second from the left."),
    BANKER("Banker", "bank", Items.WRITABLE_BOOK, "Deposits, withdrawals, loans - step right up!", "I'd like to make a deposit, please.", "Of course - your coins are safe with us."),
    POSTMAN("Postman", "post", Items.PAPER, "Letters, parcels, postcards - Solaris Post delivers!", "Anything in the post for me?", "Let me check the pigeonholes..."),
    MUSICIAN("Street Musician", "plaza", Items.NOTE_BLOCK, "A song for the good people of Solaris!", "Can you play my favourite song?", "For you? Anything!"),
    RECEPTIONIST("Receptionist", "reception", Items.TRIPWIRE_HOOK, "Welcome to Ember Heights, make yourself at home.", "Can I get my room key?", "Here you go, no rush."),
    POLICE("Police Officer", "police", Items.LIGHTNING_ROD, "Solaris PD - keeping you safe!", "Anything to report, officer?", "All quiet on my beat."),
    FIREFIGHTER("Firefighter", "fire", Items.WATER_BUCKET, "Solaris Fire Dept - always ready!", "Any fires today?", "Not on my watch. All quiet."),
    REPAIR("Repair Technician", "garage", Items.IRON_PICKAXE, "If it's broken, I'll fix it!", "Can you fix something for me?", "Point me at it - I'll have it good as new.");

    public final String title;
    public final String workKey;
    public final Item tool;
    public final String shout;
    public final String order;
    public final String reply;

    Job(String title, String workKey, Item tool, String shout, String order, String reply) {
        this.title = title;
        this.workKey = workKey;
        this.tool = tool;
        this.shout = shout;
        this.order = order;
        this.reply = reply;
    }

    public Place work() {
        return Place.get(workKey);
    }

    public static Job byName(String s) {
        for (Job j : values()) if (j.name().equalsIgnoreCase(s)) return j;
        return BAKER;
    }
}
