package EPTFAssignment.solver.jms;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import EPTFAssignment.solver.ServerEvent;

@Component
public class Solver {
	@Autowired 
	JmsTemplate jmsTemplate;
	
    @Value(value = "${inFilePath}")
    String inFilePath;
	public void send(String line) {
		JSONObject jo = new JSONObject(line);
		ServerEvent se = null;
		if (jo.isNull("type"))
			se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"), null,
					null);
		else
			se = new ServerEvent(jo.getString("id"), jo.getString("state"), jo.getLong("timestamp"),
					jo.getString("type"), jo.getString("host"));
	    jmsTemplate.convertAndSend("Queue", se);
	}
}
