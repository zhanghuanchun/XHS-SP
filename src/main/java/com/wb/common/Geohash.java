package com.wb.common;

public class Geohash {

    private static final int PRECISION = 12;
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    /**
     * 编码经纬度坐标为 geohash 字符串
     *
     * @param latitude 纬度（-90 到 90 度之间）
     * @param longitude 经度（-180 到 180 度之间）
     * @return geohash 字符串
     */
    public static String encode(double latitude, double longitude) {
        StringBuilder hash = new StringBuilder();
        double[] minValues = {-90.0, -180.0};
        double[] maxValues = {90.0, 180.0};
        boolean isEven = true;

        for (int i = 0; i < PRECISION; i++) {
            double midValue = (minValues[i % 2] + maxValues[i % 2]) / 2;
            if (isEven) {
                if (longitude > midValue) {
                    hash.append('1');
                    minValues[1] = midValue;
                } else {
                    hash.append('0');
                    maxValues[1] = midValue;
                }
            } else {
                if (latitude > midValue) {
                    hash.append('1');
                    minValues[0] = midValue;
                } else {
                    hash.append('0');
                    maxValues[0] = midValue;
                }
            }
            isEven = !isEven;
        }

        return hash.toString();
    }

    /**
     * 解码 geohash 字符串为经纬度坐标
     *
     * @param hash geohash 字符串
     * @return 经纬度坐标数组 [latitude, longitude]
     */
    public static double[] decode(String hash) {
        double[] minValues = {-90.0, -180.0};
        double[] maxValues = {90.0, 180.0};
        boolean isEven = true;
        double latitude, longitude;

        for (int i = 0; i < hash.length(); i++) {
            int charIndex = BASE32.indexOf(hash.charAt(i));
            for (int j = 4; j >= 0; j--) {
                int bit = (charIndex >> j) & 1;
                if (isEven) {
                    if (bit == 1) {
                        minValues[1] = (minValues[1] + maxValues[1]) / 2;
                    } else {
                        maxValues[1] = (minValues[1] + maxValues[1]) / 2;
                    }
                } else {
                    if (bit == 1) {
                        minValues[0] = (minValues[0] + maxValues[0]) / 2;
                    } else {
                        maxValues[0] = (minValues[0] + maxValues[0]) / 2;
                    }
                }
                isEven = !isEven;
            }
        }

        latitude = (minValues[0] + maxValues[0]) / 2;
        longitude = (minValues[1] + maxValues[1]) / 2;
        return new double[]{latitude, longitude};
    }

    // 根据经纬度计算两个坐标之间的距离
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径，单位：km

        double dLat = toRadians(lat2 - lat1);
        double dLon = toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // 将角度转换为弧度
    private static double toRadians(double degree) {
        return degree * Math.PI / 180;
    }
}

