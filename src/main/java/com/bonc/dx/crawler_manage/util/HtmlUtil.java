package com.bonc.dx.crawler_manage.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * https://developer.aliyun.com/article/144087
 * 去除所有html标签
 */
public class HtmlUtil {

    public static String deatilHtml(String htmlStr){

        if (htmlStr == null){
            return "";
        } else {
            String textStr = "";
            Pattern p_script;
            Matcher m_script;
            Pattern p_style;
            Matcher m_style;
            Pattern p_html;
            Matcher m_html;
            Pattern p_special;
            Matcher m_special;
            try {
                //定义script的正则表达式{或<script[^>]*?>[\\s\\S]*?<\\/script>
                String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>";
                //定义style的正则表达式{或<style[^>]*?>[\\s\\S]*?<\\/style>
                String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>";
                // 定义HTML标签的正则表达式
                String regEx_html = "<[^>]+>";
                // 定义一些特殊字符的正则表达式 如：
                String regEx_special = "\\&[a-zA-Z]{1,10};";

                p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
                m_script = p_script.matcher(htmlStr);
                // 过滤script标签
                htmlStr = m_script.replaceAll("");
                p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
                m_style = p_style.matcher(htmlStr);
                // 过滤style标签
                htmlStr = m_style.replaceAll("");
                p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
                m_html = p_html.matcher(htmlStr);
                // 过滤html标签
                htmlStr = m_html.replaceAll("");
                p_special = Pattern.compile(regEx_special, Pattern.CASE_INSENSITIVE);
                m_special = p_special.matcher(htmlStr);
                // 过滤特殊标签
                htmlStr = m_special.replaceAll("");
                textStr = htmlStr;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return textStr.replaceAll("\r\n|\r|\n", " ");
        }
    }
}
