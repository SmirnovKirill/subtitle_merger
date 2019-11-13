#!/bin/bash

function need_to_download {
  if [[ $DOWNLOAD_EVERY_TIME == "y" ]]; then
    echo "y"
  elif [[ -z $(ls "$1") ]]; then
    echo "y"
  else
    echo "n"
  fi
}

function download_and_unpack {
  rm "${1:?}/*" -rf
  create_git_ignore "$1/.gitignore"

  wget -P "$1" "$2"

  if [[ -n $(find "$1" -maxdepth 1 -name "*.gz") ]]; then
    bsdtar --strip-components=1 -xzf "${1:?}/*.gz" -C "$1"
    rm "{$1:?}/*.gz"
  elif [[ -n $(find "$1" -maxdepth 1 -name "*.zip") ]]; then
    bsdtar --strip-components=1 -xzf "${1:?}/*.zip" -C "$1"
    rm "{$1:?}/*.zip"
  else
    echo "unknown archive format in $1"
    exit 1
  fi
}

if [[ $BUILD_LINUX_64 == "y" ]]; then
  DOWNLOAD_JDK=$(need_to_download "$CURRENT_DIRECTORY/downloads/jdk/linux_64")
  if [[ $DOWNLOAD_JDK == "y" ]]; then
    download_and_unpack "$CURRENT_DIRECTORY/downloads/jdk/linux_64" "$JDK_LINUX_64_DOWNLOAD_URL"
  else
    echo "jdk for linux x64 has already been downloaded"
  fi

  DOWNLOAD_JAVAFX=$(need_to_download "$CURRENT_DIRECTORY/downloads/javafx/linux")
  if [[ $DOWNLOAD_JAVAFX == "y" ]]; then
    download_and_unpack "$CURRENT_DIRECTORY/downloads/javafx/linux" "$OPENJFX_LINUX_DOWNLOAD_URL"
  else
    echo "javafx for linux has already been downloaded"
  fi

  if [[ $INCLUDE_FFMPEG == "y" ]]; then
    DOWNLOAD_FFMPEG=$(need_to_download "$CURRENT_DIRECTORY/downloads/ffmpeg/linux_64")
    if [[ $DOWNLOAD_FFMPEG == "y" ]]; then
      download_and_unpack "$CURRENT_DIRECTORY/downloads/ffmpeg/linux_64" "$FFMPEG_LINUX_64_DOWNLOAD_URL"
    else
      echo "ffmpeg for linux x64 has already been downloaded"
    fi
  else
    echo "don't include ffmpeg according to the settings"
  fi
fi

if [[ $BUILD_WIN_64 == "y" ]]; then
  DOWNLOAD_JDK=$(need_to_download "$CURRENT_DIRECTORY/downloads/jdk/win_64")
  if [[ $DOWNLOAD_JDK == "y" ]]; then
    download_and_unpack "$CURRENT_DIRECTORY/downloads/jdk/win_64" "$JDK_WIN_64_DOWNLOAD_URL"
  else
    echo "jdk for windows x64 has already been downloaded"
  fi

  DOWNLOAD_JAVAFX=$(need_to_download "$CURRENT_DIRECTORY/downloads/javafx/win")
  if [[ $DOWNLOAD_JAVAFX == "y" ]]; then
    download_and_unpack "$CURRENT_DIRECTORY/downloads/javafx/win" "$OPENJFX_WIN_DOWNLOAD_URL"
  else
    echo "javafx for windows has already been downloaded"
  fi

  if [[ $INCLUDE_FFMPEG == "y" ]]; then
    DOWNLOAD_FFMPEG=$(need_to_download "$CURRENT_DIRECTORY/downloads/ffmpeg/win_64")
    if [[ $DOWNLOAD_FFMPEG == "y" ]]; then
      download_and_unpack "$CURRENT_DIRECTORY/downloads/ffmpeg/win_64" "$FFMPEG_WIN_64_DOWNLOAD_URL"
    else
      echo "ffmpeg for windows x64 has already been downloaded"
    fi
  else
    echo "don't include ffmpeg according to the settings"
  fi
fi