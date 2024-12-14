#!/bin/bash
find src/main/java/ -type l -delete
directories=$(find ~/ -type d -path "*/scent-java/src/main/java/com/hirohiro716/*" -name scent 2>/dev/null)
for directory in $directories
do
    mkdir -p src/main/java/com/hirohiro716/
    ln -s $directory src/main/java/com/hirohiro716/scent
    echo ライブラリへのリンクを作成しました。
    echo $directory
    exit 0
done
