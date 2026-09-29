stopsound @s * fireheart:car_engine
stopsound @s * createdieselgenerators:engine_normal
playsound minecraft:block.iron_trapdoor.close block @s ~ ~ ~ 0.6 0.6
tag @s remove car_engine_on
scoreboard players set @s car_rev 0
scoreboard players set @s car_gear -1
