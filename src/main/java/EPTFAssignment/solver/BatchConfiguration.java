package EPTFAssignment.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.separator.JsonRecordSeparatorPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BatchConfiguration.class);
    //used for reading chunk/paging
    int chunkSize = 100000;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    // tag::readerwriterprocessor[]
    @Bean
    public <T> FlatFileItemReader<T> reader() {
        FlatFileItemReader<T> reader = new FlatFileItemReader<T>();
        reader.setResource(new FileSystemResource(Application.getInFilePath()));
        reader.setRecordSeparatorPolicy(new JsonRecordSeparatorPolicy());
        reader.setLineMapper((LineMapper<T>) new EventJsonLineMapper());

        return reader;
    }

    @Bean
    public ServerEventItemProcessor processor() {
        return new ServerEventItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<ServerEvent> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ServerEvent>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO event (id,state,timestamp,type,host,alert) VALUES (:id,:state,:timestamp,:type,:host,:alert)")
                .dataSource(dataSource)
                .build();
    }
    // end::readerwriterprocessor[]

    // tag::jobstep[]
    @Bean
    public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
        return jobBuilderFactory.get("importJsonJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1(JdbcBatchItemWriter<ServerEvent> writer) {
        return stepBuilderFactory.get("step1")
                .<ServerEvent, ServerEvent>chunk(chunkSize)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .build();
    }
    // end::jobstep[]

    @Scheduled(fixedRate = 5000 * 12)
    public void printCacheStats() {
        log.info("IdUtils.getInstance().getIds().size()" + IdUtils.getInstance().getIds().size());
    }
}
