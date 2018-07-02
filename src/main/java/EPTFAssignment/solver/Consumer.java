package EPTFAssignment.solver;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TransferQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer implements Callable<ConcurrentHashMap<String, ServerEvent>> {
	private static final Logger log = LoggerFactory.getLogger(Consumer.class);
	int cpuCount = Runtime.getRuntime().availableProcessors();
	public ConcurrentHashMap<String, ServerEvent> eventsFromFile = new ConcurrentHashMap();

	public ConcurrentHashMap<String, ServerEvent> getEventsFromFile() {
		return eventsFromFile;
	}

	protected TransferQueue<ServerEvent> transferQueue;
	FasterSolution fs;

	public Consumer(TransferQueue<ServerEvent> queue, FasterSolution fasterSolution) {
		this.transferQueue = queue;
		this.fs = fasterSolution;
	}

	public void process(String id) {
		ServerEvent seStarted = eventsFromFile.get(id + "STARTED");
		ServerEvent seFinished = eventsFromFile.get(id + "FINISHED");

		if (seFinished != null && seStarted != null) {
			long abs = Math.abs(seFinished.getTimestamp() - seStarted.getTimestamp());
			if (abs > 4) {
				seStarted.setAlert(true);
				seFinished.setAlert(true);
			}
			fs.insert("INSERT INTO event(id,state,timestamp,type,host,alert) VALUES ('" + seStarted.getId() + "','"
					+ seStarted.getState() + "'," + seStarted.getTimestamp() + ",'" + seStarted.getType() + "','"
					+ seStarted.getHost() + "','" + seStarted.getAlert() + "');");
			fs.insert("INSERT INTO event(id,state,timestamp,type,host,alert) VALUES ('" + seFinished.getId() + "','"
					+ seFinished.getState() + "'," + seFinished.getTimestamp() + ",'" + seFinished.getType() + "','"
					+ seFinished.getHost() + "','" + seFinished.getAlert() + "');");
			eventsFromFile.remove(seStarted.getId() + seStarted.getState());
			eventsFromFile.remove(seFinished.getId() + seFinished.getState());

		}
	}

	@Override
	public ConcurrentHashMap<String, ServerEvent> call() throws Exception {
		long counter = 0;
		Thread.sleep(2000);
		while (Thread.currentThread().isAlive()) {
			try {
				ServerEvent se = transferQueue.take();
				eventsFromFile.putIfAbsent(se.getId() + se.getState(), se);
				process(se.getId());
				if (fs.executor.isShutdown()) {
					log.info("executor.isShutdown():"+fs.executor.isShutdown());
					break;
				}
//				if (++counter % 10000 == 0) {
//					log.info("eventsFromFile:" + eventsFromFile.size());
//				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return eventsFromFile;
	}

	public void setMap(ConcurrentHashMap<String, ServerEvent> ll) {
		this.eventsFromFile = ll;
	}

}
