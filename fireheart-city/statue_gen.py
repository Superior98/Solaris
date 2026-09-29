import sys, os, math
from PIL import Image

SKIN = sys.argv[1]
OUT = sys.argv[2]
S = 3
X0, Y0, ZF = -26, 77, -65

PAL = {
    "white_concrete": (207, 213, 214), "orange_concrete": (224, 97, 1), "magenta_concrete": (169, 48, 159), "light_blue_concrete": (36, 137, 199),
    "yellow_concrete": (241, 175, 21), "lime_concrete": (94, 169, 24), "pink_concrete": (214, 101, 143), "gray_concrete": (55, 58, 62),
    "light_gray_concrete": (125, 125, 115), "cyan_concrete": (21, 119, 136), "purple_concrete": (100, 32, 156), "blue_concrete": (45, 47, 143),
    "brown_concrete": (96, 60, 32), "green_concrete": (73, 91, 36), "red_concrete": (142, 33, 33), "black_concrete": (8, 10, 15),
    "white_wool": (234, 236, 237), "orange_wool": (241, 118, 20), "magenta_wool": (190, 69, 180), "light_blue_wool": (58, 175, 217),
    "yellow_wool": (249, 198, 40), "lime_wool": (112, 185, 26), "pink_wool": (238, 141, 172), "gray_wool": (63, 68, 72),
    "light_gray_wool": (142, 142, 135), "cyan_wool": (21, 138, 145), "purple_wool": (122, 42, 173), "blue_wool": (53, 57, 157),
    "brown_wool": (114, 72, 41), "green_wool": (85, 110, 28), "red_wool": (161, 39, 35), "black_wool": (21, 21, 26),
    "terracotta": (152, 94, 68), "white_terracotta": (210, 178, 161), "orange_terracotta": (162, 84, 38), "magenta_terracotta": (150, 88, 109),
    "light_blue_terracotta": (113, 109, 138), "yellow_terracotta": (186, 133, 35), "lime_terracotta": (104, 118, 53), "pink_terracotta": (162, 78, 79),
    "gray_terracotta": (58, 42, 36), "light_gray_terracotta": (135, 107, 98), "cyan_terracotta": (87, 91, 91), "purple_terracotta": (118, 70, 86),
    "blue_terracotta": (74, 60, 91), "brown_terracotta": (77, 51, 36), "green_terracotta": (76, 83, 42), "red_terracotta": (143, 61, 47),
    "black_terracotta": (37, 23, 16),
    "quartz_block": (236, 230, 223), "smooth_stone": (159, 159, 159), "stone": (126, 126, 126), "andesite": (136, 136, 137),
    "diorite": (189, 188, 189), "granite": (149, 103, 86), "deepslate": (80, 80, 82), "polished_deepslate": (72, 72, 73),
    "deepslate_tiles": (54, 54, 55), "deepslate_bricks": (70, 70, 71), "blackstone": (42, 36, 41), "polished_blackstone": (53, 49, 57),
    "bricks": (150, 97, 83), "nether_bricks": (44, 21, 26), "red_nether_bricks": (69, 7, 9), "sandstone": (216, 203, 155),
    "smooth_sandstone": (223, 214, 170), "oak_planks": (162, 131, 79), "spruce_planks": (115, 85, 49), "birch_planks": (192, 175, 121),
    "dark_oak_planks": (67, 43, 20), "cherry_planks": (226, 178, 172), "crimson_planks": (101, 49, 71), "warped_planks": (43, 105, 99),
    "snow_block": (249, 254, 254), "bone_block": (229, 226, 208), "calcite": (223, 224, 220), "tuff": (108, 109, 102),
    "mud_bricks": (137, 103, 79), "packed_mud": (142, 106, 80), "prismarine": (99, 156, 151), "dark_prismarine": (51, 91, 75),
    "clay": (160, 166, 179), "mushroom_stem": (203, 196, 185), "lapis_block": (30, 67, 140), "blue_ice": (116, 167, 253),
    "packed_ice": (141, 180, 250), "sculk": (12, 29, 36), "crying_obsidian": (32, 10, 60), "obsidian": (15, 10, 24),
    "coal_block": (16, 15, 15), "purpur_block": (169, 125, 169), "amethyst_block": (133, 97, 191), "iron_block": (220, 220, 220),
    "gold_block": (246, 208, 61), "mud": (60, 57, 60), "soul_soil": (75, 57, 46), "smooth_quartz": (235, 229, 222),
}

def lab(c):
    def f(u):
        u /= 255
        return ((u + 0.055) / 1.055) ** 2.4 if u > 0.04045 else u / 12.92
    r, g, b = f(c[0]), f(c[1]), f(c[2])
    x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
    y = r * 0.2126 + g * 0.7152 + b * 0.0722
    z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883
    def h(t):
        return t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    return (116 * h(y) - 16, 500 * (h(x) - h(y)), 200 * (h(y) - h(z)))

PL = {k: lab(v) for k, v in PAL.items()}
cache = {}

def block(c):
    c = (c[0] // 4 * 4, c[1] // 4 * 4, c[2] // 4 * 4)
    if c in cache:
        return cache[c]
    l = lab(c)
    best = min(PL, key=lambda k: (PL[k][0] - l[0]) ** 2 + (PL[k][1] - l[1]) ** 2 + (PL[k][2] - l[2]) ** 2)
    cache[c] = best
    return best

im = Image.open(SKIN).convert("RGBA")
HD = im.size[0] // 64

def px(u, v):
    return im.getpixel((u, v))

def sample(region, ov, iu, iv):
    U0, V0, tw, th = region
    u = U0 * HD + min(tw * HD - 1, int((iu + 0.5) * HD / S))
    v = V0 * HD + min(th * HD - 1, int((iv + 0.5) * HD / S))
    c = px(u, v)
    if ov is not None:
        o = px(u + ov[0] * HD, v + ov[1] * HD)
        if o[3] >= 128:
            c = o
    if c[3] < 128:
        return None
    return c

def faces(u0, v0, w, h, d):
    return {"top": (u0 + d, v0, w, d), "bottom": (u0 + d + w, v0, w, d), "right": (u0, v0 + d, d, h), "front": (u0 + d, v0 + d, w, h), "left": (u0 + d + w, v0 + d, d, h), "back": (u0 + d + w + d, v0 + d, w, h)}

world = {}

def box(bx, by, bz, w, h, d, uv, ov):
    f = faces(uv[0], uv[1], w, h, d)
    W, H, D = w * S, h * S, d * S
    for ix in range(W):
        for iy in range(H):
            for iz in range(D):
                on = []
                if iz == D - 1: on.append(("front", ix, H - 1 - iy))
                if ix == 0: on.append(("right", iz, H - 1 - iy))
                if ix == W - 1: on.append(("left", D - 1 - iz, H - 1 - iy))
                if iz == 0: on.append(("back", W - 1 - ix, H - 1 - iy))
                if iy == H - 1: on.append(("top", ix, iz))
                if iy == 0: on.append(("bottom", ix, D - 1 - iz))
                pos = (bx + ix, by + iy, bz + iz)
                if not on:
                    world[pos] = "stone"
                    continue
                name, u, v = on[0]
                c = sample(f[name], ov, u, v)
                if c is None:
                    c = sample(f[name], None, u, v) or (20, 20, 30, 255)
                world[pos] = block(c)

BZ = ZF - 17
box(X0 + 4 * S, Y0 + 24 * S, ZF - 23, 8, 8, 8, (0, 0), (32, 0))
box(X0 + 4 * S, Y0 + 12 * S, BZ, 8, 12, 4, (16, 16), (0, 16))
box(X0, Y0 + 12 * S, BZ, 4, 12, 4, (40, 16), (0, 16))
box(X0 + 12 * S, Y0 + 12 * S, BZ, 4, 12, 4, (32, 48), (16, 0))
box(X0 + 4 * S, Y0, BZ, 4, 12, 4, (0, 16), (0, 16))
box(X0 + 8 * S, Y0, BZ, 4, 12, 4, (16, 48), (-16, 0))

os.makedirs(OUT, exist_ok=True)
lines = []
for y in range(Y0, Y0 + 96, 4):
    lines.append(f"fill {X0} {y} {ZF - 23} {X0 + 47} {min(y + 3, Y0 + 95)} {ZF} minecraft:air")
keys = sorted(world, key=lambda p: (p[1], p[2], p[0]))
i = 0
while i < len(keys):
    x, y, z = keys[i]
    b = world[keys[i]]
    j = i
    while j + 1 < len(keys) and keys[j + 1] == (keys[j][0] + 1, y, z) and world[keys[j + 1]] == b:
        j += 1
    x2 = keys[j][0]
    lines.append(f"fill {x} {y} {z} {x2} {y} {z} minecraft:{b}" if x2 > x else f"setblock {x} {y} {z} minecraft:{b}")
    i = j + 1
CH = 6000
n = 0
for k in range(0, len(lines), CH):
    n += 1
    open(os.path.join(OUT, f"stellar{n}.mcfunction"), "w").write("\n".join(lines[k:k + CH]) + "\n")
print(len(world), "blocks", len(lines), "commands", n, "files")

prev = Image.new("RGB", (48, 96), (135, 206, 235))
for (x, y, z), b in world.items():
    X, Y = x - X0, Y0 + 95 - y
    if z == max(zz for (xx, yy, zz) in [(x, y, z)]):
        pass
front = {}
for (x, y, z), b in world.items():
    k = (x, y)
    if k not in front or z > front[k][0]:
        front[k] = (z, b)
for (x, y), (z, b) in front.items():
    prev.putpixel((x - X0, Y0 + 95 - y), PAL[b])
back = Image.new("RGB", (48, 96), (135, 206, 235))
bk = {}
for (x, y, z), b in world.items():
    k = (x, y)
    if k not in bk or z < bk[k][0]:
        bk[k] = (z, b)
for (x, y), (z, b) in bk.items():
    back.putpixel((X0 + 47 - x, Y0 + 95 - y), PAL[b])
out = Image.new("RGB", (100, 96), (255, 255, 255))
out.paste(prev, (0, 0))
out.paste(back, (52, 0))
out.resize((500, 480), Image.NEAREST).save(os.path.join(OUT, "preview.png"))
