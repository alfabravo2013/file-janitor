#!/usr/bin/env bash

# write your script here
show_info() {
    year=$(date +%Y)
    echo "File Janitor, $year"
    echo "Powered by Bash"
}

show_help() {
    echo
    cat file-janitor-help.txt
}

show_info

if [[ $# -gt 0 ]] && [[ $1 = "help" ]]; then
    show_help
fi
