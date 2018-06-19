package EPTFAssignment.solver;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GsonConfig {

    @Bean
    public Gson gson() {
        return new Gson();
    }

    public ServerEvent returnMap(String e) {
        return gson().fromJson(e, ServerEvent.class);
    }
}