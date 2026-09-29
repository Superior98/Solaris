execute as @a at @s on vehicle run tellraw @a ["SEATDBG ",{"entity":"@s","nbt":"Pos"}]
execute as @a on vehicle at @s if block ~ ~-1 ~ betterblockz:reactor_7_blockz_13 run tellraw @a "SEATDBG marker-1 ok"
execute as @a on vehicle at @s if block ~ ~-0.6 ~ betterblockz:reactor_7_blockz_13 run tellraw @a "SEATDBG marker-0.6 ok"
