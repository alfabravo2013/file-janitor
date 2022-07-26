#!/usr/bin/env bash

# write your script here

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

# checks if $1 is a valid dir
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
    echo

    check_if_valid "$1"

    if [ "$1" = "." ]; then
        echo "Listing files in the current directory"
    else
        echo "Listing files in $1"
    fi
    echo

    ls -1 -A "$1"
}

# reports the number and total size of certain files are in the specified dir
report_dir_content() {
    echo

    check_if_valid "$1"

    if [ "$1" = "." ]; then
        echo "The current directory contains:"
    else
        echo "$1 contains:"
    fi

    for ext in "tmp" "log" "py"; do
        # non-recursive search
        count=0
        size=0
        list=$(find "$1" -maxdepth 1 -name "*.$ext" 2>/dev/null)
        for file in $list; do
            ((count+=1))
            ((size+=$(wc -c < "$file")))
        done

        echo "$count $ext file(s), with total size of $size bytes"
    done
}

###########################
# main body of the script #
###########################

# to handle filenames with blank spaces
IFS=$'\n'
show_info

case "$1" in
    help )
        show_help;;
    list )
        show_dir_content "${2-.}";;
    report )
        report_dir_content "${2-.}";;
    *)
        show_hint;;
esac
