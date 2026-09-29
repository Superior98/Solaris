scoreboard players add #wait sky 1
execute if score #wait sky matches 1 run title @a[tag=sky_rider] times 10 50 20
execute if score #wait sky matches 1 run title @a[tag=sky_rider] subtitle {"text":"Next stop: NEON HEIGHTS","color":"aqua"}
execute if score #wait sky matches 1 if score #state sky matches 2 run title @a[tag=sky_rider] subtitle {"text":"Next stop: FIREHEART CITY","color":"gold"}
execute if score #wait sky matches 1 run title @a[tag=sky_rider] title {"text":"NEON SKYLINER","color":"light_purple","bold":true}
execute if score #wait sky matches 20 run title @a[tag=sky_rider] actionbar {"text":"Departing in 3...","color":"aqua"}
execute if score #wait sky matches 40 run title @a[tag=sky_rider] actionbar {"text":"Departing in 2...","color":"aqua"}
execute if score #wait sky matches 60 run title @a[tag=sky_rider] actionbar {"text":"Departing in 1...","color":"aqua"}
execute if score #wait sky matches 20 as @a[tag=sky_rider] at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1
execute if score #wait sky matches 40 as @a[tag=sky_rider] at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1
execute if score #wait sky matches 60 as @a[tag=sky_rider] at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1
execute if score #wait sky matches 80.. run function sky:depart
