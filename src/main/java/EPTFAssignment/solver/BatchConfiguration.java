package EPTFAssignment.solver;

import javax.sql.DataSource;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BatchConfiguration.class);
    //used for reading chunk/paging
    int chunkSize = 100000;
    
    @Value(value = "${inFilePath}")
    String inFilePath;
    
    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    // tag::readerwriterprocessor[]
    @Bean
    public <T> FlatFileItemReader<T> reader() {
        FlatFileItemReader<T> reader = new FlatFileItemReader<T>();
        reader.setResource(new FileSystemResource(inFilePath));
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
    public Job importJsonJob(JobCompletionNotificationListener listener, Step step1) {
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

}
