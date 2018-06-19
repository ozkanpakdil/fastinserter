import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	// static Queue<String> idQueue = new ConcurrentLinkedQueue<String>();
	static int cpuCount = Runtime.getRuntime().availableProcessors();
	private final static ArrayBlockingQueue<String>[] idQueue = new ArrayBlockingQueue[cpuCount];

	static Connection conn;
	static String dbFileName = "./hsqldbfastsol/data;hsqldb.log_data=false;hsqldb.default_table_type=CACHED;hsqldb.nio_data_file=true;hsqldb.nio_max_size=1024m";
	static Statement st = null;
	static int dbCounter = 0;
	static Thread readerThread = null;
	static BufferedWriter writer = null;
	static ExecutorService executor = null;

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
		writer = new BufferedWriter(new FileWriter("output.sql"));
		for (int i = 0; i < idQueue.length; i++) {
			idQueue[i] = new ArrayBlockingQueue<>(cpuCount * 1000);
		}
		log.info("CPU count:" + Runtime.getRuntime().availableProcessors());
		executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		startFileReader(inFile);

	}

	private static void startDBInserter() throws Exception {
		Runnable task = () -> {
			/*
			 * openDbConnection(); try {
			 * st.execute("SET FILES LOG FALSE;DROP TABLE event IF EXISTS;\n" +
			 * "CREATE TABLE IF NOT EXISTS event (\n" + "    id VARCHAR(50) ,\n" +
			 * "    state VARCHAR(50),\n" + "    timestamp BIGINT,\n" +
			 * "    type VARCHAR(50),\n" + "    host VARCHAR(50),\n" + "    alert BOOLEAN\n"
			 * + ");\n"); conn.commit(); conn.setAutoCommit(false); } catch (Exception e) {
			 * e.printStackTrace(); System.exit(1); } log.info("TABLE CREATED:");
			 */
			long lineCounter = 0;
			int threadId = (int) Thread.currentThread().getId()%cpuCount;
			while (idQueue[threadId].size() > 0) {
				String id = idQueue[threadId].poll();
				if (id == null) {
					System.out.println("id bitti");
				}
				ServerEvent seStarted = eventsFromFile.get(id + "STARTED");
				ServerEvent seFinished = eventsFromFile.get(id + "FINISHED");

				if (seFinished != null && seStarted != null) {
					long abs = Math.abs(seFinished.getTimestamp() - seStarted.getTimestamp());
					if (abs > 4) {
						seStarted.setAlert(true);
						seFinished.setAlert(true);
					}
					insert("INSERT INTO event(id,state,timestamp,type,host,alert) VALUES ('" + seStarted.getId() + "','"
							+ seStarted.getState() + "'," + seStarted.getTimestamp() + ",'" + seStarted.getType()
							+ "','" + seStarted.getHost() + "'," + seStarted.getAlert() + ");");
					insert("INSERT INTO event(id,state,timestamp,type,host,alert) VALUES ('" + seFinished.getId()
							+ "','" + seFinished.getState() + "'," + seFinished.getTimestamp() + ",'"
							+ seFinished.getType() + "','" + seFinished.getHost() + "'," + seFinished.getAlert()
							+ ");");
					eventsFromFile.remove(seStarted.getId() + seStarted.getState());
					eventsFromFile.remove(seFinished.getId() + seFinished.getState());
					if (lineCounter++ % 10000 == 0) {
						log.info("DBinserter at line:" + lineCounter + " eventsFromFile:" + eventsFromFile.size()
								+ " idQueue:" + idQueue[threadId].size());
					}

				}

			}
			// shutdownDB();
		};
		// Thread dbinserter = new Thread(task);
		// dbinserter.start();
		executor.execute(task);
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
		try {
			writer.write(expression + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(2);
		}
		/*
		 * try { st.addBatch(expression); if(dbCounter++%10000==0) st.executeBatch(); }
		 * catch (SQLException e) { log.error(expression); e.printStackTrace();
		 * System.exit(1); }
		 */
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
			LineIterator it = null;
			try {
				it = FileUtils.lineIterator(inFile, "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
			long lineCounter = 0;
			while (it.hasNext()) {
				String line = it.nextLine();
				JSONObject jo = new JSONObject(line);
				ServerEvent se = null;
				if (jo.has("type"))
					se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"),
							jo.getString("type"), jo.getString("host"));
				else
					se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"), null,
							null);

				eventsFromFile.put(se.getId() + se.getState(), se);
				// if(!idQueue.contains(se.getId()))
				idQueue[(int) (lineCounter%cpuCount)].offer(se.getId());
				if (lineCounter++ % 10000 == 0) {
					log.info("Filereader at line:" + lineCounter);
					try {
						startDBInserter();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				// if (idQueue.size() - 1000 > cpuCount * 1000) {
				// try {
				// Thread.sleep(100);
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				// }

			}
		};

		readerThread = new Thread(task);
		readerThread.start();

		log.info("Json file reader started!");

	}
}
