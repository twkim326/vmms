package com.nucco.lib;

/**
 * StringEx.java
 *
 * string library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class StringEx {
/**
 * charset 변경
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String charset(String arg) {
		return charset(arg, "ISO-8859-1", com.nucco.Common.CHARSET);
	}
/**
 * charset 변경
 *
 * @param arg 문자열
 * @param source 변경할 charset
 * @return string
 *
 */
	public static String charset(String arg, String source) {
		return charset(arg, source, com.nucco.Common.CHARSET);
	}
/**
 * charset 변경
 *
 * @param arg 문자열
 * @param source 변경할 charset
 * @param target 변경될 charset
 * @return string
 *
 */
	public static String charset(String arg, String source, String target) {
		try {
			return new String(arg.getBytes(source), target);
		} catch (Exception e) {
			return arg;
		}
	}
/**
 * 빈 문자열 체크
 *
 * @param arg 문자열
 * @return boolean
 *
 */
	public static boolean isEmpty(String arg) {
		if (arg == null) {
			return true;
		} else if (arg.equals("")) {
			return true;
		}

		return false;
	}
/**
 * 빈 문자열일 경우 기본값 할당
 *
 * @param arg 문자열
 * @param defaultValue 대체할 값
 * @return boolean
 *
 */
	public static String setDefaultValue(String arg, String defaultValue) {
		if (isEmpty(arg)) {
			return defaultValue;
		} else {
			return arg;
		}
	}
/**
 * 키워드 설정
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String getKeyword(String arg) {
		if (isEmpty(arg)) {
			return "";
		}

		return replace(replace(replace(replace(replace(replace(arg, ">", ""), "<", ""), "--", ""), "#", ""), "\"", ""), "'", "");
	}
/**
 * int -> string
 *
 * @param arg 숫자
 * @return string
 *
 */
	public static String int2str(int arg) {
		try {
			return String.valueOf(arg);
		} catch( Exception e ) {
			return "";
		}
	}
/**
 * long -> string
 *
 * @param arg 숫자
 * @return string
 *
 */
	public static String long2str(long arg) {
		try {
			return String.valueOf(arg);
		} catch( Exception e ) {
			return "";
		}
	}
/**
 * string -> int
 *
 * @param arg 문자열
 * @return int
 *
 */
	public static int str2int(String arg) {
		try {
			return Integer.parseInt(arg.trim());
		} catch( Exception e ) {
			return 0;
		}
	}
/**
 * string -> int
 *
 * @param arg 문자열
 * @param min = 최솟값
 * @return int
 *
 */
	public static int str2int(String arg, int min) {
		if (isEmpty(arg)) {
			return min;
		} else if (str2int(arg) < min) {
			return min;
		} else {
			return str2int(arg);
		}
	}
/**
 * string -> int
 *
 * @param arg 문자열
 * @param min = 최솟값
 * @param max = 최댓값
 * @return int
 *
 */
	public static int str2int(String arg, int min, int max) {
		if (isEmpty(arg)) {
			return min;
		} else if (str2int(arg) < min) {
			return min;
		} else if (str2int(arg) > max) {
			return max;
		} else {
			return str2int(arg);
		}
	}
/**
 * string -> long
 *
 * @param arg 문자열
 * @return long
 *
 */
	public static long str2long(String arg) {
		try {
			return Long.parseLong(arg.trim());
		} catch( Exception e ) {
			return 0;
		}
	}
/**
 * string -> long
 *
 * @param arg 문자열
 * @param min = 최솟값
 * @return long
 *
 */
	public static long str2long(String arg, long min) {
		if (isEmpty(arg)) {
			return min;
		} else if (str2long(arg) < min) {
			return min;
		} else {
			return str2long(arg);
		}
	}
/**
 * string -> long
 *
 * @param arg 문자열
 * @param min = 최솟값
 * @param max = 최댓값
 * @return long
 *
 */
	public static long str2long(String arg, long min, long max) {
		if (isEmpty(arg)) {
			return min;
		} else if (str2long(arg) < min) {
			return min;
		} else if (str2long(arg) > max) {
			return max;
		} else {
			return str2long(arg);
		}
	}
/**
 * string -> float
 *
 * @param arg 문자열
 * @return float
 *
 */
	public static float str2float(String arg) {
		try {
			return Float.parseFloat(arg.trim());
		} catch( Exception e ) {
			return 0;
		}
	}
/**
 * 문자열 변경
 *
 * @param arg 문자열
 * @param org 바꿀 문자
 * @param rep 변경할 문자
 * @return string
 *
 */
	public static String replace(String arg, String org, String rep) {
		StringBuffer sb = new StringBuffer();
		String remain = arg;
		int i;

		while (remain != null) {
			i = remain.indexOf(org);

			if (i == -1) {
				sb.append(remain);
				break;
			} else {
				sb.append(remain.substring(0, i));
				sb.append(rep);
			}

			if (remain.length() < org.length()) {
				sb.append(remain);
				break;
			} else {
				remain = remain.substring(i + org.length());
			}
		}

		return sb.toString();
	}
/**
 * 문자열 변경
 *
 * @param arg 문자열
 * @param org 바꿀 문자
 * @param rep 변경할 숫자
 * @return string
 *
 */
	public static String replace(String arg, String org, int rep) {
		return replace(arg, org, int2str(rep));
	}
/**
 * 문자열 변경
 *
 * @param arg 문자열
 * @param org 바꿀 문자
 * @param rep 변경할 숫자
 * @return string
 *
 */
	public static String replace(String arg, String org, long rep) {
		return replace(arg, org, long2str(rep));
	}
/**
 * 기준 문자열로 정리
 *
 * @param arg 문자열
 * @param d1 체크할 기준 문자열
 * @param d2 정리할 기준 문자열
 * @return string
 *
 */
	public static String arrange(String arg, String d1, String d2) {
		String returnValue = "";

		if (!isEmpty(arg)) {
			String arr[] = split(arg, d1);
			int c = 0;

			for (int i = 0; i < arr.length; i++) {
				if (!isEmpty(arr[i])) {
					if (c == 0) {
						returnValue += arr[i];
					} else {
						returnValue += d2 + arr[i];
					}

					c++;
				}
			}
		}

		return returnValue;
	}
/**
 * 문자열 반복
 *
 * @param arg 문자열
 * @param cnt 반복횟수
 * @return string
 *
 */
	public static String repeat(String arg, int cnt) {
		String returnValue = "";

		for (int i = 1; i <= cnt; i++) {
			returnValue += arg;
		}

		return returnValue;
	}
/**
 * 자릿수 맞추기
 *
 * @param arg 문자열
 * @param frm 포맷문자
 * @param cnt 길이
 * @return string
 *
 */
	public static String format(String arg, String frm, int cnt) {
		String returnValue = setDefaultValue(arg, "");
		int len = returnValue.length();

		for (int i = 1; i <= cnt - len; i++) {
			returnValue = frm + returnValue;
		}

		return returnValue;
	}
/**
 * 자릿수 맞추기
 *
 * @param arg 숫자
 * @param frm 포맷문자
 * @param cnt 길이
 * @return string
 *
 */
	public static String format(int arg, String frm, int cnt) {
		return format(int2str(arg), frm, cnt);
	}
/**
 * 자릿수 맞추기
 *
 * @param arg 숫자
 * @param frm 포맷문자
 * @param cnt 길이
 * @return string
 *
 */
	public static String format(long arg, String frm, int cnt) {
		return format(long2str(arg), frm, cnt);
	}
/**
 * 3자리마다 콤마찍기
 *
 * @param arg 숫자
 * @return string
 *
 */
	public static String comma(int arg) {
		return comma((long) arg);
	}
/**
 * 3자리마다 콤마찍기
 *
 * @param arg 숫자
 * @return string
 *
 */
	public static String comma(long arg) {
		java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance();

		return nf.format(arg);
	}
/**
 * 3자리마다 콤마찍기
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String comma(String arg) {
		return comma(str2long(arg));
	}
/**
 * 소숫점 끊기
 *
 * @param arg 숫자
 * @param cipher 자릿수
 * @return string
 *
 */
	public static String round(double arg, int cipher) {
		java.text.DecimalFormat df = new java.text.DecimalFormat("#." + repeat("0", cipher));

		return df.format(arg).toString();
	}
/**
 * 소숫점 끊기
 *
 * @param arg 숫자
 * @param cipher 자릿수
 * @return string
 *
 */
	public static String round(float arg, int cipher) {
		return round((double) arg, cipher);
	}
/**
 * 문자열 자르기
 *
 * @param arg 문자열
 * @param len 길이
 * @return string
 *
 */
	public static String cut(String arg, int len) {
		return cut(arg, len, "...");
	}
/**
 * 문자열 자르기
 *
 * @param arg 문자열
 * @param len 길이
 * @param tail 꼬릿말
 * @return string
 *
 */
	public static String cut(String arg, int len, String tail) {
		if (isEmpty(arg)) {
			return "";
		}

		if (arg.length() <= len) {
			return arg;
		}

		int count = 0;

		for (int i = 0; i < arg.length(); i++) {
			if (count > len) {
				break;
			}

			if ((char) arg.charAt(i) >= 127) {
				count += 2;
			} else {
				count += 1;
			}
		}

		return arg.substring(0, count > arg.length() ? arg.length() : count) + tail;
	}
/**
 * 배열안에 값이 있는지 체크
 *
 * @param var 문자열
 * @param arr 배열
 * @return boolean
 *
 */
	public static boolean inArray(String var, String[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].equals(var)) {
				return true;
			}
		}

		return false;
	}
/**
 * 배열안에 값이 있는지 체크
 *
 * @param var 숫자
 * @param arr 배열
 * @return boolean
 *
 */
	public static boolean inArray(int var, int[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == var) {
				return true;
			}
		}

		return false;
	}
/**
 * 배열안에 값이 있는지 체크
 *
 * @param var 숫자
 * @param arr 배열
 * @return boolean
 *
 */
	public static boolean inArray(long var, long[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == var) {
				return true;
			}
		}

		return false;
	}
/**
 * 배열 검사 후 정해진 배열크기가 아니면 강제로 크기 맞추기
 *
 * @param arr 배열
 * @param len 최대 크기
 * @return string[]
 *
 */
	public static String[] fixArray(String[] arr, int len) {
		if (arr.length < len) {
			String[] returnValue = new String[len];

			for (int i = 0; i < arr.length; i++) {
				returnValue[i] = arr[i];
			}

			return returnValue;
		} else {
			return arr;
		}
	}
/**
 * 배열 검사 후 정해진 배열크기가 아니면 강제로 크기 맞추기
 *
 * @param arr 배열
 * @param len 최대 크기
 * @return int[]
 *
 */
	public static int[] fixArray(int[] arr, int len) {
		if (arr.length < len) {
			int[] returnValue = new int[len];

			for (int i = 0; i < arr.length; i++) {
				returnValue[i] = arr[i];
			}

			return returnValue;
		} else {
			return arr;
		}
	}
/**
 * 배열 검사 후 정해진 배열크기가 아니면 강제로 크기 맞추기
 *
 * @param arr 배열
 * @param len 최대 크기
 * @return long[]
 *
 */
	public static long[] fixArray(long[] arr, int len) {
		if (arr.length < len) {
			long[] returnValue = new long[len];

			for (int i = 0; i < arr.length; i++) {
				returnValue[i] = arr[i];
			}

			return returnValue;
		} else {
			return arr;
		}
	}
/**
 * 문자열 파싱 후 해당 값 읽어오기
 *
 * @param arg 문자열
 * @param var 읽어올 변수명
 * @return string
 *
 */
	public static String parse(String arg, String var) {
		if (isEmpty(arg)) {
			return "";
		}

		String[] data = arg.split("&");
		String[] tmps;

		for (int i = 0; i < data.length; i++) {
			tmps = data[i].split("=");

			if (tmps.length == 2) {
				if (tmps[0].equals(var)) {
					return tmps[1];
				}
			}
		}

		return "";
	}
/**
 * 문자열 분리
 *
 * @param arg 문자열
 * @param dlm 구분자
 * @return string[]
 *
 */
	public static String[] split(String arg, String dlm) {
		if (isEmpty(arg)) {
			return new String[0];
		}

		java.util.StringTokenizer st = new java.util.StringTokenizer(arg, dlm);
		String[] returnValue = new String[st.countTokens()];
		int i = 0;

		while (st.hasMoreTokens()) {
			returnValue[i++] = st.nextToken();
		}

		return returnValue;
	}
/**
 * 역순 정렬
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String reverse(String arg) {
		if (isEmpty(arg)) {
			return "";
		}

		return (new StringBuffer(arg)).reverse().toString();
	}
/**
 * 인코딩
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String encode(String arg) {
		if (isEmpty(arg)) {
			return "";
		}

		try {
			return java.net.URLEncoder.encode(arg, com.nucco.Common.CHARSET);
		} catch (Exception e) {
			return arg;
		}
	}
/**
 * 디코딩
 *
 * @param arg 문자열
 * @return string
 *
 */
	public static String decode(String arg) {
		if (isEmpty(arg)) {
			return "";
		}

		try {
			return java.net.URLDecoder.decode(arg, com.nucco.Common.CHARSET);
		} catch (Exception e) {
			return arg;
		}
	}
}