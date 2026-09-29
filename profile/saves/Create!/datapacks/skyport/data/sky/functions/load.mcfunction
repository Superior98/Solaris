scoreboard objectives add sky dummy
execute unless score #state sky matches 0.. run scoreboard players set #state sky 0
execute unless score #lock sky matches 0.. run scoreboard players set #lock sky 0
scoreboard players set #wait sky 0
execute if score #state sky matches 1 run scoreboard players set #state sky 0
execute if score #state sky matches 3 run scoreboard players set #state sky 2
forceload add -11 77 5 95
forceload add -13 226 7 246
