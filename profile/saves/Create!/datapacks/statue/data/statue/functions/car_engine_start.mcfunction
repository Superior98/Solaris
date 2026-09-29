tag @s add car_engine_on
scoreboard players set @s car_rev 0
scoreboard players set @s car_gear -1
scoreboard players set @s car_timer 0
playsound minecraft:block.piston.extend block @s ~ ~ ~ 0.8 0.5
playsound minecraft:entity.iron_golem.repair block @s ~ ~ ~ 0.7 0.6
