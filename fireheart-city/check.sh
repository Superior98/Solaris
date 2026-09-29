#!/bin/bash
D=${1:-$PWD}
cd "$D" || exit 1
rm -rf /tmp/chk_$$ && mkdir -p /tmp/chk_$$
javac --release 17 -nowarn -proc:none -cp "$(cat /tmp/modbuild/cp.txt)" -d /tmp/chk_$$ $(find src -name '*.java') 2>&1 | grep -v -E 'JAVA_TOOL|^Note|^Picked'
RC=${PIPESTATUS[0]}
rm -rf /tmp/chk_$$
[ $RC -eq 0 ] && echo "COMPILE OK" || echo "COMPILE FAILED"
