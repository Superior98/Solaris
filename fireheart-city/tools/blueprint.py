import gzip
import io
import os
import struct
import sys
import zlib
import nbtlib
import numpy as np

REG = sys.argv[1] if len(sys.argv) > 1 else '/mnt/user-data/uploads/Create!/region/'
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(__file__), '..', 'res', 'data', 'fireheartcity', 'blueprint.bin')

AREAS = [(-110, -90, 145, 160, 55, 150), (-80, 200, 80, 355, 150, 260)]
EXCLUDE = [(40, 3, 145, 100), (-20, 94, 16, 148), (63, -53, 71, -40), (75, -53, 81, -48)]
NATURAL_EXACT = {
    'air', 'cave_air', 'void_air', 'water', 'lava', 'bubble_column', 'grass_block', 'dirt', 'coarse_dirt', 'podzol', 'rooted_dirt', 'mud', 'stone', 'deepslate',
    'granite', 'diorite', 'andesite', 'tuff', 'calcite', 'gravel', 'sand', 'red_sand', 'clay', 'bedrock', 'grass', 'tall_grass', 'fern', 'large_fern',
    'dead_bush', 'vine', 'snow', 'snow_block', 'ice', 'seagrass', 'tall_seagrass', 'kelp', 'kelp_plant', 'sugar_cane', 'bamboo', 'cactus', 'moss_block',
    'moss_carpet', 'glow_lichen', 'dripstone_block', 'pointed_dripstone', 'fire', 'soul_fire', 'cobweb', 'sweet_berry_bush', 'wheat', 'carrots', 'potatoes',
    'beetroots', 'pumpkin_stem', 'melon_stem', 'brown_mushroom', 'red_mushroom', 'lily_pad', 'hanging_roots', 'spore_blossom', 'big_dripleaf', 'big_dripleaf_stem',
    'small_dripleaf', 'azalea', 'flowering_azalea', 'mangrove_roots', 'muddy_mangrove_roots', 'sculk', 'sculk_vein', 'powder_snow', 'magma_block', 'obsidian',
    'dandelion', 'poppy', 'blue_orchid', 'allium', 'azure_bluet', 'red_tulip', 'orange_tulip', 'white_tulip', 'pink_tulip', 'oxeye_daisy', 'cornflower',
    'lily_of_the_valley', 'sunflower', 'lilac', 'rose_bush', 'peony', 'pink_petals', 'bee_nest', 'suspicious_sand', 'suspicious_gravel', 'sandstone',
    'amethyst_block', 'budding_amethyst', 'smooth_basalt', 'infested_stone', 'cocoa', 'glow_berries', 'cave_vines', 'cave_vines_plant', 'twisting_vines',
    'weeping_vines', 'seagrass', 'sea_pickle', 'light', 'barrier', 'structure_void', 'brain_coral', 'tube_coral', 'horn_coral', 'fire_coral', 'bubble_coral', 'scaffolding', 'farmland', 'dirt_path'}


SKIP_ANY = ('cake', 'delivery_box', 'pie', 'feast', 'roast_chicken', 'stuffed_pumpkin', 'honey_glazed_ham', 'shepherds_pie', 'rice_roll_medley', 'cheesecake', 'candle')


def natural(name):
    ns, n = name.split(':')
    if any(k in n for k in SKIP_ANY):
        return True
    if ns != 'minecraft':
        return False
    if n in NATURAL_EXACT:
        return True
    if n.endswith('_leaves') or n.endswith('_ore') or n.endswith('_sapling') or n.endswith('_coral') or n.endswith('_coral_block') or n.endswith('_coral_fan'):
        return True
    if n.endswith('_log') and not n.startswith('stripped_'):
        return True
    if n.startswith('infested_') or n.endswith('amethyst_bud') or n == 'amethyst_cluster':
        return True
    return False


DECO = set()
try:
    _d = nbtlib.load(os.path.join(os.path.dirname(REG.rstrip('/')), 'data', 'fireheartcity.dat'))['data']
    for v in _d.get('festivalDeco', []):
        v = int(v)
        DECO.add((v >> 38, (v << 52 >> 52) if False else ((v & 0xFFF) - (0x1000 if v & 0x800 else 0)), ((v >> 12) & 0x3FFFFFF) - (0x4000000 if (v >> 12) & 0x2000000 else 0)))
except Exception as e:
    print('no festival data', e)


def excluded(x, z):
    for (a, b, c, d) in EXCLUDE:
        if a <= x <= c and b <= z <= d:
            return True
    return False


def state_str(p):
    name = str(p['Name'])
    props = p.get('Properties')
    if props:
        return name + '[' + ','.join('%s=%s' % (k, str(v)) for k, v in sorted(props.items())) + ']'
    return name


regions = {}


def chunk_nbt(cx, cz):
    rx, rz = cx >> 5, cz >> 5
    key = (rx, rz)
    if key not in regions:
        p = os.path.join(REG, 'r.%d.%d.mca' % (rx, rz))
        regions[key] = open(p, 'rb').read() if os.path.exists(p) else None
    f = regions[key]
    if f is None:
        return None
    i = 4 * ((cx & 31) + (cz & 31) * 32)
    off = (f[i] << 16 | f[i + 1] << 8 | f[i + 2]) * 4096
    if off == 0:
        return None
    ln = struct.unpack('>I', f[off:off + 4])[0]
    comp = f[off + 4]
    data = f[off + 5:off + 4 + ln]
    raw = zlib.decompress(data) if comp == 2 else gzip.decompress(data)
    return nbtlib.File.parse(io.BytesIO(raw))


palette = {}
chunks = {}


def pid(s):
    if s not in palette:
        palette[s] = len(palette)
    return palette[s]


for (x1, z1, x2, z2, y1, y2) in AREAS:
    for cx in range(x1 >> 4, (x2 >> 4) + 1):
        for cz in range(z1 >> 4, (z2 >> 4) + 1):
            t = chunk_nbt(cx, cz)
            if t is None:
                continue
            for sec in t['sections']:
                sy = int(sec['Y'])
                if sy * 16 + 15 < y1 or sy * 16 > y2 or 'block_states' not in sec:
                    continue
                bs = sec['block_states']
                pal = bs['palette']
                names = [str(p['Name']) for p in pal]
                keep = [not natural(n) for n in names]
                if not any(keep):
                    continue
                if 'data' in bs:
                    bits = max(4, (len(pal) - 1).bit_length())
                    per = 64 // bits
                    longs = np.array([int(v) & ((1 << 64) - 1) for v in bs['data']], dtype=np.uint64)
                    idx = np.zeros(4096, dtype=np.int64)
                    mask = np.uint64((1 << bits) - 1)
                    for k in range(per):
                        vals = (longs >> np.uint64(k * bits)) & mask
                        pos = np.arange(len(longs)) * per + k
                        ok = pos < 4096
                        idx[pos[ok]] = vals[ok].astype(np.int64)
                else:
                    idx = np.zeros(4096, dtype=np.int64)
                keepa = np.array(keep)
                for n in np.nonzero(keepa[idx])[0]:
                    ly, lz, lx = n >> 8, (n >> 4) & 15, n & 15
                    wx, wy, wz = cx * 16 + lx, sy * 16 + ly, cz * 16 + lz
                    if not (x1 <= wx <= x2 and z1 <= wz <= z2 and y1 <= wy <= y2) or excluded(wx, wz) or (wx, wy, wz) in DECO:
                        continue
                    chunks.setdefault((cx, cz), []).append((lx, lz, wy, pid(state_str(pal[int(idx[n])]))))


def varint(v):
    out = bytearray()
    while True:
        b = v & 0x7F
        v >>= 7
        if v:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def zz(v):
    return (v << 1) ^ (v >> 31)


buf = bytearray(b'FHBP1')
pl = sorted(palette.items(), key=lambda kv: kv[1])
buf += varint(len(pl))
for s, _ in pl:
    e = s.encode()
    buf += varint(len(e)) + e
buf += varint(len(chunks))
total = 0
for (cx, cz), es in sorted(chunks.items()):
    buf += varint(zz(cx)) + varint(zz(cz)) + varint(len(es))
    for lx, lz, y, p in es:
        buf += bytes([(lx << 4) | lz]) + varint(zz(y)) + varint(p)
    total += len(es)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with gzip.open(OUT, 'wb') as g:
    g.write(bytes(buf))
print('palette', len(pl), 'chunks', len(chunks), 'blocks', total, 'bytes', os.path.getsize(OUT))
