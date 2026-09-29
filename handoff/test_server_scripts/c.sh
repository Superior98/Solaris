#!/bin/bash
for a in "$@"; do echo "$a" >> /tmp/srv/cmds.txt; done
