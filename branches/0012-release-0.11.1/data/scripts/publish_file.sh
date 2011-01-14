#!/bin/bash

declare -r file=$1

rm -f "$file".*
date +%s > "$file.info"
split -d -b 1000000 "$file" "$file.part."
du -b "$file".part.* > "$file.parts"
gzip "$file".part.*

