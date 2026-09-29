scoreboard players set #state sky 2
scoreboard players set #t sky 0
title @a times 10 60 20
title @a subtitle {"text":"Sneak to step off - sit again to fly home","color":"aqua"}
title @a title {"text":"NEON HEIGHTS","color":"light_purple","bold":true}
execute as @a at @s run playsound minecraft:block.beacon.deactivate master @s ~ ~ ~ 1 1.2
execute as @a at @s run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 0.6 1.4
