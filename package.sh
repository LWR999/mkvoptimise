#!/bin/bash

rm MKVInfoParser.jar
javac -d bin -cp ./lib/json-20231013.jar ./src/*.java

jar cvfm MKVInfoParser.jar Manifest.txt -C bin/ .

mkdir temp
cd temp
jar xf ../lib/json-20231013.jar
rm -rf META-INF
jar uf ../MKVInfoParser.jar *
cd ..
rm -rf temp

cp MKVInfoParser.jar /Volumes/_Torrents/completed/
