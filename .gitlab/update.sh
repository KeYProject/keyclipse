#!/bin/sh

KEY_FOLDER=${KEY_FOLDER:-key}
KEY_BRANCH=${KEY_BRANCH:stable}


if [ ! -f $KEY_FOLDER ]; then
    echo "KeY folder '$KEY_FOLDER' does not exists..."
    echo "Checking out key-public to $KEY_FOLDER with branch $KEY_BRANCH"

    git --depth 1 --branch $KEY_BRANCH https://git.key-project.org/key-public/key $KEY_FOLDER
fi

echo "Building key..."
(cd $KEY_FOLDER/key;
 gradle --parallel :key.ui:shadowJar)


SHADOW_JAR=$(ls $KEY_FOLDER/key/key.ui/build/libs/*-exe.jar)

echo "Unpacking $SHADOW_JAR into KeYLib"
# zip
