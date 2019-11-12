#!/bin/bash

if [[ $BUILD_LINUX_64 == "y" ]]; then
  DOWNLOAD_JDK="n"
  if [[ $DOWNLOAD_EVERY_TIME == "y" ]]; then
    DOWNLOAD_JDK="y"
  elif [[ -z "$(ls $CURRENT_DIRECTORY/downloads/jdk/linux_64)" ]]; then
    DOWNLOAD_JDK="y"
  fi
fi