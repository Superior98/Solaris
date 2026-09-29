package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Network messages for the Fireheart PC (FireOS). */
public final class PcNet {
    private PcNet() {}

    private static final String VERSION = "6";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(FireheartCity.MODID, "pc"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.messageBuilder(Data.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Data::write).decoder(Data::read).consumerMainThread(PcNet::onData).add();
        CHANNEL.messageBuilder(Msg.class, 1, NetworkDirection.PLAY_TO_CLIENT).encoder(Msg::write).decoder(Msg::read).consumerMainThread(PcNet::onMsg).add();
        CHANNEL.messageBuilder(Act.class, 2, NetworkDirection.PLAY_TO_SERVER).encoder(Act::write).decoder(Act::read).consumerMainThread(PcNet::onAct).add();
        CHANNEL.messageBuilder(BlobUp.class, 3, NetworkDirection.PLAY_TO_SERVER).encoder(BlobUp::write).decoder(BlobUp::readUp).consumerMainThread(PcNet::onBlobUp).add();
        CHANNEL.messageBuilder(Blob.class, 4, NetworkDirection.PLAY_TO_CLIENT).encoder(Blob::write).decoder(Blob::read).consumerMainThread(PcNet::onBlobDown).add();
    }

    public static final class Data {
        public BlockPos pos = BlockPos.ZERO;
        public String owner = "", player = "", clock = "", notes = "";
        public int coins, savings;
        public boolean hasAccount;
        public final List<String> news = new ArrayList<>();
        public final List<String[]> residents = new ArrayList<>();
        public final List<String> inbox = new ArrayList<>();
        public final List<String> statement = new ArrayList<>();
        public final List<String> scores = new ArrayList<>();
        public int device, phoneColor, px, pz;
        public boolean kiosk, pisle, inStore;
        public String ferry = "", weather = "";
        public final List<String> feed = new ArrayList<>();
        public final List<String> map = new ArrayList<>();
        public final List<String> catalog = new ArrayList<>();
        public final List<String> unread = new ArrayList<>();
        public int battery = 100;
        public boolean charging;
        public String settings = "0|0|0|1";
        public final List<String> follows = new ArrayList<>();
        public final List<String> menu = new ArrayList<>();
        public final List<String> gallery = new ArrayList<>();
        public long tv;
        public final List<String> uploads = new ArrayList<>();
        public final List<String> orders = new ArrayList<>();

        static void list(FriendlyByteBuf b, List<String> l) {
            b.writeVarInt(l.size());
            for (String s : l) b.writeUtf(s.length() > 8000 ? s.substring(0, 8000) : s, 32767);
        }

        static void rlist(FriendlyByteBuf b, List<String> l) {
            int n = b.readVarInt();
            for (int i = 0; i < n; i++) l.add(b.readUtf(32767));
        }

        void write(FriendlyByteBuf b) {
            b.writeBlockPos(pos);
            b.writeUtf(owner);
            b.writeUtf(player);
            b.writeUtf(clock);
            b.writeUtf(notes, 8192);
            b.writeVarInt(coins);
            b.writeVarInt(savings);
            b.writeBoolean(hasAccount);
            list(b, news);
            b.writeVarInt(residents.size());
            for (String[] r : residents) { b.writeUtf(r[0]); b.writeUtf(r[1]); b.writeUtf(r[2]); b.writeUtf(r[3]); b.writeUtf(r[4]); b.writeUtf(r.length > 5 ? r[5] : "-1"); }
            list(b, inbox);
            list(b, statement);
            list(b, scores);
            b.writeVarInt(device);
            b.writeVarInt(phoneColor);
            b.writeInt(px);
            b.writeInt(pz);
            b.writeBoolean(kiosk);
            b.writeBoolean(pisle);
            b.writeBoolean(inStore);
            b.writeUtf(ferry);
            b.writeUtf(weather);
            list(b, feed);
            list(b, map);
            list(b, catalog);
            list(b, unread);
            b.writeVarInt(battery);
            b.writeBoolean(charging);
            b.writeUtf(settings);
            list(b, follows);
            list(b, menu);
            list(b, gallery);
            b.writeLong(tv);
            list(b, uploads);
            list(b, orders);
        }

        static Data read(FriendlyByteBuf b) {
            Data d = new Data();
            d.pos = b.readBlockPos();
            d.owner = b.readUtf();
            d.player = b.readUtf();
            d.clock = b.readUtf();
            d.notes = b.readUtf(8192);
            d.coins = b.readVarInt();
            d.savings = b.readVarInt();
            d.hasAccount = b.readBoolean();
            rlist(b, d.news);
            int n = b.readVarInt();
            for (int i = 0; i < n; i++) d.residents.add(new String[]{b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf()});
            rlist(b, d.inbox);
            rlist(b, d.statement);
            rlist(b, d.scores);
            d.device = b.readVarInt();
            d.phoneColor = b.readVarInt();
            d.px = b.readInt();
            d.pz = b.readInt();
            d.kiosk = b.readBoolean();
            d.pisle = b.readBoolean();
            d.inStore = b.readBoolean();
            d.ferry = b.readUtf();
            d.weather = b.readUtf();
            rlist(b, d.feed);
            rlist(b, d.map);
            rlist(b, d.catalog);
            rlist(b, d.unread);
            d.battery = b.readVarInt();
            d.charging = b.readBoolean();
            d.settings = b.readUtf();
            rlist(b, d.follows);
            rlist(b, d.menu);
            rlist(b, d.gallery);
            d.tv = b.readLong();
            rlist(b, d.uploads);
            rlist(b, d.orders);
            return d;
        }
    }

    public static final class Msg {
        public final String line;

        public Msg(String line) {
            this.line = line;
        }

        void write(FriendlyByteBuf b) {
            b.writeUtf(line, 4096);
        }

        static Msg read(FriendlyByteBuf b) {
            return new Msg(b.readUtf(4096));
        }
    }

    public static final class Act {
        public final BlockPos pos;
        public final String kind, a, b;

        public Act(BlockPos pos, String kind, String a, String b) {
            this.pos = pos;
            this.kind = kind;
            this.a = a == null ? "" : a;
            this.b = b == null ? "" : b;
        }

        void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(kind);
            buf.writeUtf(a, 8192);
            buf.writeUtf(b, 8192);
        }

        static Act read(FriendlyByteBuf buf) {
            return new Act(buf.readBlockPos(), buf.readUtf(), buf.readUtf(8192), buf.readUtf(8192));
        }
    }

    /** A chunk of a binary file (photos, videos): kind, id, chunk index, chunk count and bytes. */
    public static class Blob {
        public final String kind, id, meta;
        public final int index, total;
        public final byte[] bytes;

        public Blob(String kind, String id, String meta, int index, int total, byte[] bytes) {
            this.kind = kind;
            this.id = id;
            this.meta = meta == null ? "" : meta;
            this.index = index;
            this.total = total;
            this.bytes = bytes;
        }

        void write(FriendlyByteBuf b) {
            b.writeUtf(kind, 32);
            b.writeUtf(id, 64);
            b.writeUtf(meta, 2048);
            b.writeVarInt(index);
            b.writeVarInt(total);
            b.writeByteArray(bytes);
        }

        static Blob read(FriendlyByteBuf b) {
            return new Blob(b.readUtf(32), b.readUtf(64), b.readUtf(2048), b.readVarInt(), b.readVarInt(), b.readByteArray(40000));
        }
    }

    public static final class BlobUp extends Blob {
        public BlobUp(String kind, String id, String meta, int index, int total, byte[] bytes) {
            super(kind, id, meta, index, total, bytes);
        }

        static BlobUp readUp(FriendlyByteBuf b) {
            return new BlobUp(b.readUtf(32), b.readUtf(64), b.readUtf(2048), b.readVarInt(), b.readVarInt(), b.readByteArray(40000));
        }
    }

    static void onBlobUp(BlobUp m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ServerPlayer pl = ctx.get().getSender();
        if (pl == null) return;
        try {
            Photos.receive(pl, m);
        } catch (Throwable t) {
            FireheartCity.LOG.error("Upload failed", t);
        }
    }

    static void onBlobDown(Blob m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.fireheart.city.client.pc.PhotoCache.receive(m));
    }

    static void onData(Data d, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.fireheart.city.client.pc.ClientPc.show(d));
    }

    static void onMsg(Msg m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.fireheart.city.client.pc.ClientPc.message(m.line));
    }

    static void onAct(Act a, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ServerPlayer pl = ctx.get().getSender();
        if (pl == null) return;
        try {
            if (a.a.contains("[pic:here]") || a.b.contains("[pic:here]")) {
                java.util.List<String> gal = Extras.gallery(CityData.get(pl.serverLevel()), pl.getName().getString());
                String k = "[pic:" + (!gal.isEmpty() && gal.get(0).startsWith("ph_") ? gal.get(0).split(":")[0] : Extras.sceneAt(pl)) + "]";
                a = new Act(a.pos, a.kind, a.a.replace("[pic:here]", k), a.b.replace("[pic:here]", k));
            }
            Computers.action(pl, a);
        } catch (Throwable t) {
            FireheartCity.LOG.error("PC action failed", t);
        }
    }

    public static void send(ServerPlayer pl, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> pl), msg);
    }
}
