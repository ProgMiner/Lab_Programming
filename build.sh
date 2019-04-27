#!/bin/bash

# Script for building lab works of Programming discipline at SEaCS ITMO on Helios

# MIT License
#
# Copyright (c) 2019 Eridan Domoratskiy
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# Name of lab work
LAB_NAME='lab7'

# Array of external libs' JARs
LAB_LIBS=($(echo libs/* | while read lib ; do echo "$lib" ; done))

# Array of entry points for compilation (e.g. main classes)
LAB_ENTRIES=(
    'ru.byprogminer.Lab5_Programming.Main'
    'ru.byprogminer.Lab6_Programming.Main'
)

# Array of building JAR artifacts
#
# Each JAR is represents by several items in array:
#   - Additional char-options for `jar`
#   - JAR file's name
#   - Num of additional arguments
#   - Additional argument 1
#   - ...
#   - Additional argument N (= Num of additional arguments)
#
# !!! Files in additional arguments have to specified relative to $LAB_PATH/$BIN_DIR !!!
LAB_JARS=(
    'me' 'lab6_server.jar' 2 '../manifest.mf' 'ru.byprogminer.Lab5_Programming.Main'
    'me' 'lab6_client.jar' 2 '../manifest.mf' 'ru.byprogminer.Lab6_Programming.Main'
)

# Name of sources' directory in the lab's directory
SRC_DIR='src'

# Name of binaries' directory in the lab's directory
BIN_DIR='bin'

# Name of docs' directory in the lab's directory
DOC_DIR='doc'

# Lab's directory
LAB_PATH="$(cd ~ && pwd)/$LAB_NAME"

# Directory with web-files
WEB_PATH="$(cd ~/public_html && pwd)"

# Command for start `javac`
CMD_JAVAC='javac18'

# Command for start `javadoc`
CMD_JAVADOC='javadoc18'

# Command for start `jar`
CMD_JAR='jar'


# Internal variables

warns=()


# Functions

# https://stackoverflow.com/questions/1527049/how-can-i-join-elements-of-an-array-in-bash
function join_by() {
    local IFS="$1"
    shift

    echo "$*"
}

function warn() {
    local warn="$(echo "Warning:" "$@")"
    warns=("${warns[@]}" "$warn")

    echo "$warn"
}

function print_warns() {
    for warn in "${warns[@]}" ; do
        echo "$warn"
    done
}

function die() {
    print_warns
    echo "$@"
    kill %%
}


# Entry point

# Go to lab's directory
cd "$LAB_PATH"
[[ $? -ne 0 ]] && die "It seems like there isn't the lab's directory"

# Compile files
mkdir -p bin
classpath="$(join_by ':' "${LAB_LIBS[@]}")"
for entry in "${LAB_ENTRIES[@]}" ; do
    "$CMD_JAVAC" -cp "$classpath" -sourcepath "$SRC_DIR" -d "$BIN_DIR" "$SRC_DIR/${entry//.//}.java"
    [[ $? -ne 0 ]] && die "An error occurred while compilation from the entry point $entry"
done

# Make JavaDocs
"$CMD_JAVADOC" -sourcepath "$SRC_DIR" -d "$DOC_DIR" -classpath "$classpath" \
    $(find "$SRC_DIR" -type d | while read pkg ; do pkg="${pkg/$SRC_DIR\//}" ; echo "${pkg////.}" ; done)
[[ $? -ne 0 ]] && warn 'An error occurred while making a JavaDoc'

# Upload JavaDocs to web
rm -r "$WEB_PATH/$LAB_NAME"
cp -r "$DOC_DIR" "$WEB_PATH/$LAB_NAME"
[[ $? -ne 0 ]] && warn 'An error occurred while uploading the JavaDoc to web'

# Build JARs
cd "$BIN_DIR"
for (( i = 0; i < "${#LAB_JARS[@]}"; ++i )) ; do
    opts="${LAB_JARS[$i]}"

    (( ++i ))
    [[ $i -ge "${#LAB_JARS[@]}" ]] && die 'Bad trailing elements in LAB_JARS'
    filename="${LAB_JARS[$i]}"

    (( ++i ))
    [[ $i -ge "${#LAB_JARS[@]}" ]] && die 'Bad trailing elements in LAB_JARS'
    args_count="${LAB_JARS[$i]}"
    args=()

    for (( j = 0; j < "$args_count"; ++j )) ; do
        (( ++i ))
        [[ $i -ge "${#LAB_JARS[@]}" ]] && die 'Bad trailing elements in LAB_JARS'

        args[$j]="${LAB_JARS[$i]}"
    done

    "$CMD_JAR" "-cvf$opts" "../$filename" "${args[@]}" *
    [[ $? -ne 0 ]] && die "An error occurred while building the $filename artifact"
done

print_warns
echo 'Done!'
