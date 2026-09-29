summon create:seat -28657664.5 126 12290045.5 {Tags:["skytest"]}
ride @s mount @e[type=create:seat,tag=skytest,limit=1,sort=nearest]
tag @e[type=create:seat,tag=skytest] remove skytest
