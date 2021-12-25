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

echo "Clean KeYLib"
(cd KeYLib/;
 rm -rf antlr bibliothek data de de.uka.ilkd.key.util fonts \
    javax javacc.class jjdoc.class jjtree.class MANIFEST.MF examples.zip \
    key.properties module-info.class META-INF net org recoder services;
 ll)

unzip -v $SHADOW_JAR

echo "Unpacking $SHADOW_JAR into KeYLib"
(cd KeYLib; unzip $SHADOW_JAR)

git status
