#!/bin/bash

if [[ $BUILD_LINUX_64 == "y" ]]; then
  DOWNLOAD_JDK="n"
  if [[ $DOWNLOAD_EVERY_TIME == "y" ]]; then
    DOWNLOAD_JDK="y"
  elif [[ -z "$(ls $CURRENT_DIRECTORY/downloads/jdk/linux_64)" ]]; then
    DOWNLOAD_JDK="y"
  fi

  if [[ $DOWNLOAD_JDK == "y" ]]; then
    rm $CURRENT_DIRECTORY/downloads/jdk/linux_64/* -rf
    wget -P $CURRENT_DIRECTORY/downloads/jdk/linux_64/ $JDK_LINUX_64_DOWNLOAD_URL
    create_git_ignore $CURRENT_DIRECTORY/downloads/jdk/linux_64/.gitignore
    tar -xzf $CURRENT_DIRECTORY/downloads/jdk/linux_64/*.gz --strip 1 -C $CURRENT_DIRECTORY/downloads/jdk/linux_64
    rm $CURRENT_DIRECTORY/downloads/jdk/linux_64/*.gz
  fi
fi