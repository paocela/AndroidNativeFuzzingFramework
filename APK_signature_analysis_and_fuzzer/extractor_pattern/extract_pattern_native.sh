#!/bin/bash

# compile
javac -cp 'com.qdox.jar:.' extractor.java

# execute
java -cp 'com.qdox.jar:.' extractor $1
