package co.rob.api;

import co.rob.api.generated.invoker.ApiClient;

import java.net.http.HttpClient;
import java.time.Duration;

public class ApiClientWrapper extends ApiClient {

    public ApiClientWrapper() {
        // Override the HttpClient with one that has a shorter timeout
        super(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)),
                JacksonConfig.getMapper(),
                "http://localhost:8080/api");//TODO read in from statics props/override with Args
    }

}
