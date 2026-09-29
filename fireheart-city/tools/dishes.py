import json
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'res', 'assets', 'fireheartcity')
random.seed(11)
CELLS = {}
img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
px = img.load()


def cell(name, base, fn=None):
    i = len(CELLS)
    CELLS[name] = i
    cx, cy = (i % 8) * 8, (i // 8) * 8
    for y in range(8):
        for x in range(8):
            c = fn(x, y) if fn else None
            if c is None:
                j = random.randint(-8, 8)
                c = tuple(max(0, min(255, v + j)) for v in base)
            px[cx + x, cy + y] = c + (255,) if len(c) == 3 else c


def dots(base, dot, every=5):
    return lambda x, y: dot if (x * 3 + y * 5) % every == 0 else None


def stripes(base, line, vertical=False):
    return lambda x, y: line if ((x if vertical else y) % 3 == 0) else None


cell('plate', (240, 240, 236))
cell('rim', (205, 208, 212))
cell('bowl', (38, 70, 130), lambda x, y: (230, 230, 230) if y in (2, 5) and x % 2 == 0 else None)
cell('bowl_in', (238, 232, 214))
cell('bun', (201, 132, 58))
cell('bun_top', (212, 144, 62), dots((212, 144, 62), (250, 240, 200), 7))
cell('patty', (92, 52, 32), dots((92, 52, 32), (60, 32, 20), 3))
cell('cheese', (250, 200, 50))
cell('lettuce', (96, 176, 64), lambda x, y: (140, 210, 90) if (x + y) % 4 == 0 else None)
cell('tomato', (214, 48, 40), lambda x, y: (250, 200, 120) if (x - 3.5) ** 2 + (y - 3.5) ** 2 < 2 else None)
cell('steak', (120, 62, 36), lambda x, y: (58, 30, 18) if (x + y) % 4 == 0 else None)
cell('potato_skin', (160, 110, 60))
cell('potato_in', (245, 225, 150))
cell('butter', (255, 225, 110))
cell('beans', (70, 150, 60), stripes((70, 150, 60), (50, 120, 45)))
cell('broth', (205, 135, 50), lambda x, y: (235, 180, 90) if (x * y) % 7 == 1 else None)
cell('noodles', (244, 222, 150), stripes((244, 222, 150), (225, 195, 110)))
cell('egg_white', (250, 250, 245))
cell('yolk', (255, 150, 30))
cell('chopstick', (200, 160, 110))
cell('rice', (245, 243, 235), dots((245, 243, 235), (220, 215, 200), 4))
cell('peas', (110, 190, 70))
cell('carrot', (240, 130, 40))
cell('stew', (130, 80, 45), dots((130, 80, 45), (170, 110, 60), 5))
cell('mushroom', (200, 170, 130))
cell('bamboo', (205, 175, 100), stripes((205, 175, 100), (170, 140, 70), True))
cell('dumpling', (245, 238, 220), lambda x, y: (225, 215, 195) if x in (2, 5) else None)
cell('crust', (150, 90, 40))
cell('loaf_top', (205, 140, 60), lambda x, y: (240, 200, 140) if y == x or y == x - 3 else None)
cell('cookie', (205, 150, 80), dots((205, 150, 80), (70, 40, 25), 4))
cell('pie_crust', (225, 170, 90))
cell('pumpkin', (230, 120, 30))
cell('cream', (255, 252, 245))
cell('apple', (240, 215, 130), dots((240, 215, 130), (200, 150, 70), 6))
cell('sponge', (235, 205, 150))
cell('frosting', (250, 170, 200))
cell('cherry', (200, 20, 40))
cell('candle', (80, 140, 240), stripes((80, 140, 240), (250, 250, 250), True))
cell('flame', (255, 210, 60))
cell('chives', (80, 170, 70))
cell('nori', (30, 50, 35))
cell('sauce', (110, 50, 20))
cell('cabbage', (170, 220, 120))
cell('lid', (170, 140, 80), stripes((170, 140, 80), (140, 110, 60)))
cell('choc', (80, 45, 25))
cell('parsley', (60, 150, 50))
cell('onion', (240, 225, 235))
cell('steam_dummy', (255, 255, 255))

os.makedirs(os.path.join(ROOT, 'textures', 'item'), exist_ok=True)
img.save(os.path.join(ROOT, 'textures', 'item', 'dish_atlas.png'))


def uv(name):
    i = CELLS[name]
    cx, cy = (i % 8) * 2, (i // 8) * 2
    return [cx + 0.1, cy + 0.1, cx + 1.9, cy + 1.9]


def box(f, t, name, top=None, rot=None):
    faces = {}
    for d in ('north', 'south', 'east', 'west', 'down'):
        faces[d] = {'texture': '#a', 'uv': uv(name)}
    faces['up'] = {'texture': '#a', 'uv': uv(top or name)}
    e = {'from': f, 'to': t, 'faces': faces}
    if rot:
        e['rotation'] = rot
    return e


def plate(y=0):
    return [box([2, y, 2], [14, y + 0.5, 14], 'plate'), box([1.5, y + 0.5, 1.5], [14.5, y + 1, 2.5], 'rim'), box([1.5, y + 0.5, 13.5], [14.5, y + 1, 14.5], 'rim'),
            box([1.5, y + 0.5, 2.5], [2.5, y + 1, 13.5], 'rim'), box([13.5, y + 0.5, 2.5], [14.5, y + 1, 13.5], 'rim')]


def bowl(inner, fill_y=4.5):
    return [box([4, 0, 4], [12, 1, 12], 'bowl'), box([3, 1, 3], [13, 5, 4], 'bowl'), box([3, 1, 12], [13, 5, 13], 'bowl'),
            box([3, 1, 4], [4, 5, 12], 'bowl'), box([12, 1, 4], [13, 5, 12], 'bowl'), box([4, 1, 4], [12, fill_y, 12], inner)]


def r(axis, angle, origin=(8, 4, 8)):
    return {'angle': angle, 'axis': axis, 'origin': list(origin)}


D = {}
D['steak'] = plate() + [box([4, 1, 5], [11, 2.5, 11], 'steak'), box([11.5, 1, 4], [13, 1.8, 6], 'parsley'), box([3, 1, 11.5], [5, 1.6, 13], 'tomato'),
                        box([11, 1, 10], [13, 1.8, 12.5], 'potato_in')]
D['baked_potato'] = plate() + [box([4, 1, 5], [12, 5, 11], 'potato_skin', 'potato_in'), box([6, 5, 6.5], [10, 5.6, 9.5], 'butter'), box([6.5, 5, 5.5], [7.5, 5.4, 10.5], 'chives'),
                               box([8.5, 5, 5.5], [9.5, 5.4, 10.5], 'chives')]
D['burger'] = plate() + [box([4, 1, 4], [12, 2.5, 12], 'bun'), box([4.5, 2.5, 4.5], [11.5, 3.2, 11.5], 'lettuce'), box([3.8, 3.2, 3.8], [12.2, 4.8, 12.2], 'patty'),
                         box([4, 4.8, 4], [12, 5.3, 12], 'cheese'), box([4.5, 5.3, 4.5], [11.5, 5.9, 11.5], 'tomato'), box([3.8, 5.9, 3.8], [12.2, 8.3, 12.2], 'bun', 'bun_top'),
                         box([5, 8.3, 5], [11, 9, 11], 'bun_top'), box([7.8, 8, 7.8], [8.2, 11, 8.2], 'chopstick')]
D['steak_potatoes'] = plate() + [box([3, 1, 4], [9, 2.4, 10], 'steak'), box([9.5, 1, 4], [12, 3, 6.5], 'potato_in'), box([10, 1, 7], [12.5, 3, 9.5], 'potato_in'),
                                 box([9.5, 1, 10], [12, 3, 12.5], 'potato_in'), box([3.5, 1, 10.5], [9, 1.8, 11.5], 'beans'), box([3.5, 1, 12], [9, 1.8, 13], 'beans'),
                                 box([6, 2.4, 6], [7, 2.8, 8], 'butter')]
D['mushroom_stew'] = bowl('stew') + [box([5, 4.5, 5], [7, 5.3, 7], 'mushroom'), box([9, 4.5, 8], [11, 5.3, 10], 'mushroom'), box([6, 4.5, 9], [7.5, 5, 10.5], 'parsley')]
D['ramen'] = bowl('broth') + [box([4.5, 4.5, 5], [11.5, 4.9, 6], 'noodles'), box([4.5, 4.5, 7], [11.5, 4.9, 8], 'noodles'), box([4.5, 4.5, 9], [11.5, 4.9, 10], 'noodles'),
                              box([5, 4.6, 10.5], [7.5, 5.6, 11.8], 'egg_white', 'yolk'), box([8, 4.6, 10.5], [10.5, 5.6, 11.8], 'egg_white', 'yolk'),
                              box([10.5, 4.5, 4.2], [11.8, 7.5, 5.2], 'nori'), box([5, 4.8, 4.5], [6.5, 5.2, 6], 'chives'),
                              box([2, 5.5, 7], [15, 6, 7.5], 'chopstick', rot=r('z', 22.5, (8, 5.7, 7.2))), box([2, 5.5, 8.5], [15, 6, 9], 'chopstick', rot=r('z', 22.5, (8, 5.7, 8.7)))]
D['veg_noodles'] = plate() + [box([4, 1, 4], [12, 2.5, 12], 'noodles'), box([5, 2.5, 5], [11, 3.5, 11], 'noodles'), box([5.5, 3, 6], [7, 3.8, 7.5], 'carrot'),
                              box([8.5, 3, 5.5], [10, 3.8, 7], 'cabbage'), box([6.5, 3, 8.5], [8, 3.8, 10], 'peas'), box([9, 3, 9], [10.5, 3.8, 10.5], 'carrot'),
                              box([3, 3.5, 7.5], [13, 4, 8], 'chopstick', rot=r('y', 22.5, (8, 3.7, 7.7)))]
D['fried_rice'] = bowl('rice', 4.2) + [box([5, 4.2, 5], [11, 6, 11], 'rice'), box([6, 6, 6], [10, 6.8, 10], 'rice'), box([5.5, 5.5, 6], [6.5, 6.4, 7], 'peas'),
                                       box([8, 6.3, 7], [9, 7, 8], 'carrot'), box([9.5, 5.6, 9], [10.5, 6.4, 10], 'peas'), box([6.8, 6.4, 8.5], [8.2, 7, 9.8], 'yolk'),
                                       box([7, 5.8, 10.4], [8, 6.6, 11.2], 'carrot')]
D['dumplings'] = [box([2, 0, 2], [14, 3, 14], 'bamboo'), box([2.5, 3, 2.5], [13.5, 3.2, 13.5], 'cabbage')] + [
    box([x, 3.2, z], [x + 3.4, 5.2, z + 3.4], 'dumpling') for x, z in ((3.5, 3.5), (9, 3.5), (3.5, 9), (9, 9))] + [
    box([x + 0.8, 5.2, z + 1.3], [x + 2.6, 5.9, z + 2.1], 'dumpling') for x, z in ((3.5, 3.5), (9, 3.5), (3.5, 9), (9, 9))] + [
    box([13, 0, 5], [15.5, 1, 7.5], 'sauce')]
D['bread'] = [box([2, 0, 3], [14, 0.6, 13], 'lid'), box([3, 0.6, 5], [13, 4.5, 11], 'crust', 'loaf_top'), box([4, 4.5, 6], [12, 5.5, 10], 'loaf_top')]
D['cookies'] = plate() + [box([4, 1, 4], [9, 1.8, 9], 'cookie'), box([7, 1.8, 6], [12, 2.6, 11], 'cookie'), box([5, 2.6, 7], [10, 3.4, 12], 'cookie'),
                          box([10, 1, 3], [12.5, 3.5, 5.5], 'cream')]
D['pumpkin_pie'] = plate() + [box([3, 1, 3], [13, 3.5, 13], 'pie_crust', 'pumpkin'), box([3, 3.5, 3], [13, 4, 4], 'pie_crust'), box([3, 3.5, 12], [13, 4, 13], 'pie_crust'),
                              box([3, 3.5, 4], [4, 4, 12], 'pie_crust'), box([12, 3.5, 4], [13, 4, 12], 'pie_crust'), box([7, 3.5, 7], [9, 4.8, 9], 'cream')]
D['apple_pie'] = plate() + [box([3, 1, 3], [13, 3.5, 13], 'pie_crust', 'apple')] + [box([3, 3.5, z], [13, 4, z + 1], 'pie_crust') for z in (4, 7, 10)] + [
    box([x, 3.5, 3], [x + 1, 4.1, 13], 'pie_crust') for x in (4.5, 7.5, 10.5)]
D['cake'] = plate() + [box([3, 1, 3], [13, 4, 13], 'sponge'), box([3, 4, 3], [13, 4.6, 13], 'frosting'), box([3.5, 4.6, 3.5], [12.5, 7, 12.5], 'sponge'),
                       box([3.5, 7, 3.5], [12.5, 7.8, 12.5], 'frosting'), box([7.4, 7.8, 7.4], [8.6, 10, 8.6], 'candle'), box([7.6, 10, 7.6], [8.4, 11, 8.4], 'flame'),
                       box([4, 7.8, 4], [5.2, 8.8, 5.2], 'cherry'), box([10.8, 7.8, 10.8], [12, 8.8, 12], 'cherry'), box([10.8, 7.8, 4], [12, 8.8, 5.2], 'cherry'),
                       box([4, 7.8, 10.8], [5.2, 8.8, 12], 'cherry'), box([3, 3.6, 3], [13, 4.1, 3.4], 'cream'), box([3, 3.6, 12.6], [13, 4.1, 13], 'cream')]

ORDER = ['steak', 'baked_potato', 'burger', 'steak_potatoes', 'mushroom_stew', 'ramen', 'veg_noodles', 'fried_rice', 'dumplings', 'bread', 'cookies', 'pumpkin_pie', 'apple_pie', 'cake']
DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 1.5, 0], 'scale': [0.8, 0.8, 0.8]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [-90, 0, 0], 'translation': [0, 0, -3], 'scale': [0.7, 0.7, 0.7]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 14, 0], 'scale': [0.9, 0.9, 0.9]},
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 2.5], 'scale': [0.45, 0.45, 0.45]},
    'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 2.5], 'scale': [0.45, 0.45, 0.45]},
    'firstperson_righthand': {'rotation': [10, -30, 0], 'translation': [1, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'firstperson_lefthand': {'rotation': [10, -30, 0], 'translation': [1, 3, 0], 'scale': [0.5, 0.5, 0.5]}}
md = os.path.join(ROOT, 'models', 'item')
for k in ORDER:
    json.dump({'textures': {'a': 'fireheartcity:item/dish_atlas', 'particle': 'fireheartcity:item/dish_atlas'}, 'elements': D[k], 'display': DISPLAY, 'gui_light': 'side'},
              open(os.path.join(md, 'dish_' + k + '.json'), 'w'), indent=1)
over = [{'predicate': {'fireheartcity:dish': i}, 'model': 'fireheartcity:item/dish_' + k} for i, k in enumerate(ORDER) if i > 0]
json.dump({'parent': 'fireheartcity:item/dish_' + ORDER[0], 'overrides': over}, open(os.path.join(md, 'dish.json'), 'w'), indent=1)
print(len(CELLS), 'cells;', len(ORDER), 'dishes')
