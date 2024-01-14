#!/bin/bash

# Check if exactly two arguments are provided
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 [-i|-o] filename"
    exit 1
fi

# Assign arguments to variables
flag="$1"
filename="$2"

# Check for valid flag
if [ "$flag" != "-i" ] && [ "$flag" != "-o" ]; then
    echo "Invalid flag. Use -i for info or -o for optimise."
    exit 1
fi

# Check if the file exists
if [ ! -f "$filename" ]; then
    echo "Error: File '$filename' not found."
    exit 1
fi

# Call the Java class with the flag and filename
java -jar /usr/local/lib/java/mkvoptimise/MKVInfoParser.jar "$flag" "$filename"