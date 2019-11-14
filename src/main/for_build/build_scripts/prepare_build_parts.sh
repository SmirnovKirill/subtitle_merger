#!/bin/bash

set -e

CURRENT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd )"
PROJECT_BASE_DIR="$(dirname "$(dirname "$(dirname "$(dirname "$CURRENT_DIRECTORY")")")")"

function create_git_ignore {
  cat << EndOfText | head -c -1 > "$1"
*
!.gitignore
EndOfText
}

source "$CURRENT_DIRECTORY/settings.sh"
source "$CURRENT_DIRECTORY/download_and_unpack.sh"
source "$CURRENT_DIRECTORY/make_custom_jre.sh"