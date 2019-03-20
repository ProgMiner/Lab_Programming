#!/bin/bash

cd ~/lab6

mkdir -p bin
javac18 -cp fastjson.jar -sourcepath src -d bin src/ru/byprogminer/Lab{5,6}_Programming/Main.java

javadoc18 -classpath fastjson.jar -sourcepath src -d doc ru.byprogminer.Lab{3,4,5,6}_Programming

rm -r ~/public_html/lab6
cp -r doc ~/public_html/lab6

cd bin
jar -cvfme ../lab6_server.jar ../manifest.mf ru.byprogminer.Lab5_Programming.Main *
jar -cvfme ../lab6_client.jar ../manifest.mf ru.byprogminer.Lab6_Programming.Main *


cd ~/lab6
rm lab6.zip

cd ..
zip -r lab6{/lab6.zip,}

cd lab6
cp lab6.zip ~/public_html/lab6/
