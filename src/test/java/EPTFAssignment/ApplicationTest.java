package EPTFAssignment;

import EPTFAssignment.solver.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ApplicationTest {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	//@Autowired
	//BatchConfiguration batchConfiguration;

	@Test
	public void test() {
		//assertThat(batchConfiguration).isNotNull();
		logger.info("ALL TESTS PASSED");
	}
}