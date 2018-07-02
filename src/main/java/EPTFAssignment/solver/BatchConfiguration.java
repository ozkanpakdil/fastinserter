package EPTFAssignment.solver;

import java.net.MalformedURLException;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

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
    
    @Value("org/springframework/batch/core/schema-drop-sqlite.sql")
    private Resource dropReopsitoryTables;
 
    @Value("org/springframework/batch/core/schema-sqlite.sql")
    private Resource dataReopsitorySchema;
    
    @Value("schema-all.sql")
    private Resource schemaSql;
 
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:mydb.db");
        return dataSource;
    }
    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource)
      throws MalformedURLException {
        ResourceDatabasePopulator databasePopulator = 
          new ResourceDatabasePopulator();
 
        databasePopulator.addScript(dropReopsitoryTables);
        databasePopulator.addScript(dataReopsitorySchema);
        
        databasePopulator.addScript(schemaSql);
        databasePopulator.setIgnoreFailedDrops(true);
 
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator);
 
        return initializer;
    }
}
