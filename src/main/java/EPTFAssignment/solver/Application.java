package EPTFAssignment.solver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration/*(
        exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class}
)*/
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static String inFilePath;

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("Json file path not provided.");
            System.exit(1);
        }
        inFilePath = args[0];
        File inFile = new File(inFilePath);
        if (!inFile.exists()) {
            log.error("Json file does not exist. Provided Path:" + inFilePath);
            System.exit(2);
        }
        Application.setInFilePath(args[0]);
        log.info("PATH:" + Application.getInFilePath());
        SpringApplication.run(Application.class, args);
    }

    /*@Bean
    @ConfigurationProperties(prefix = "batch")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }*/

    public static void setInFilePath(String inFilePath) {
        Application.inFilePath = inFilePath;
    }

    public static String getInFilePath() {
        return inFilePath;
    }

}
