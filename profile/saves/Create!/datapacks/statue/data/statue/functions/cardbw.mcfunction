setblock -28669953 127 12290047 create:gearshift[axis=x]
setblock -28669952 127 12290047 create:clutch[axis=x]
setblock -28669953 128 12290047 create:red_seat
setblock -28669951 127 12290049 minecraft:black_concrete
setblock -28669950 127 12290049 minecraft:black_concrete
setblock -28669952 127 12290049 minecraft:redstone_block
setblock -28669951 127 12290048 minecraft:lever[face=floor,facing=north,powered=false]
setblock -28669952 127 12290048 minecraft:comparator[facing=south,mode=subtract]
setblock -28669951 128 12290047 drivebywire:controller_hub
setblock -28669951 128 12290046 drivebywire:backup_block[facing=north]{WireNetwork:{Name:"car",BackupOffset:0L,Network:{car:{keyUp:[L;4096L,-274877894657L,5L],keyDown:[L;4096L,-274877894657L,5L,4096L,-549755805697L,1L],keyLeft:[L;4096L,824633720831L,1L],keyRight:[L;4096L,549755830271L,1L],keyJump:[L;4096L,8192L,1L]}}}}
give @s create:linked_controller{Hub:-7880736072727007104L}
say CAR_DBW_DONE
