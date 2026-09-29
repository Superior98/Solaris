function skyx:terminal
function skyx:city_pad
function skyx:gardens
function skyx:observatory
execute if score #c0 skyx matches 1 run forceload remove 0 224
execute if score #c1 skyx matches 1 run forceload remove 0 240
execute if score #c2 skyx matches 1 run forceload remove 16 224
execute if score #c3 skyx matches 1 run forceload remove 16 240
execute if score #c4 skyx matches 1 run forceload remove 32 224
execute if score #c5 skyx matches 1 run forceload remove 32 240
execute if score #c6 skyx matches 1 run forceload remove -80 240
execute if score #c7 skyx matches 1 run forceload remove -80 256
execute if score #c8 skyx matches 1 run forceload remove -80 272
execute if score #c9 skyx matches 1 run forceload remove -64 240
execute if score #c10 skyx matches 1 run forceload remove -64 256
execute if score #c11 skyx matches 1 run forceload remove -64 272
execute if score #c12 skyx matches 1 run forceload remove -48 240
execute if score #c13 skyx matches 1 run forceload remove -48 256
execute if score #c14 skyx matches 1 run forceload remove -48 272
execute if score #c15 skyx matches 1 run forceload remove -32 240
execute if score #c16 skyx matches 1 run forceload remove -32 256
execute if score #c17 skyx matches 1 run forceload remove -32 272
execute if score #c18 skyx matches 1 run forceload remove 32 288
execute if score #c19 skyx matches 1 run forceload remove 32 304
execute if score #c20 skyx matches 1 run forceload remove 32 320
execute if score #c21 skyx matches 1 run forceload remove 48 288
execute if score #c22 skyx matches 1 run forceload remove 48 304
execute if score #c23 skyx matches 1 run forceload remove 48 320
execute if score #c24 skyx matches 1 run forceload remove 0 64
execute if score #c25 skyx matches 1 run forceload remove 0 80
execute if score #c26 skyx matches 1 run forceload remove 16 64
execute if score #c27 skyx matches 1 run forceload remove 16 80
tellraw @a ["",{"text":"[Fireheart] ","color":"gold"},{"text":"Neon Heights expansion built: Sky Ferry terminal, city ferry pad, Sky Gardens and the Observatory.","color":"aqua"}]
