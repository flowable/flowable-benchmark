# Build jar

Run 'mvn clean package'

A jar _flowable-benchmark.jar_ with all dependencies will be created in the _target_ folder.

# Run jar

Create a file _config.properties_ next to the jar. Check the _example-config.properties_ file in _src/main/resources_ for all possible properties.

java -Xms1024m -Xmx20148m -jar flowable-benchmark.jar