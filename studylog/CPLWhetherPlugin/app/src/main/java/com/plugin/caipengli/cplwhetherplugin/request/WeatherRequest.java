package com.plugin.caipengli.cplwhetherplugin.request;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by caipengli on 16年4月1日.
 */
public class WeatherRequest {
    public static final String SERVICE_BASE_URL = "http://wthrcdn.etouch.cn/weather_mini?";
    // http://wthrcdn.etouch.cn/weather_mini?city=北京
    public static final String RESULT_ERROR = "requestError";

    public static String requestWeather(String city) {
        String result = null;
        try {
            String urlStr = SERVICE_BASE_URL + "city=" + city;
            URL url = new URL(new String(urlStr.getBytes("UTF-8"),"UTF-8"));
            Log.i("weather","url : " + new String(urlStr.getBytes(),"UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "text/html");
            conn.setRequestProperty("Accept-Charset", "utf-8");
            conn.setRequestProperty("contentType", "utf-8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.connect();
            if (HttpURLConnection.HTTP_OK == conn.getResponseCode()) {
                result = readStream(conn.getInputStream());
                return result;
            } else {
                return RESULT_ERROR;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String readStream(InputStream in) throws IOException {
        String result;
        byte [] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int length = -1;
        while ((length = in.read(buffer)) > 0) {
            sb.append(new String(buffer, 0, length, "UTF-8"));
        }
        result = sb.toString();
        in.close();
        return result;
    }
}
