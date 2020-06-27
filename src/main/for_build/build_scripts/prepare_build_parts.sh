#!/bin/bash

set -e

CURRENT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd )"
PROJECT_BASE_DIRECTORY="$(dirname "$(dirname "$(dirname "$(dirname "$CURRENT_DIRECTORY")")")")"
CUSTOM_JRE_DIRECTORY="$PROJECT_BASE_DIRECTORY/build_parts/custom_jre"
DOWNLOADED_FFMPEG_DIRECTORY="$PROJECT_BASE_DIRECTORY/build_parts/downloads/ffmpeg"
DOWNLOADED_JAVAFX_DIRECTORY="$PROJECT_BASE_DIRECTORY/build_parts/downloads/javafx_jmods"
DOWNLOADED_JDK_DIRECTORY="$PROJECT_BASE_DIRECTORY/build_parts/downloads/jdk"

function create_git_ignore {
  cat << EndOfText | head -c -1 > "$1"
*
!.gitignore
EndOfText
}

source "$CURRENT_DIRECTORY/settings.sh"
source "$CURRENT_DIRECTORY/download_and_unpack.sh"
source "$CURRENT_DIRECTORY/make_custom_jre.sh"