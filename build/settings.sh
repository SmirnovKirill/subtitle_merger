#!/bin/bash

set -e

BUILD_LINUX_64="y"
BUILD_WIN_64="y"
INCLUDE_FFMPEG="y"
DOWNLOAD_EVERY_TIME="n" #for debug mostly

#https://jdk.java.net/archive/
JDK_LINUX_64_DOWNLOAD_URL="https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz"
JDK_WIN_64_DOWNLOAD_URL="https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_windows-x64_bin.zip"

#https://gluonhq.com/products/javafx/
OPENJFX_LINUX_DOWNLOAD_URL="http://gluonhq.com/download/javafx-11-0-2-sdk-linux/"
OPENJFX_WIN_DOWNLOAD_URL="http://gluonhq.com/download/javafx-11-0-2-sdk-windows/"

#https://www.ffmpeg.org/download.html
FFMPEG_LINUX_64_DOWNLOAD_URL="https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz"
FFMPEG_WIN_64_DOWNLOAD_URL="https://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-20191111-20c5f4d-win64-static.zip"

