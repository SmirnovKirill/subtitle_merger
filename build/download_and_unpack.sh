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
    bsdtar --strip-components=1 -xzf $CURRENT_DIRECTORY/downloads/jdk/linux_64/*.gz -C $CURRENT_DIRECTORY/downloads/jdk/linux_64
    rm $CURRENT_DIRECTORY/downloads/jdk/linux_64/*.gz
  fi
fi

if [[ $BUILD_WIN_64 == "y" ]]; then
  DOWNLOAD_JDK="n"
  if [[ $DOWNLOAD_EVERY_TIME == "y" ]]; then
    DOWNLOAD_JDK="y"
  elif [[ -z "$(ls $CURRENT_DIRECTORY/downloads/jdk/win_64)" ]]; then
    DOWNLOAD_JDK="y"
  fi

  if [[ $DOWNLOAD_JDK == "y" ]]; then
    rm $CURRENT_DIRECTORY/downloads/jdk/win_64/* -rf
    wget -P $CURRENT_DIRECTORY/downloads/jdk/win_64/ $JDK_WIN_64_DOWNLOAD_URL
    create_git_ignore $CURRENT_DIRECTORY/downloads/jdk/win_64/.gitignore
    bsdtar --strip-components=1 -xzf $CURRENT_DIRECTORY/downloads/jdk/win_64/*.zip -C $CURRENT_DIRECTORY/downloads/jdk/win_64
    rm $CURRENT_DIRECTORY/downloads/jdk/win_64/*.zip
  fi
fi