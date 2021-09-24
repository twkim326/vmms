package com.nucco.lib.security;

/**
 * Hash.java
 *
 * 암호화/복호화 관련
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class Hash {
/**
 * MD5
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String md5(String arg) {
		try {
			StringBuffer sb = new StringBuffer(32);
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");

			md.update(arg.getBytes("UTF-8"));
			byte[] r = md.digest();

			for (int i = 0; i < r.length; i++) {
				String x = Integer.toHexString(r[i] & 0xff);

				if (x.length() < 2) {
					sb.append("0");
				}

				sb.append(x);
			}

			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}
/**
 * SHA1
 *
 * @param args 문자열
 * @return string
 *
 */
	public static String sha1(String args) {
		try {
			StringBuffer sb = new StringBuffer();
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");

			md.update(args.getBytes("UTF-8"));
			byte[] r = md.digest();

			for (int i = 0; i < r.length; i++) {
				String x = Integer.toHexString(r[i] & 0xff);

				if (x.length() < 2) {
					sb.append("0");
				}

				sb.append(x);
			}

			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}
}