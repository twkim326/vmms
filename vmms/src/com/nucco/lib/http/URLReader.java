package com.nucco.lib.http;

/**
 * URLReader.java
 *
 * URL 읽기
 *
 * 작성일 - 2008/11/11, 정원광
 *
 */

public class URLReader {
/**
 * URL 읽기
 *
 * @param src 소켓으로 읽어 올 URL
 * @return string
 *
 */
	public static String read(String src) {
		java.io.InputStreamReader stream = null;
		java.io.BufferedReader reader = null;
		String returnValue = "";

		try {
			java.net.URL url = new java.net.URL(src);
			java.net.URLConnection conn = url.openConnection();
			String line = null;

			stream = new java.io.InputStreamReader(conn.getInputStream(), com.nucco.Common.CHARSET);
			reader = new java.io.BufferedReader(stream);

			while ((line = reader.readLine()) != null) {
				returnValue += line + "\r\n";
			}
		} catch (Exception e) {
			returnValue = "failed read " + src + " :: " + e.getMessage();
		} finally {
			try {
				if (reader != null) {
					reader.close();
					reader = null;
				}
			} catch (Exception e_) {
			}

			try {
				if (stream != null) {
					stream.close();
					stream = null;
				}
			} catch (Exception e_) {
			}
		}

		return returnValue;
	}
}
