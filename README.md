How to run spring boot application
./gradlew bootRun -Pargs=example.data.json

for big json file
./gradlew bootRun -Pargs=sample.json -Dorg.gradle.jvmargs=-Xmx4g

Test Result
Chunk size :10000 jsonsize:8gb 100000000lines took 36m