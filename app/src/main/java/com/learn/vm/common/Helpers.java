package com.learn.vm.common;

import ua_parser.Parser;

public class Helpers {
    private static final Parser uaParser = new Parser();
    //http://www.java2s.com/example/java-utility-method/ip-address-to-int-index-0.html
    public static long ipToNum(String ip) {
        if (isNullOrEmpty(ip))
            return 0;

        try {
            String[] addrArray = ip.split("\\.");
            long num = 0;
            for (int i = 0; i < addrArray.length; i++) {
                num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, 3 - i)));
            }
            return num;
        } catch (Exception exp) {
            return 0;
        }
    }

    public static String getUA(String uaHeader) {
        if(uaHeader == null)
            return null;
        var ua = uaParser.parseUserAgent(uaHeader);

        if(ua != null && !isNullOrEmpty(ua.family))
            return ua.family.trim();
        return null;
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isBlank();
    }
}
