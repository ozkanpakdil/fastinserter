package data.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import EPTFAssignment.solver.ServerEvent;

@Component
public class GenerateSampleJson {
	@Value("${jsonTestDataObjectCount:10000}")
	Long objectCount;

	public static void main(String[] args) throws Exception {
		GenerateSampleJson gsj = new GenerateSampleJson();
		if (StringUtils.isNoneBlank(args[0])) {
			System.out.println("Object count passed as parameter:" + args[0]);
			gsj.objectCount = Long.valueOf(args[0]);
		}
		if (gsj.objectCount == null)
			gsj.objectCount = 1000L;

		long startTime = System.nanoTime();
		gsj.generateJson(gsj.objectCount);
		long elapsedTime = System.nanoTime() - startTime;
		System.out.println("Total execution time to create " + gsj.objectCount + " objects in Java in millis: "
				+ elapsedTime / 1000000);
	}

	public void generateJson(Long i) throws IOException, JsonProcessingException {
		Random r = new Random();
		String host = "12345";
		String type = "APPLICATION_LOG";
		ObjectMapper mapper = new ObjectMapper();
		Path path = Paths.get("sample.json");
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			while (i-- > 0) {
				String id = RandomStringUtils.randomAlphabetic(10);
				Instant instant = Instant.now();
				long timestamp = instant.toEpochMilli();
				ServerEvent se = new ServerEvent(id, "STARTED", timestamp, type, host);
				writer.write(mapper.writeValueAsString(se) + System.lineSeparator());
				se = new ServerEvent(id, "FINISHED", timestamp + r.nextInt(10), null, null);
				writer.write(mapper.writeValueAsString(se) + System.lineSeparator());
			}
		}
	}
}
