if [ -d dist ]; then
    echo "dist/ already exists, please delete it first"
    exit 0
fi

if ! mkdir dist/; then
    echo "Exiting..."
    exit -1
fi

nlScript_version="0.1.0"

version=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`

if ! [[ $version =~ [0-9]+\.[0-9]+\.[0-9]+ ]]; then
    echo "Strange version string: $version"
    echo "Exiting..."
    exit -1
fi

cp target/screenplay-$version.jar dist/
cp ../nlScript-java/target/nlScript-${nlScript_version}.jar dist/

(cd dist/ && \
    curl -O https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.10.0/jna-5.10.0.jar && \
    curl -O https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.10.0/jna-platform-5.10.0.jar)

jpackage --input dist/ \
         --name Screenplay \
         --main-jar screenplay-$version.jar \
         --main-class nlScript.screenplay.Main \
         --type exe \
         --win-menu --win-menu-group "Screenplay" \
	 --app-version $version \
	 --copyright "Benjamin Schmid" \
	 --license-file LICENSE.txt \
	 --icon icon.ico

