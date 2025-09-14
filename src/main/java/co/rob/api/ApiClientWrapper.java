package co.rob.api;

import co.rob.api.generated.invoker.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;

public class ApiClientWrapper extends ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClientWrapper.class);

    public ApiClientWrapper() {
        // Override the HttpClient with one that has a shorter timeout
        super(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)),
                JacksonConfig.getMapper(),
                "http://localhost:8080/api");
    }

}
