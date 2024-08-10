package com.wb.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 利用高德地图，通过经纬度获取用户所在省份
 */
public class Geocoder {

    private static final String KEY = ""; // 替换为你自己的高德地图 API Key

    /**
     * 获取经度、纬度对应的省份名称
     */
    public static String getProvince(double latitude, double longitude) throws Exception {
        String urlStr = "https://restapi.amap.com/v3/geocode/regeo?key=" + KEY +
                "&location=" + longitude + "," + latitude;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject json = JSONObject.parseObject(response.toString());
            JSONArray addressComponents = json.getJSONObject("regeocode")
                    .getJSONObject("addressComponent").getJSONArray("province");
            return addressComponents.getString(0);
        } else {
            throw new RuntimeException("获取省份信息失败，HTTP 错误码：" + responseCode);
        }
    }

}

