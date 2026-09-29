tag @a remove car_driver
tag @a[nbt={RootVehicle:{Entity:{id:"create:seat"}},SelectedItem:{id:"create:linked_controller"}}] add car_driver
execute as @a[tag=car_engine_on,tag=!car_driver] run function statue:car_engine_off
execute as @a[tag=car_driver] at @s if block -28669952 127 12290047 create:clutch run function statue:car_engine_sound
