tellraw @a ["BIKEDIAG engine ",{"block":"-28661763 128 12290047","nbt":"Speed"},{"text":" fuel "},{"block":"-28661763 128 12290047","nbt":"Tanks[0].TankContent.Amount"}]
tellraw @a ["BIKEDIAG gearbox1 ",{"block":"-28661764 126 12290047","nbt":"Speed"}]
tellraw @a ["BIKEDIAG clutch ",{"block":"-28661765 126 12290047","nbt":"Speed"}]
tellraw @a ["BIKEDIAG gearshift ",{"block":"-28661766 126 12290047","nbt":"Speed"}]
tellraw @a ["BIKEDIAG axle ",{"block":"-28661767 126 12290047","nbt":"Speed"}]
tellraw @a ["BIKEDIAG comparator out ",{"block":"-28661765 126 12290046","nbt":"OutputSignal"}]
execute if block -28661765 126 12290047 create:clutch[powered=true] run tellraw @a "BIKEDIAG clutch POWERED (disengaged)"
execute if block -28661765 126 12290047 create:clutch[powered=false] run tellraw @a "BIKEDIAG clutch unpowered (engaged)"
execute if block -28661763 128 12290047 createdieselgenerators:diesel_engine run tellraw @a "BIKEDIAG engine block present"
execute if block -28661763 127 12290047 create:mechanical_pump run tellraw @a "BIKEDIAG pump present"
