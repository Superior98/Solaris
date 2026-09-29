kill @e[type=create:seat,tag=bikeseat]
summon create:seat -28661760.5 127.0 12290047.5 {Tags:["bikeseat"]}
ride @s mount @e[type=create:seat,tag=bikeseat,limit=1]
item replace entity @s hotbar.2 with create:linked_controller{Hub:-7878484547791228801L,display:{Name:'{"text":"Motorbike","color":"gold","italic":false}'}}
say BIKESIT_DONE
