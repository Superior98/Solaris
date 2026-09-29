setblock -28661765 125 12290046 minecraft:polished_blackstone_slab[type=top]
setblock -28661765 126 12290046 minecraft:comparator[facing=north,mode=subtract]
setblock -28661765 126 12290045 minecraft:air
setblock -28661765 126 12290045 minecraft:redstone_block
tellraw @a ["BIKEFIX2 comparator out ",{"block":"-28661765 126 12290046","nbt":"OutputSignal"}]
tellraw @a "BIKEFIX2_DONE"
