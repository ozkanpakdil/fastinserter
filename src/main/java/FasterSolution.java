import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import EPTFAssignment.solver.ServerEvent;

public class FasterSolution {
	private static final Logger log = LoggerFactory.getLogger(FasterSolution.class);
	private static String inFilePath;
	static ConcurrentMap<String, ServerEvent> eventsFromFile = new ConcurrentHashMap<>();
	static int cpuCount = Runtime.getRuntime().availableProcessors();
	// static Queue<String>[] idQueue = new ConcurrentLinkedQueue[cpuCount];
	// private final static LinkedBlockingQueue<String>[] idQueue = new
	// LinkedBlockingQueue[cpuCount];
	// private final static ArrayBlockingQueue<String> idQueue = new
	// ArrayBlockingQueue(cpuCount*100000);
	private final static ArrayBlockingQueue<String>[] idQueue = new ArrayBlockingQueue[cpuCount];

	static Connection conn;
	static String dbFileName = "./hsqldbfastsol/data;hsqldb.log_data=false;hsqldb.default_table_type=CACHED;hsqldb.nio_data_file=true;hsqldb.nio_max_size=1024m";
	static Statement st = null;
	static int dbCounter = 0;
	static Thread readerThread = null;
	static BufferedWriter writer = null;
	static ThreadPoolExecutor executor = null;

	public static void main(String[] args) throws Exception {
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
		//writer = new BufferedWriter(new FileWriter("output.sql"));

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

		for (int i = 0; i < idQueue.length; i++) {
			idQueue[i] = new ArrayBlockingQueue<>(cpuCount * 10000);
			// idQueue[i] = new ConcurrentLinkedQueue<>();
			// idQueue[i]=new LinkedBlockingQueue<>();
		}

		log.info("CPU count:" + Runtime.getRuntime().availableProcessors());
		executor = new ThreadPoolExecutor(1, cpuCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		startFileReader(inFile);
		while (!executor.isTerminated()) {
			Thread.sleep(1000);
			if (executor.getQueue().size() < cpuCount)
				for (int i = 0; i < cpuCount; i++)
					startDBInserter(i);
		}
conn.commit();
		ResultSet r = st.executeQuery("SELECT COUNT(*) FROM event");
		r.next();
		log.info("object count in db " + r.getString(1));
		shutdownDB();
	}

	private static void process(String id) {
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

	private static void startDBInserter(int threadId) throws Exception {
		if (executor.getQueue().size() < cpuCount) {
			Runnable task = () -> {
				try {
					long lineCounter = 0;
					while (idQueue[threadId].size() > 0) {
						process(idQueue[threadId].poll());
						if (++lineCounter % 100000 == 0) {
							log.info("THREADID:" + threadId + " DBinserter at line:" + lineCounter + " eventsFromFile:"
									+ eventsFromFile.size() + " idQueue:" + idQueue[threadId].size());
						}
						if (eventsFromFile.size() == 0) {
							idQueue[threadId].clear();
							System.gc();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			};
			if (!executor.isShutdown())
				executor.execute(task);
		}
	}

	public static void openDbConnection() {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			conn = DriverManager.getConnection("jdbc:hsqldb:file:" + dbFileName, "SA", "");
			st = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void insert(String expression) {
		// try {
		// writer.write(expression + "\n");
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// System.exit(2);
		// }

		try {
			// st.execute(expression);
			st.addBatch(expression);
			if (++dbCounter % (cpuCount * 1000) == 0){
				st.executeBatch();
                conn.commit();
            }
		} catch (SQLException e) {
			log.error(expression);
			e.printStackTrace();
			System.exit(1);
		}

	}

	public static void shutdownDB() {
		Statement st;
		try {
			st = conn.createStatement();
			st.execute("SHUTDOWN");
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void startFileReader(File inFile) throws Exception {
		Runnable task = () -> {
			try {
				LineIterator it = FileUtils.lineIterator(inFile, "UTF-8");
				long lineCounter = 0;
				while (it.hasNext()) {
					String line = it.nextLine();
					JSONObject jo = new JSONObject(line);
					ServerEvent se = null;
					int threadId = (int) (lineCounter % cpuCount);
					if (jo.has("type"))
						se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"),
								jo.getString("type"), jo.getString("host"));
					else
						se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"), null,
								null);

					eventsFromFile.putIfAbsent(se.getId() + se.getState(), se);
					//if (!idQueue[threadId].contains(se.getId()))
						idQueue[threadId].put(se.getId());
					if (++lineCounter % (cpuCount * 10000) == 0) {
						log.info("Filereader at line:" + lineCounter);
						//Thread.sleep(eventsFromFile.size() / 10);
					}
					se = null;
				}
				// file read ended
				log.info("File read finished");
				for (ServerEvent se : eventsFromFile.values()) {
					process(se.getId());
				}
				executor.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		readerThread = new Thread(task);
		readerThread.start();

		log.info("Json file reader started!");

	}
}
