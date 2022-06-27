#!/usr/bin/env bash

# displays info about the script
show_info() {
    year=$(date +%Y)
    echo "File Janitor, $year"
    echo "Powered by Bash"
}

# displays help file with hardcoded filename
show_help() {
    echo
    cat file-janitor-help.txt
}

# displays a hint to use 'help'
show_hint() {
    echo
    echo "Type $(basename "$0") help to see available options"
}

check_if_valid() {
    if [ ! -e "$1" ]; then
        echo "$1 is not found"
        exit 0
    fi

    if [ ! -d "$1" ]; then
        echo "$1 is not a directory"
        exit 0
    fi
}

# displays the content of a directory in a pre-defined format
show_dir_content() {

    check_if_valid "$1"

    local dir_name="$1"
    if [ "$1" = "." ]; then
        dir_name="the current directory"
    fi

    echo
    echo "Listing files in $dir_name"
    echo

    ls -1 -A "$1"
}

# selects appropriate action depending on the args passed to the script
select_action() {
    # option 'help'
    if [ "$1" = "help" ]; then
        show_help
    # option 'list'
    elif [ "$1" = "list" ]; then
        show_dir_content "${2-.}"
    # other options to follow
    else
      show_hint
    fi
}

# main body of the script
show_info

if [ $# -eq 0 ]; then
    show_hint
else
    select_action "$@"
fi
