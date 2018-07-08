package EPTFAssignment.solver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FasterSolution {
	private static final Logger log = LoggerFactory.getLogger(FasterSolution.class);
	private String inFilePath;
	ArrayBlockingQueue<ServerEvent> idQueue = new ArrayBlockingQueue(1000000);
	List<Consumer> futuresList = new ArrayList<>();

	Thread readerThread = null;
	ThreadPoolExecutor executor = null;
	Connection conn;
	public static String dbFileName = "//localhost:3306/test?useSSL=false";
	Statement st = null;
	int dbCounter = 0;

	public void run(String filePath) throws Exception {
		inFilePath = filePath;
		File inFile = new File(inFilePath);
		openDbConnection();
		try {
			st.execute("drop table if exists test.event;");
			st.execute("SET GLOBAL binlog_format = 'ROW';");
			st.execute("SET GLOBAL TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;");		
			st.execute("SET GLOBAL concurrent_insert = 2;");
			st.execute("CREATE TABLE test.event (id varchar(15) ,state varchar(10),timestamp BIGINT,"
					+ "    type varchar(15)," + "    host varchar(10)," + "    alert BOOLEAN" + ") ENGINE = MYISAM;");

			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		log.info("TABLE CREATED");
		log.info("CPU count:" + Runtime.getRuntime().availableProcessors());
		executor = new ThreadPoolExecutor(32, 32, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		log.info("executor.isShutdown():" + executor.isShutdown());

		startFileReader(inFile);
		while (!executor.isTerminated()) {
			Thread.sleep(2000);
			loginfo();
			if (!readerThread.isAlive()) {
				log.info("READER NOT ALIVE");
				//executor.shutdownNow();
			}
		}
		shutdownDB();
	}

	public void loginfo() {
		log.info(" readers getCompletedTaskCount:" + executor.getCompletedTaskCount() + " queue:" + idQueue.size()
				+ " active:" + executor.getActiveCount() + " readerQ:" + executor.getQueue().size());
	}

	void startInserter() {
		Consumer c = new Consumer(idQueue);
		futuresList.add(c);
		executor.execute(c);
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
					//log.info("sending");
					idQueue.put(se);

					if (++lineCounter % 100000 == 0) {
						log.info("Filereader at line:" + lineCounter);
						startInserter();
						startInserter();
						loginfo();
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
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql:" + dbFileName, "root", "sa");
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
			st.execute(expression);
			/*
			 * st.addBatch(expression); if (++dbCounter % 100000 == 0) { st.executeBatch();
			 * conn.commit(); }
			 */
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
