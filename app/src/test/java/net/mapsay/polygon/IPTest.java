package net.mapsay.polygon;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 卞河儒
 * @email bianheru@mapsay.net
 * @desc (描述)
 * @date 2019-05-15
 **/
public class IPTest {

    @Test
    public void testMac() {
        try {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>mac:" + getMacAddress("192.168.1.1"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行单条指令
     *
     * @param cmd 命令
     * @return 执行结果
     * @throws Exception
     */
    public static String command(String cmd) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
        InputStream in = process.getInputStream();
        StringBuilder result = new StringBuilder();
        byte[] data = new byte[256];
        while (in.read(data) != -1) {
            String encoding = System.getProperty("sun.jnu.encoding");
            result.append(new String(data, encoding));
        }
        return result.toString();
    }


    /**
     * 获取mac地址
     *
     * @param ip
     * @return
     * @throws Exception
     */
    public static String getMacAddress(String ip) throws IOException, InterruptedException {
        String result = command("ping " + ip + " -n 2");
        if (result.contains("TTL")) {
            result = command("arp -a " + ip);
        }
        String regExp = "([0-9A-Fa-f]{2})([-:][0-9A-Fa-f]{2}){5}";
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(result);
        StringBuilder mac = new StringBuilder();
        while (matcher.find()) {
            String temp = matcher.group();
            mac.append(temp);
        }
        return mac.toString();
    }

}
