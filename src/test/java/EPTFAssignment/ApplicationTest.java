package EPTFAssignment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import EPTFAssignment.solver.Application;
import EPTFAssignment.solver.BatchConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@PropertySource("classpath:application.properties")
public class ApplicationTest {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	BatchConfiguration batchConfiguration;
	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	Job job;

	@Test
	public void runBatchInserter() throws Exception {
		assertThat(batchConfiguration).isNotNull();
		jobLauncher.run(job, new JobParameters());
		logger.info("ALL TESTS PASSED");
	}
}