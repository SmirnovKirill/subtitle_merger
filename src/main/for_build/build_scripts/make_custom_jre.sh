#!/bin/bash

set -e

function make_custom_jre {
  echo "start creating custom jre for $1"

  rm "$PROJECT_BASE_DIR/build_parts/custom_jre/$1" -rf

  if [[ $1 == "linux_64" ]]; then
    JAVAFX_DIRECTORY_NAME="linux"
  elif [[ $1 == "win_64" ]]; then
    JAVAFX_DIRECTORY_NAME="win"
  else
    echo "unknown argument $1"
    exit 1
  fi

  jlink --module-path "$PROJECT_BASE_DIR/build_parts/downloads/jdk/$1/jmods:$PROJECT_BASE_DIR/build_parts/downloads/javafx_jmods/$JAVAFX_DIRECTORY_NAME" \
    --compress=2 \
    --no-man-pages \
    --add-modules "$CUSTOM_JRE_ALL_MODULES" \
    --output "$PROJECT_BASE_DIR/build_parts/custom_jre/$1"

  create_git_ignore "$PROJECT_BASE_DIR/build_parts/custom_jre/$1/.gitignore"

  echo "custom jre for $1 has been successfully created"
}

make_custom_jre "win_64"
make_custom_jre "linux_64"