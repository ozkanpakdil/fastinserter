package EPTFAssignment.solver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import EPTFAssignment.solver.ServerEvent;

@Component
public class FasterSolution {
	private static final Logger log = LoggerFactory.getLogger(FasterSolution.class);
	private String inFilePath;
	ConcurrentHashMap<String, ServerEvent> eventsFromFile = new ConcurrentHashMap();
	int cpuCount = Runtime.getRuntime().availableProcessors();
	int maxPoolSize = cpuCount * 20;
	TransferQueue<ServerEvent> idQueue = new LinkedTransferQueue();

	Connection conn;
	String dbFileName = "./hsqldbfastsol/data;hsqldb.log_data=false;hsqldb.default_table_type=CACHED;hsqldb.nio_data_file=true;hsqldb.nio_max_size=1024m";
	Statement st = null;
	int dbCounter = 0;
	Thread readerThread = null;
	ThreadPoolExecutor executor = null;

	public void run(String filePath) throws Exception {
		inFilePath = filePath;
		File inFile = new File(inFilePath);
		openDbConnection();
		try {
			st.execute("SET FILES LOG FALSE;DROP TABLE event IF EXISTS;\n" + "CREATE TABLE IF NOT EXISTS event (\n"
					+ "    id VARCHAR(50) ,\n" + "    state VARCHAR(50),\n" + "    timestamp BIGINT,\n"
					+ "    type VARCHAR(50),\n" + "    host VARCHAR(50),\n" + "    alert BOOLEAN\n" + ");\n");
			// st.execute("CREATE UNIQUE INDEX uq_event ON event(id, state);");
			conn.commit();
			conn.setAutoCommit(false);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		log.info("TABLE CREATED");

		log.info("CPU count:" + Runtime.getRuntime().availableProcessors());
		executor = new ThreadPoolExecutor(cpuCount, maxPoolSize, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		startFileReader(inFile);
		startDBInserter();
		while (!executor.isTerminated()) {
			Thread.sleep(2000);
			log.info("eventsFromFile:" + eventsFromFile.size() + " readers getCompletedTaskCount:"
					+ executor.getCompletedTaskCount() + " queue:" + idQueue.size() + " active:"
					+ executor.getActiveCount() + " readerQ:" + executor.getQueue().size());

		}
		conn.commit();
		ResultSet r = st.executeQuery("SELECT COUNT(*) FROM event");
		r.next();
		log.info("object count in db " + r.getString(1));
		shutdownDB();
	}

	void process(String id) {
		ServerEvent seStarted = eventsFromFile.get(id + "STARTED");
		ServerEvent seFinished = eventsFromFile.get(id + "FINISHED");

		if (seFinished != null && seStarted != null) {
			long abs = Math.abs(seFinished.getTimestamp() - seStarted.getTimestamp());
			if (abs > 4) {
				seStarted.setAlert(true);
				seFinished.setAlert(true);
			}
			insert("INSERT INTO event(id,state,timestamp,type,host,alert) VALUES ('" + seStarted.getId() + "','"
					+ seStarted.getState() + "'," + seStarted.getTimestamp() + ",'" + seStarted.getType() + "','"
					+ seStarted.getHost() + "'," + seStarted.getAlert() + ");");
			insert("INSERT INTO event(id,state,timestamp,type,host,alert) VALUES ('" + seFinished.getId() + "','"
					+ seFinished.getState() + "'," + seFinished.getTimestamp() + ",'" + seFinished.getType() + "','"
					+ seFinished.getHost() + "'," + seFinished.getAlert() + ");");
			eventsFromFile.remove(seStarted.getId() + seStarted.getState());
			eventsFromFile.remove(seFinished.getId() + seFinished.getState());

		}
	}

	void startDBInserter() throws Exception {
		if (executor.getActiveCount() >= cpuCount)
			return;
		Runnable task = () -> {
			while (Thread.currentThread().isAlive()) {
				if (idQueue.remainingCapacity() == 0) {
					Collection<ServerEvent> all = new ArrayList();
					idQueue.drainTo(all);
					all.stream().forEach(se -> eventsFromFile.putIfAbsent(se.getId() + se.getState(), se));
					all.stream().forEach(se -> process(se.getId()));
				} else {
					ServerEvent se = idQueue.poll();

					if (se != null) {
						eventsFromFile.putIfAbsent(se.getId() + se.getState(), se);
						process(se.getId());
					}
				}
				if (eventsFromFile.size() == 0 && idQueue.size() == 0)
					break;
				if (eventsFromFile.size() > 0 && idQueue.size() == 0)
					eventsFromFile.values().stream().forEach(se -> process(se.getId()));
			}
		};
		if (!executor.isShutdown())
			executor.execute(task);

	}

	public void openDbConnection() {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			conn = DriverManager.getConnection("jdbc:hsqldb:file:" + dbFileName, "SA", "");
			st = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void insert(String expression) {
		try {
			st.addBatch(expression);
			if (++dbCounter % (cpuCount * 1000) == 0) {
				st.executeBatch();
				conn.commit();
			}
		} catch (SQLException e) {
			log.error(expression);
			e.printStackTrace();
			System.exit(1);
		}

	}

	public void shutdownDB() {
		Statement st;
		try {
			st = conn.createStatement();
			st.execute("SHUTDOWN");
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	void startFileReader(File inFile) throws Exception {
		Runnable task = () -> {
			try {
				LineIterator it = FileUtils.lineIterator(inFile, "UTF-8");
				long lineCounter = 0;
				while (it.hasNext()) {
					String line = it.nextLine();
					JSONObject jo = new JSONObject(line);
					ServerEvent se = null;
					if (jo.isNull("type"))
						se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"), null,
								null);
					else
						se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"),
								jo.getString("type"), jo.getString("host"));

					idQueue.put(se);
					if (++lineCounter % (cpuCount * 10000) == 0) {
						startDBInserter();
						log.info("Filereader at line:" + lineCounter);
						// Thread.sleep(eventsFromFile.size() / 10);
						// eventsFromFile.values().parallelStream().forEach(v -> process(v.getId()));
					}
					se = null;
				}
				log.info("File read finished");
				executor.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		readerThread = new Thread(task);
		readerThread.start();

		log.info("Json file reader started!");

	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		FasterSolution f = new FasterSolution();
		try {
			f.run(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("elapsedTime:" + elapsedTime / 1000 + " secs");

		}
	}
}
