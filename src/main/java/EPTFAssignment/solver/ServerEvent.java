package EPTFAssignment.solver;

import com.google.gson.annotations.Expose;


public class ServerEvent {
    @Expose
    String id, state, type, host;
    @Expose
    Long timestamp;
    @Expose
    Boolean alert=null;

    public ServerEvent(String id, String state, Long timestamp, String type, String host, Boolean alert) {
        this.id = id;
        this.state = state;
        this.timestamp = timestamp;
        this.type = type;
        this.host = host;
        this.alert = alert;
    }

    public ServerEvent(String id, String state, long timestamp, String type, String host) {
        this.id = id;
        this.state = state;
        this.timestamp = timestamp;
        this.type = type;
        this.host = host;
    }

    @Override
    public String toString() {
        return "ServerEvent{" +
                "id='" + id + '\'' +
                ", state='" + state + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", type='" + type + '\'' +
                ", host='" + host + '\'' +
                ", alert=" + alert +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public Boolean getAlert() {
        return alert;
    }

    public void setAlert(boolean b) {
        this.alert = b;
    }
}


