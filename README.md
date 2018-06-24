First I implemented spring boot-batch then I implemented my multi thread solution(FasterSolution.java). how to run and test results are below. I think this can be done better with stream API but I did not have more time to research and implement better solution.

How to run spring boot application
./gradlew bootRun -Pargs=example.data.json

for big json file
./gradlew bootRun -Pargs=sample.json -Dorg.gradle.jvmargs=-Xmx4g

Test Result
Chunk size :10000 jsonsize:8gb 100000000lines took 36m

running faster solution
./gradlew -PmainClass=FasterSolution -Pargs=sample.json execute 

latest test with 8gb file took 33m. therefore spring boot beaten by 3 minutes :)
./gradlew -PmainClass=FasterSolution -Pargs=sample.json execute -Dorg.gradle.jvmargs="-Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication"


For proper testing I implemented a json generator. you can easily generate 16gb json file. took 7 minutes in my laptop to run command below
after gson cleared I re-run the command and it took 5.23 minutes.
./gradlew -PmainClass=data.generator.GenerateSampleJson execute -Pargs= -Dorg.gradle.jvmargs="-Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication"

