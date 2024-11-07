#!/bin/bash

# Check if an argument was provided via the command line
if [ $# -gt 0 ]; then
    ver="$1"
    echo "Argument provided via command line: $ver"
else
    # If no argument was provided, prompt the user for input
    read -p "Please enter an argument: " user_input
    ver="$user_input"
    echo "Argument provided by user: $ver"
fi

mvn versions:set -DnewVersion=$ver
mvn versions:set -DnewVersion=$ver -f pom-jdk8.xml

# after this, you need to commit the changes
# mvn versions:commit ; mvn versions:commit -f pom-jdk8.xml