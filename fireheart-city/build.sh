#!/bin/bash
cd /tmp/modbuild
rm -rf classes && mkdir classes
CP=$(cat cp.txt)
javac --release 17 -nowarn -proc:none -cp "$CP" -d classes $(find src -name '*.java') > javac.log 2>&1; RC=$?
grep -v -E 'JAVA_TOOL|^Note|^Picked' javac.log
if [ $RC -ne 0 ]; then echo "BUILD FAILED"; rm -f fhc.jar; exit 1; fi
rm -f named-mod.jar fhc.jar
(cd classes && jar cfm ../named-mod.jar ../manifest.txt .) && (cd res && jar uf ../named-mod.jar .)
java -jar /mnt/user-data/uploads/meta/libraries/net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar --input named-mod.jar --output fhc.jar --map srg2named.srg --reverse -e named-forge-1.20.1-47.4.20-client.jar -e named-client-1.20.1-20230612.114412-srg.jar -e named-forge-1.20.1-47.4.20-universal.jar > art-mod.log 2>&1
ls -l fhc.jar
