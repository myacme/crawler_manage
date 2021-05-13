package com.bonc.dx.crawler_manage.pool.ip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 更新代理的zip文件
 */
public class ProxyZipUtil {

	static final ReadWriteLock lock = new ReentrantReadWriteLock();
	static final Lock readLock = lock.readLock();
	static final Lock writeLock = lock.writeLock();

	/**
	 * 生成更新后的压缩文件
	 *
	 * @param inputName
	 * @param outPutName
	 * @param ip
	 * @throws IOException
	 */
	public String updateZipFile(String inputName, String outPutName, String ip) {
		String result = "";
		String[] iplist = ip.split(":");
		String host = iplist[0];
		String port = iplist[1];
		writeLock.lock();
		try {
			ZipFile zipFile = new ZipFile(inputName);
			// 复制为新zip
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outPutName));
			// 遍历所有文件复制
			for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
				ZipEntry entryIn = (ZipEntry) e.nextElement();
				System.out.println(entryIn.getName());
				if (entryIn.getName().equalsIgnoreCase("background.js")) {
					zos.putNextEntry(new ZipEntry(entryIn.getName()));
					InputStream is = zipFile.getInputStream(entryIn);
					byte[] buf = new byte[1024 * 5];
					int len;
					while ((len = (is.read(buf))) > 0) {
						String s = new String(buf);
						String hostTemp = s.substring(s.indexOf("host"), s.indexOf("port") - 15);
						String portTemp = s.substring(s.indexOf("port"), s.indexOf("bypassList") - 10);
						if (s.contains(hostTemp)) {
							s = s.replace(hostTemp, "host:\"" + host + "\"");
							result += hostTemp.replace("host", "")
									.replace(":", "")
									.replace("\"", "").trim();
						}
						if (s.contains(portTemp)) {
							s = s.replace(portTemp, "port:" + port + " },");
							result += ":";
							result += portTemp.replace("port", "")
									.replace(":", "")
									.replace(",", "")
									.replace("}", "").trim();
						}
						buf = s.getBytes();
						zos.write(buf, 0, (len < buf.length) ? len : buf.length);
					}
				} else {
					// zos.putNextEntry(entryIn);
					zos.putNextEntry(new ZipEntry(entryIn.getName()));
					InputStream is = zipFile.getInputStream(entryIn);
					byte[] buf = new byte[1024];
					int len;
					while ((len = is.read(buf)) > 0) {
						zos.write(buf, 0, (len < buf.length) ? len : buf.length);
					}
				}
				zos.closeEntry();
			}
			zipFile.close();
			zos.close();
			coverZip(inputName, outPutName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writeLock.unlock();
		}
		return result;
	}


	/**
	 * 读取压缩文件ip
	 *
	 * @param
	 * @throws IOException
	 */
	public static String getIp(int i) {
		String inputName = System.getProperty("proxyDir") + "proxy" + i + ".zip";
		String result = "";
		writeLock.lock();
		try {
			ZipFile zipFile = new ZipFile(inputName);
			// 遍历所有文件复制
			for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
				ZipEntry entryIn = (ZipEntry) e.nextElement();
				System.out.println(entryIn.getName());
				if (entryIn.getName().equalsIgnoreCase("background.js")) {
					InputStream is = zipFile.getInputStream(entryIn);
					byte[] buf = new byte[1024 * 5];
					int len;
					while ((len = (is.read(buf))) > 0) {
						String s = new String(buf);
						String hostTemp = s.substring(s.indexOf("host"), s.indexOf("port") - 15);
						String portTemp = s.substring(s.indexOf("port"), s.indexOf("bypassList") - 10);
						if (s.contains(hostTemp)) {
							result += hostTemp.replace("host", "")
									.replace(":", "")
									.replace("\"", "").trim();
						}
						if (s.contains(portTemp)) {
							result += ":";
							result += portTemp.replace("port", "")
									.replace(":", "")
									.replace(",", "")
									.replace("}", "").trim();
						}
						buf = s.getBytes();
					}
				}
			}
			zipFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writeLock.unlock();
		}
		return result;
	}

	/**
	 * 将更新后的替换原来的
	 *
	 * @param path1
	 * @param path2
	 */
	public void coverZip(String path1, String path2) {
		File file1 = new File(path1);
		File file2 = new File(path2);
		if (file1.exists() && file2.exists()) {
			file1.delete();
			file2.renameTo(new File(path1));
			file2.delete();
		}
	}

	/**
	 * 读取zip文件
	 *
	 * @param path
	 * @return
	 */
	public File getZip(String path) {
		readLock.lock();
		try {
			return new File(path);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			readLock.unlock();
		}
		return null;
	}


	public void init() {
		int size = Integer.parseInt(System.getProperty("driver_size"));
		for (int i = 0; i < size; i++) {
			String path1 = System.getProperty("proxyDir") + "proxy" + i + ".zip";
			String path2 = path1.replace(".zip", "_temp.zip");
			File file = new File(path1);
			if (file.exists()) {
				//获取新ip
				String ip = IpGetAndRelease.getIp(System.getProperty("ipGetUrl"));
				System.out.println("ip:" + ip);
				//更新代理文件
				String oldIp = updateZipFile(path1, path2, ip);
				System.out.println("oldIp:" + oldIp);
				IpGetAndRelease.releaseIp(System.getProperty("ipReleaseUrl") + oldIp);
			}
		}
	}

	public String setProxyIp(int i) {
		String path1 = System.getProperty("proxyDir") + "proxy" + i + ".zip";
		String path2 = path1.replace(".zip", "_temp.zip");
		File file = new File(path1);
		String ip = "";
		if (file.exists()) {
			//获取新ip
			ip = IpGetAndRelease.getIp(System.getProperty("ipGetUrl"));
			System.out.println("ip:" + ip);
			if (!ip.contains("code")) {
				//更新代理文件
				String oldIp = updateZipFile(path1, path2, ip);
				System.out.println("oldIp: " + oldIp);
				IpGetAndRelease.releaseIp(System.getProperty("ipReleaseUrl") + oldIp);
			}
		}
		return ip;
	}

	public static void main(String[] args) throws IOException {
		ProxyZipUtil zipFlieChangeUtil = new ProxyZipUtil();
		zipFlieChangeUtil.updateZipFile("C:\\Users\\xiong\\Desktop\\proxy\\proxy0.zip",
				"C:\\Users\\xiong\\Desktop\\proxy\\proxy11.zip",
				"60.175.22.85:5021");
	}
}
