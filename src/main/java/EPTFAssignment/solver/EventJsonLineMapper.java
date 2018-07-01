package EPTFAssignment.solver;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.LineMapper;

public class EventJsonLineMapper implements LineMapper<ServerEvent> {
	private static final Logger log = LoggerFactory.getLogger(EventJsonLineMapper.class);

	@Override
	public ServerEvent mapLine(String line, int lineNumber) {
		JSONObject jo = new JSONObject(line);
		ServerEvent se = null;
		if (!jo.isNull("type"))
			se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"),
					jo.getString("type"), jo.getString("host"));
		else
			se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"), null, null);

		ServerEvent checkEvent = IdUtils.getInstance().getIds().get(se.getId());
		// control if same ID event came before
		if (checkEvent != null) {
			long abs = Math.abs(se.getTimestamp() - checkEvent.getTimestamp());
			if (abs > 4) {
				se.setAlert(true);
			}
			IdUtils.getInstance().getIds().remove(se.getId());
		} else {
			IdUtils.getInstance().getIds().put(se.getId(), se);
		}
		if (lineNumber % 100000 == 0)
			log.info("Object prcessed:" + lineNumber + " list size:" + IdUtils.getInstance().getIds().size());
		return se;
	}

}
