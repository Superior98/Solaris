scoreboard objectives add skyx dummy
execute store success score #c0 skyx run forceload add 0 224
execute store success score #c1 skyx run forceload add 0 240
execute store success score #c2 skyx run forceload add 16 224
execute store success score #c3 skyx run forceload add 16 240
execute store success score #c4 skyx run forceload add 32 224
execute store success score #c5 skyx run forceload add 32 240
execute store success score #c6 skyx run forceload add -80 240
execute store success score #c7 skyx run forceload add -80 256
execute store success score #c8 skyx run forceload add -80 272
execute store success score #c9 skyx run forceload add -64 240
execute store success score #c10 skyx run forceload add -64 256
execute store success score #c11 skyx run forceload add -64 272
execute store success score #c12 skyx run forceload add -48 240
execute store success score #c13 skyx run forceload add -48 256
execute store success score #c14 skyx run forceload add -48 272
execute store success score #c15 skyx run forceload add -32 240
execute store success score #c16 skyx run forceload add -32 256
execute store success score #c17 skyx run forceload add -32 272
execute store success score #c18 skyx run forceload add 32 288
execute store success score #c19 skyx run forceload add 32 304
execute store success score #c20 skyx run forceload add 32 320
execute store success score #c21 skyx run forceload add 48 288
execute store success score #c22 skyx run forceload add 48 304
execute store success score #c23 skyx run forceload add 48 320
execute store success score #c24 skyx run forceload add 0 64
execute store success score #c25 skyx run forceload add 0 80
execute store success score #c26 skyx run forceload add 16 64
execute store success score #c27 skyx run forceload add 16 80
tellraw @a ["",{"text":"[Fireheart] ","color":"gold"},{"text":"Loading the building sites... (3 seconds)","color":"gray"}]
schedule function skyx:build2 60t
