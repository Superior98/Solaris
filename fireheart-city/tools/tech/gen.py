out = []
C = out.append
X0, X1, Z0, Z1 = 31, 39, -7, 5
C("kill @e[type=item_frame,x=31,y=70,z=-7,dx=8,dy=8,dz=12]")
C(f"fill {X0} 71 {Z0} {X1} 78 {Z1} air")
C(f"fill {X0} 70 {Z0} {X1} 70 {Z1} white_concrete")
for x in range(32, 39):
    for z in range(-6, 5):
        if (x + z) % 2 == 0:
            C(f"setblock {x} 70 {z} light_gray_concrete")
for x in range(32, 37):
    C(f"setblock {x} 70 -1 gray_concrete")
C(f"fill {X0} 71 {Z0} {X1} 75 {Z0} white_concrete")
C(f"fill {X0} 71 {Z1} {X1} 75 {Z1} white_concrete")
C(f"fill {X1} 71 {Z0} {X1} 75 {Z1} white_concrete")
C(f"fill {X0} 71 {Z0} {X0} 75 {Z1} white_concrete")
for x, z in ((X0, Z0), (X0, Z1), (X1, Z0), (X1, Z1)):
    C(f"fill {x} 71 {z} {x} 76 {z} black_concrete")
C(f"fill {X0} 72 -6 {X0} 74 -3 glass")
C(f"fill {X0} 72 1 {X0} 74 4 glass")
C(f"fill {X0} 71 -2 {X0} 73 0 air")
C(f"fill {X0} 75 -6 {X0} 75 4 orange_concrete")
C(f"fill {X0 + 1} 75 {Z0} {X1 - 1} 75 {Z0} orange_concrete")
C(f"fill {X0 + 1} 75 {Z1} {X1 - 1} 75 {Z1} orange_concrete")
C(f"fill {X0} 76 {Z0} {X1} 76 {Z1} white_concrete")
for x in (33, 35, 37):
    for z in (-4, -1, 2):
        C(f"setblock {x} 76 {z} sea_lantern")
C(f"fill {X0} 77 {Z0} {X1} 77 {Z0} smooth_stone_slab")
C(f"fill {X0} 77 {Z1} {X1} 77 {Z1} smooth_stone_slab")
C(f"fill {X0} 77 {Z0} {X0} 77 {Z1} smooth_stone_slab")
C(f"fill {X1} 77 {Z0} {X1} 77 {Z1} smooth_stone_slab")
def sign(x, y, z, facing, lines, glow=True, color="black", wood="dark_oak"):
    msgs = ",".join("'" + l + "'" for l in lines)
    C(f"setblock {x} {y} {z} {wood}_wall_sign[facing={facing}]{{front_text:{{has_glowing_text:{1 if glow else 0}b,color:\"{color}\",messages:[{msgs}]}}}}")
sign(30, 74, -1, "west", ['""', '{"text":"FIRETECH","bold":true,"color":"gold"}', '{"text":"PCs · Phones","color":"white"}', '""'], color="white")
sign(30, 74, -2, "west", ['""', '{"text":"★ NEW ★","color":"yellow"}', '{"text":"FirePhone","color":"white"}', '{"text":"40 coins","color":"gray"}'], color="white")
sign(30, 74, 0, "west", ['""', '{"text":"Fireheart PC","color":"white"}', '{"text":"90 coins","color":"gray"}', '""'], color="white")
C("fill 37 71 -3 37 71 1 smooth_quartz")
C("setblock 37 72 -1 fireheartcity:computer[facing=west]")
C("setblock 37 72 1 potted_cactus")
C("setblock 37 72 -3 lantern")
for x in (33, 35, 37):
    C(f"setblock {x} 71 -6 fireheartcity:computer[facing=south]")
    sign(x, 73, -6, "south", ['""', '{"text":"DEMO","bold":true,"color":"dark_red"}', '{"text":"Try FireOS!"}', '""'], glow=False, wood="birch")
for i, x in enumerate(range(32, 39)):
    C(f'summon item_frame {x}.5 73.5 4.97 {{Pos:[{x}.5d,73.5d,4.97d],TileX:{x},TileY:73,TileZ:4,Facing:2b,Fixed:1b,Invulnerable:1b,Item:{{id:"fireheartcity:phone",Count:1b,tag:{{Color:{i}}}}}}}')
sign(35, 72, 4, "north", ['{"text":"FirePhone","bold":true}', '{"text":"8 colours"}', '{"text":"40 coins","color":"dark_green"}', '""'], glow=False, wood="birch")
for x, col in ((33, 7), (37, 2)):
    C(f"setblock {x} 71 2 quartz_pillar")
    C(f'summon item_frame {x}.5 72.03 2.5 {{Pos:[{x}.5d,72.03d,2.5d],TileX:{x},TileY:72,TileZ:2,Facing:1b,Fixed:1b,Invulnerable:1b,Item:{{id:"fireheartcity:phone",Count:1b,tag:{{Color:{col}}}}}}}')
C("setblock 32 71 4 potted_bamboo")
C("setblock 38 71 4 potted_azalea_bush")
C("setblock 32 71 -6 potted_fern")
C("setblock 38 71 -6 potted_fern")
C('tellraw @a {"text":"[Fireheart City] FireTech just opened on the east side of the city, by the fuel station road!","color":"gold"}')
open('/tmp/modbuild/res/data/fireheartcity/functions/tech/build.mcfunction', 'w', encoding='utf-8').write("\n".join(out) + "\n")
print(len(out), "commands")
