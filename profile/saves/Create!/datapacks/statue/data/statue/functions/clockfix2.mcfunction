setblock -26 64 17 create:creative_fluid_tank{Size:1,Height:1,Uninitialized:1b,TankContent:{FluidName:"createdieselgenerators:diesel",Amount:8000}}
setblock -26 65 17 create:mechanical_pump[facing=up]
setblock -26 66 17 createdieselgenerators:diesel_engine[facing=south]{Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
setblock -26 66 18 create:gearbox[axis=x]
setblock -26 65 18 create:cogwheel[axis=y]
fill -26 67 18 -26 87 18 create:shaft[axis=y]
setblock -26 88 18 create:gearbox[axis=x]
setblock -26 88 17 create:shaft[axis=z]
setblock -26 88 19 create:shaft[axis=z]
setblock -26 88 16 create:cuckoo_clock[facing=north]
setblock -26 88 20 create:cuckoo_clock[facing=south]
setblock -26 89 18 create:gearbox[axis=z]
setblock -27 89 18 create:shaft[axis=x]
setblock -25 89 18 create:shaft[axis=x]
setblock -28 89 18 create:cuckoo_clock[facing=west]
setblock -24 89 18 create:cuckoo_clock[facing=east]
say CLOCKFIX2_DONE
