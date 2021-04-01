package com.bonc.dx.crawler_manage.pool.ip;



import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * @author wuqhh
 * @date 2021/1/22 11:38
 */
public class IpGetAndRelease {

    public static String getIp(String url) {
        String result = "";
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();

            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        System.out.println("ip" + result);
        return result;
    }

    /**
     * 　　sendUrl    （远程请求的URL）
     * 　　param    （远程请求参数）
     * 　　JSONObject    （远程请求返回的JSON）
     */
    public static Object releaseIp(String url){

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = null;
        try {
            post = new HttpPost(url);
            //http
            HttpResponse response = httpClient.execute(post);

            JSONObject jsonObject = new JSONObject();
            System.out.println(response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();

                String resJson = EntityUtils.toString(entity);
                jsonObject = JSONObject.parseObject(resJson);
            }

            //结果
            if (jsonObject != null ) {
                return jsonObject;
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                if (post != null) {
                    post.releaseConnection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    public static void main(String[] args) {
        releaseIp("https://api.xiaoxiangdaili.com/ip/release?appKey=668299180825268224&appSecret=Wz9nGgoH&proxy=" +
                "122.239.137.188:5021");
    }
}
