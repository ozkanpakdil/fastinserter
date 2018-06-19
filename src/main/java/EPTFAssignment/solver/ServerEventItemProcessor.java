package EPTFAssignment.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class ServerEventItemProcessor implements ItemProcessor<ServerEvent, ServerEvent> {

    private static final Logger log = LoggerFactory.getLogger(ServerEventItemProcessor.class);

    @Override
    public ServerEvent process(final ServerEvent event) throws Exception {

        final ServerEvent event1 = new ServerEvent(event.getId(), event.getState(), event.getTimestamp(), event.getType(), event.getHost(),event.getAlert());

        log.debug("EVENT:" + event.toString());

        return event1;
    }

}
