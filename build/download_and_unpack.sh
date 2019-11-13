#!/bin/bash


function download_and_unpack {
  rm $1/* -rf
  create_git_ignore $1/.gitignore

  wget -P $1 $2

  if [[ -z "$(ls $1 *.gz)" ]]; then
    bsdtar --strip-components=1 -xzf $1/*.gz -C $1
    rm $1/*.gz
  elif [[ -z "$(ls $1 *.zip)" ]]; then
    bsdtar --strip-components=1 -xzf $1/*.zip -C $1
    rm $1/*.zip
  else
    echo "unknown archive format in $1"
    exit 1
  fi
}

if [[ $BUILD_LINUX_64 == "y" ]]; then
  DOWNLOAD_JDK="n"
  if [[ $DOWNLOAD_EVERY_TIME == "y" ]]; then
    DOWNLOAD_JDK="y"
  elif [[ -z "$(ls $CURRENT_DIRECTORY/downloads/jdk/linux_64)" ]]; then
    DOWNLOAD_JDK="y"
  fi

  if [[ $DOWNLOAD_JDK == "y" ]]; then
    download_and_unpack $CURRENT_DIRECTORY/downloads/jdk/linux_64 $JDK_LINUX_64_DOWNLOAD_URL
  else
    echo "jdk for linux x64 has already been downloaded"
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
    download_and_unpack $CURRENT_DIRECTORY/downloads/jdk/win_64 $JDK_WIN_64_DOWNLOAD_URL
  else
    echo "jdk for windows x64 has already been downloaded"
  fi
fi