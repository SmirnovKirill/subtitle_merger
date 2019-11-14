#!/bin/bash

CURRENT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd )"
"$CURRENT_DIRECTORY/jre/bin/java" -jar "$CURRENT_DIRECTORY/"*.jar