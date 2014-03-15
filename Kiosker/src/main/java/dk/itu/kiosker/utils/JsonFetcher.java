package dk.itu.kiosker.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;

import dk.itu.kiosker.models.Constants;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;
import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

public class JsonFetcher {
    // Jackson mapper used to parse the json.
    public ObjectMapper mapper = new ObjectMapper(new JsonFactory()).configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    // The rest adapter that will call the server.
    private RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(Constants.JSON_BASE_URL)
            .setConverter(new JacksonConverter(mapper))
            .build();
    // The manager that ties everything together and lets us call our interface method.
    private JsonControllerService jsonController = restAdapter.create(JsonControllerService.class);

    // Get the json observable from our server.
    public Observable<LinkedHashMap> getObservableMap(String jsonPath) {
        return jsonController.getJson(jsonPath);
    }

    // Interface for the retrofit rest client.
    public interface JsonControllerService {
        @GET("/{json}")
        Observable<LinkedHashMap> getJson(@Path("json") String jsonPath);
    }
}