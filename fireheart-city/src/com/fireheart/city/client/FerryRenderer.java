package com.fireheart.city.client;

import com.fireheart.city.Ferry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.inventory.InventoryMenu;

public class FerryRenderer extends EntityRenderer<Ferry> {
    private record Part(int x, int y, int z, BlockState state, boolean glow) {}

    private static List<Part> parts;
    private static int errors;

    public FerryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.2f;
    }

    private static BlockState block(String id, BlockState fallback) {
        Block b = BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
        return b == null || b == Blocks.AIR ? fallback : b.defaultBlockState();
    }

    private static List<Part> build() {
        List<Part> p = new ArrayList<>();
        BlockState hull = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState deck = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState trim = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState seat = block("create:orange_seat", Blocks.ORANGE_CARPET.defaultBlockState());
        BlockState helmSeat = block("create:black_seat", Blocks.BLACK_CARPET.defaultBlockState());
        BlockState fenceZ = Blocks.SPRUCE_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
        BlockState fenceX = Blocks.SPRUCE_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.WEST, true);
        BlockState post = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int z = -4; z <= 4; z++) for (int x = -1; x <= 1; x++) p.add(new Part(x, 0, z, hull, false));
        p.add(new Part(0, 0, 5, hull, false));
        p.add(new Part(0, 0, -5, hull, false));
        for (int z = -3; z <= 3; z++) {
            p.add(new Part(2, 0, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP), false));
            p.add(new Part(-2, 0, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP), false));
        }
        for (int z = -4; z <= 4; z++) {
            p.add(new Part(2, 1, z, z == 0 ? Blocks.ORANGE_CONCRETE.defaultBlockState() : trim, false));
            p.add(new Part(-2, 1, z, z == 0 ? Blocks.ORANGE_CONCRETE.defaultBlockState() : trim, false));
            for (int x = -1; x <= 1; x++) p.add(new Part(x, 1, z, deck, false));
            p.add(new Part(2, 2, z, fenceZ, false));
            p.add(new Part(-2, 2, z, fenceZ, false));
        }
        for (int x = -1; x <= 1; x++) {
            p.add(new Part(x, 1, 5, hull, false));
            p.add(new Part(x, 1, -5, hull, false));
            p.add(new Part(x, 2, -5, fenceX, false));
        }
        p.add(new Part(0, 1, 6, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), false));
        p.add(new Part(-1, 2, 5, fenceX, false));
        p.add(new Part(1, 2, 5, fenceX, false));
        p.add(new Part(0, 2, 5, post, false));
        p.add(new Part(0, 3, 5, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true), false));
        for (int z : new int[]{-4, -2, 0, 2}) {
            p.add(new Part(-1, 2, z, seat, false));
            p.add(new Part(1, 2, z, seat, false));
        }
        p.add(new Part(0, 2, 3, helmSeat, false));
        for (int sx : new int[]{-2, 2}) for (int sz : new int[]{-3, 3}) for (int y = 3; y <= 5; y++) p.add(new Part(sx, y, sz, post, false));
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false);
        p.add(new Part(2, 3, 4, lantern, true));
        p.add(new Part(-2, 3, 4, lantern, true));
        p.add(new Part(2, 3, -4, lantern, true));
        p.add(new Part(-2, 3, -4, lantern, true));
        double rx = 2.7, ry = 2.2, rz = 5.8;
        int cy = 8;
        for (int x = -3; x <= 3; x++) for (int y = cy - 3; y <= cy + 3; y++) for (int z = -6; z <= 6; z++) {
            if (!inside(x, y - cy, z, rx, ry, rz)) continue;
            boolean surface = !inside(x + 1, y - cy, z, rx, ry, rz) || !inside(x - 1, y - cy, z, rx, ry, rz) || !inside(x, y - cy + 1, z, rx, ry, rz)
                    || !inside(x, y - cy - 1, z, rx, ry, rz) || !inside(x, y - cy, z + 1, rx, ry, rz) || !inside(x, y - cy, z - 1, rx, ry, rz);
            if (!surface) continue;
            BlockState wool = y == cy ? Blocks.YELLOW_WOOL.defaultBlockState() : (Math.floorMod(z, 2) == 0 ? Blocks.ORANGE_WOOL : Blocks.WHITE_WOOL).defaultBlockState();
            p.add(new Part(x, y, z, wool, false));
        }
        for (int y = cy - 1; y <= cy + 2; y++) p.add(new Part(0, y, -7, Blocks.RED_WOOL.defaultBlockState(), false));
        p.add(new Part(1, cy, -7, Blocks.RED_WOOL.defaultBlockState(), false));
        p.add(new Part(-1, cy, -7, Blocks.RED_WOOL.defaultBlockState(), false));
        p.add(new Part(0, cy - 3, 0, Blocks.SHROOMLIGHT.defaultBlockState(), true));
        p.add(new Part(0, 1, -6, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), false));
        return p;
    }

    private static boolean inside(int x, int y, int z, double rx, double ry, double rz) {
        double a = x / rx, b = y / ry, c = z / rz;
        return a * a + b * b + c * c <= 1.0;
    }

    @Override
    public void render(Ferry e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        try {
            if (parts == null) parts = build();
            BlockRenderDispatcher br = Minecraft.getInstance().getBlockRenderer();
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            for (Part p : parts) {
                pose.pushPose();
                pose.translate(p.x() - 0.5, p.y(), p.z() - 0.5);
                br.renderSingleBlock(p.state(), pose, buffers, p.glow() ? 0xF000F0 : light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            float spin = (e.tickCount + pt) * 40f * e.thrust();
            pose.pushPose();
            pose.translate(0, 2.0, -6.6);
            pose.mulPose(Axis.ZP.rotationDegrees(spin));
            for (int i = 0; i < 2; i++) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(i * 90));
                pose.scale(2.4f, 0.28f, 0.12f);
                pose.translate(-0.5, -0.5, -0.5);
                br.renderSingleBlock(Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            pose.popPose();
            pose.popPose();
        } catch (Throwable t) {
            if (errors++ < 5) com.fireheart.city.FireheartCity.LOG.error("Ferry render failed", t);
        }
        super.render(e, yaw, pt, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Ferry e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
