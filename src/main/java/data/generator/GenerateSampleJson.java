package data.generator;

import EPTFAssignment.solver.GsonUtils;
import EPTFAssignment.solver.ServerEvent;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Random;

public class GenerateSampleJson {
    static int objectCount = 100000000;

    public static void main(String[] args) throws Exception {
        Random r = new Random();
        String host = "12345";
        String type = "APPLICATION_LOG";
        long startTime = System.nanoTime();
        int i = objectCount;
        Path path = Paths.get("sample.json");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            while (i-- > 0) {
                String id = RandomStringUtils.randomAlphabetic(10);
                Instant instant = Instant.now();
                long timestamp = instant.toEpochMilli();
                ServerEvent se = new ServerEvent(id, "STARTED", timestamp, type, host);
                writer.write(GsonUtils.getInstance().toJson(se) + System.lineSeparator());

                se = new ServerEvent(id, "FINISHED", timestamp + r.nextInt(10), null, null);
                writer.write(GsonUtils.getInstance().toJson(se) + System.lineSeparator());
            }
        }
        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("Total execution time to create " + objectCount + " objects in Java in millis: "
                + elapsedTime / 1000000);
    }
}
