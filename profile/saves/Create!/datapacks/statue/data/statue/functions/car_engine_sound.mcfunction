execute unless entity @s[tag=car_engine_on] run function statue:car_engine_start
execute if block -28669952 127 12290047 create:clutch[powered=false] if score @s car_rev matches ..29 run scoreboard players add @s car_rev 1
execute if block -28669952 127 12290047 create:clutch[powered=true] if score @s car_rev matches 1.. run scoreboard players remove @s car_rev 1
scoreboard players set @s car_newgear 0
execute if score @s car_rev matches 5..9 run scoreboard players set @s car_newgear 1
execute if score @s car_rev matches 10..14 run scoreboard players set @s car_newgear 2
execute if score @s car_rev matches 15..19 run scoreboard players set @s car_newgear 3
execute if score @s car_rev matches 20..24 run scoreboard players set @s car_newgear 4
execute if score @s car_rev matches 25..29 run scoreboard players set @s car_newgear 5
execute if score @s car_rev matches 30.. run scoreboard players set @s car_newgear 6
execute unless score @s car_newgear = @s car_gear run function statue:car_engine_shift
scoreboard players remove @s car_timer 1
execute if score @s car_timer matches ..0 run function statue:car_engine_play
