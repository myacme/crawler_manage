/**
 * FileName: DateUtil
 * <p>
 * Author:   liujixiang
 * <p>
 * Date:     2021/2/2 9:53
 * <p>
 * Description:
 * <p>
 * History:
 *
 * <author>          <time>          <version>          <desc>
 * <p>
 * 作者姓名           修改时间           版本号              描述
 */


package com.bonc.dx.crawler_manage.util;





import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author ljx
 * @create 2021/2/2
 * @since 1.0.0
 */

public class DateUtil {

	public static String getDate(String formt) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formt);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, -1);
		String parse = null;
		try {
			parse = simpleDateFormat.format(calendar.getTime());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parse;
	}


}