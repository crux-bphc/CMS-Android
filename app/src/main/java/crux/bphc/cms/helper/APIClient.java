package crux.bphc.cms.helper;

import crux.bphc.cms.app.Constants;
import crux.bphc.cms.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by SKrPl on 11/23/17.
 */

public class APIClient {

    private static Retrofit retrofit = null;

    private static HttpLoggingInterceptor interceptor =
            new HttpLoggingInterceptor();
    private static OkHttpClient.Builder builder = new OkHttpClient.Builder();

    private APIClient() {
    }

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(interceptor);
            }

            retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(Constants.API_URL)
                    .client(builder.build())
                    .build();
        }
        return retrofit;
    }
}
