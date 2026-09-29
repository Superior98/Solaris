scoreboard players set #wait sky 0
scoreboard players set #t sky 0
scoreboard players set #lock sky 1
execute if score #state sky matches 2 run scoreboard players set #state sky 3
execute if score #state sky matches 0 run scoreboard players set #state sky 1
title @a[tag=sky_rider] actionbar {"text":"Lift-off!","color":"light_purple","bold":true}
execute as @a at @s run playsound minecraft:block.beacon.activate master @s ~ ~ ~ 1 0.8
execute as @a[tag=sky_rider] at @s run playsound minecraft:entity.firework_rocket.launch master @s ~ ~ ~ 1 0.6
