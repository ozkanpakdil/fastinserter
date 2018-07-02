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
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
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
	TransferQueue<ServerEvent> idQueue = new LinkedTransferQueue();
	List<Consumer> futuresList = new ArrayList<>();

	Thread readerThread = null;
	ThreadPoolExecutor executor = null;
	Connection conn;
	String dbFileName = "mydb.db";
	Statement st = null;
	int dbCounter = 0;

	public void run(String filePath) throws Exception {
		inFilePath = filePath;
		File inFile = new File(inFilePath);
		openDbConnection();
		try {
			st.execute( "CREATE TABLE event ("
					+ "    id TEXT ," + "    state TEXT," + "    timestamp BIGINT,"
					+ "    type TEXT," + "    host TEXT," + "    alert BOOLEAN" + ");");
			// st.execute("CREATE UNIQUE INDEX uq_event ON event(id, state);");
			
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		log.info("TABLE CREATED");
		log.info("CPU count:" + Runtime.getRuntime().availableProcessors());
		executor = new ThreadPoolExecutor(1, 2, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		log.info("executor.isShutdown():" + executor.isShutdown());

		startReaders();
		startReaders();
//		startReaders();
//		startReaders();
//		startReaders();
//		startReaders();
//		startReaders();
//		startReaders();
		startFileReader(inFile);
		while (!executor.isTerminated()) {
			Thread.sleep(2000);
			log.info(" readers getCompletedTaskCount:" + executor.getCompletedTaskCount() + " queue:" + idQueue.size()
					+ " active:" + executor.getActiveCount() + " readerQ:" + executor.getQueue().size());
			if (!readerThread.isAlive()) {
				log.info("READER NOT ALIVE");
				ConcurrentHashMap<String, ServerEvent> ll = new ConcurrentHashMap<>();
				for (Consumer f : futuresList) {
					log.info("left:" + f.getEventsFromFile().size());
					ll.putAll(f.getEventsFromFile());
				}
				Consumer c = new Consumer(idQueue, this);
				c.setMap(ll);
				ll.values().parallelStream().forEach(v -> c.process(v.getId()));
				executor.shutdownNow();
			}
		}
		shutdownDB();
	}

	void startReaders() {
		Consumer c = new Consumer(idQueue, this);
		futuresList.add(c);
		executor.submit(c);
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
					idQueue.transfer(se);
					if (++lineCounter %  100000 == 0) {
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
		task.run();
		log.info("Json file reader started!");

	}

	public void openDbConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:" + dbFileName, "sa", "sa");
			st = conn.createStatement();
			conn.setAutoCommit(false);
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void insert(String expression) {
		try {
			st.addBatch(expression);
			if (++dbCounter %  100000 == 0) {
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
			conn.commit();
			ResultSet r = st.executeQuery("SELECT COUNT(*) FROM event");
			r.next();
			log.info("object count in db " + r.getString(1));
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
