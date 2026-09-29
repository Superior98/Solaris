#!/bin/bash
# usage: restart.sh [fresh]  - stops server, optionally copies a fresh world, installs /tmp/modbuild/fhc.jar, starts
cd /tmp/srv
pgrep -f "[c]pw.mods.bootstraplauncher.BootstrapLauncher" | xargs -r kill
pgrep -f "[t]ail -f cmds.txt" | xargs -r kill
sleep 4
if [ "$1" = "fresh" ]; then rm -rf world; cp -r "/mnt/user-data/uploads/Create!" world; chmod -R u+w world; fi
cp /tmp/modbuild/fhc.jar mods/fireheartcity-test.jar
: > cmds.txt
[ -f out.log ] && mv out.log out.prev.log
setsid nohup ./run.sh > /dev/null 2>&1 &
for i in $(seq 1 60); do sleep 3; grep -q "Done (" out.log 2>/dev/null && break; done
grep "Done (" out.log | tail -1
