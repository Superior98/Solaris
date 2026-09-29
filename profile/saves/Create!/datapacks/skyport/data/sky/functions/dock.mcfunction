function sky:riders
execute unless entity @a[tag=sky_rider] run scoreboard players set #wait sky 0
execute unless entity @a[tag=sky_rider] run scoreboard players set #lock sky 0
execute if score #state sky matches 0 if block -9 71 77 minecraft:polished_blackstone_button[powered=true] run title @a actionbar {"text":"The Skyliner is already here - take a seat!","color":"aqua"}
execute if score #state sky matches 2 if block -10 181 245 minecraft:polished_blackstone_button[powered=true] run title @a actionbar {"text":"The Skyliner is already here - take a seat!","color":"aqua"}
execute if score #state sky matches 2 if block -9 71 77 minecraft:polished_blackstone_button[powered=true] unless entity @a[tag=sky_rider] run function sky:call
execute if score #state sky matches 0 if block -10 181 245 minecraft:polished_blackstone_button[powered=true] unless entity @a[tag=sky_rider] run function sky:call
execute if entity @a[tag=sky_rider] if score #lock sky matches 0 run function sky:countdown
