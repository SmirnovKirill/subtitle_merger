#!/bin/bash

function make_custom_jre {
  rm $1 -rf

  jlink --module-path ../manual_downloads/jdk/$1/jmods \
    --compress=2 \
    --no-man-pages \
    --add-modules java.base,java.prefs \
    --output $1

  echo "*" >> $1/.gitignore
}

make_custom_jre "win_64"
make_custom_jre "linux_64"