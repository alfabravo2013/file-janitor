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

# displays the content of $1 dir in a pre-defined format
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

# reports the number and total size of certain files are $1 dir
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
        local count=0
        size=0
        list=$(find "$1" -maxdepth 1 -name "*.$ext" 2>/dev/null)
        for f in $list; do
            ((count+=1))
            ((size+=$(wc -c < "$f")))
        done

        echo "$count $ext file(s), with total size of $size bytes"
    done
}

# cleans up $1 directory
clean_dir() {
    echo

    check_if_valid "$1"

    local cur_dir=$1
    if [ "$1" = "." ]; then
        cur_dir="the current directory"
    fi

    echo "Cleaning $cur_dir..."

    compress_logs "$1"
    remove_tmp "$1"
    move_py_scripts "$1"

    echo
    echo "Clean up of $cur_dir is complete!"
}

# compresses *.log files in $1 dir and reports number of compressed files
compress_logs() {
    total_logs=$(find "$1" -maxdepth 1 -name "*.log" 2>/dev/null | wc -l)
    echo -n "Compressing log files..."

    if [ "$total_logs" -gt 0 ]; then
        tar cvzf "$1"/logs.tar.gz ./*.log 2>&1>/dev/null
        find "$1" -maxdepth 1 -name "*.log" -exec rm {} \;
    fi

    echo "  done! $total_logs files have been compressed"
}

# removes *.tmp files in $1 dir and reports number of removed files
remove_tmp() {
    total_tmp=$(find "$1" -maxdepth 1 -name "*.tmp" 2>/dev/null | wc -l)
    echo -n "Deleting temporary files..."
    find "$1" -maxdepth 1 -name "*.tmp" -exec rm {} \;
    echo "  done! $total_tmp files have been removed"
}

# moves python scripts to python_scripts sub-dir of $1 and reports number of moved files
move_py_scripts() {
    dir_exists=0
    if [[ -e $1/py_scripts ]] && [[ -d $1/py_scripts ]]; then
        dir_exists=1
    fi

    echo -n "Moving python files..."

    total_py=$(find "$1" -maxdepth 1 -name "*.py" 2>/dev/null | wc -l)
    if [[ $total_py -gt 0 ]]; then
        mkdir -p "$1/py_scripts"
        find "$1" -maxdepth 1 -name "*.py" -exec mv {} "$1/py_scripts" \;
    fi

    echo "  done! $total_py files have been moved"

    if [[ $dir_exists -eq 0 ]]; then
        rmdir "$1/py_scripts" 2>/dev/null
    fi
}

###########################
# main body of the script #
###########################

IFS=$'\n' # to handle filenames with blank spaces

show_info

case "$1" in
    help )
        show_help;;
    list )
        show_dir_content "${2-.}";;
    report )
        report_dir_content "${2-.}";;
    clean )
        clean_dir "${2-.}";;
    *)
        show_hint;;
esac
