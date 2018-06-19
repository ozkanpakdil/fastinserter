package EPTFAssignment.solver;

import java.util.HashMap;

public class IdUtils {

    private static IdUtils sInstance;

    private HashMap<String, ServerEvent> ids;

    private IdUtils() {
        ids = new HashMap<>();
    }

    public static IdUtils getInstance() {
        if (sInstance == null) {
            sInstance = new IdUtils();
        }
        return sInstance;
    }

    public HashMap<String, ServerEvent> getIds() {
        return ids;
    }

}