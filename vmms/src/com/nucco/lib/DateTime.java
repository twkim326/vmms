package com.nucco.lib;

/**
 * DateTime.java
 *
 * date library
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class DateTime {
/**
 * 오늘 날짜
 *
 * @return yyyy-MM-dd HH:mm:ss
 *
 */
	public static String date() {
		return date("yyyy-MM-dd HH:mm:ss");
	}
/**
 * 오늘 날짜
 *
 * @param pattern date format symbols
 * @return string
 *
 */
	public static String date(String pattern) {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern);

		return sdf.format(new java.util.Date());
	}
/**
 * 특정 날짜
 *
 * @param cal java.util.Calendar
 * @param pattern date format symbols
 * @return string
 *
 */
	public static String date(java.util.Calendar cal, String pattern) {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern);

		return sdf.format(cal.getTime());
	}
/**
 * 유효한 날짜인지 체크
 *
 * @param sYear 년
 * @param sMonth 월
 * @param sDay 일
 * @return boolean
 *
 */
	public static boolean isDate(int sYear, int sMonth, int sDay) {
		if (sYear < 0 || sMonth <= 0 || sDay <= 0 || sMonth > 12 || sDay > 31) {
			return false;
		} else if (sDay > getLastDayOfMonth(sYear, sMonth - 1)) {
			return false;
		}

		return true;
	}
/**
 * 유효한 날짜인지 체크
 *
 * @param sDate 날짜 (yyyyMMdd)
 * @return boolean
 *
 */
	public static boolean isDate(String sDate) {
		if (sDate == null || sDate.length() != 8) {
			return false;
		}

		int sYear = Integer.parseInt(sDate.substring(0, 4));
		int sMonth = Integer.parseInt(sDate.substring(4, 6));
		int sDay = Integer.parseInt(sDate.substring(6, 8));

		return isDate(sYear, sMonth, sDay);
	}
/**
 * 해당 월의 마지막 일수
 *
 * @param sYear 년
 * @param sMonth 월
 * @return boolean
 *
 */
	public static int getLastDayOfMonth(int sYear, int sMonth) {
		java.util.Calendar cal = java.util.Calendar.getInstance();

		cal.set(sYear, sMonth - 1, 1);

		return cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
	}
/**
 * 해당 월의 마지막 일수
 *
 * @param sDate 날짜(yyyyMM)
 * @return boolean
 *
 */
	public static int getLastDayOfMonth(String sDate) {
		int sYear = Integer.parseInt(sDate.substring(0, 4));
		int sMonth = Integer.parseInt(sDate.substring(4, 6));

		return getLastDayOfMonth(sYear, sMonth);
	}
/**
 * 두 날짜간의 날짜수 반환
 *
 * @param sDate 시작 날짜 (yyyyMMdd)
 * @param eDate 종료 날짜 (yyyyMMdd)
 * @return long
 *
 */
	public static long getDifferDay(String sDate, String eDate) {
		int sy = Integer.parseInt(sDate.substring(0, 4));
		int sm = Integer.parseInt(sDate.substring(4, 6));
		int sd = Integer.parseInt(sDate.substring(6, 8));
		int ey = Integer.parseInt(eDate.substring(0, 4));
		int em = Integer.parseInt(eDate.substring(4, 6));
		int ed = Integer.parseInt(eDate.substring(6, 8));

		java.util.GregorianCalendar s = new java.util.GregorianCalendar(sy, sm - 1, sd, 0, 0, 0);
		java.util.GregorianCalendar e = new java.util.GregorianCalendar(ey, em - 1, ed, 0, 0, 0);

		return (e.getTime().getTime() - s.getTime().getTime()) / 86400000;
	}
/**
 * 두 날짜간의 시간수 반환
 *
 * @param sDate 시작 날짜 (yyyyMMddHHmmss)
 * @param eDate 종료 날짜 (yyyyMMddHHmmss)
 * @return long
 *
 */
	public static long getDifferTime(String sDate, String eDate) {
		int sy = Integer.parseInt(sDate.substring(0, 4));
		int sm = Integer.parseInt(sDate.substring(4, 6));
		int sd = Integer.parseInt(sDate.substring(6, 8));
		int sh = Integer.parseInt(sDate.substring(8, 10));
		int sn = Integer.parseInt(sDate.substring(10, 12));
		int ss = Integer.parseInt(sDate.substring(12, 14));
		int ey = Integer.parseInt(eDate.substring(0, 4));
		int em = Integer.parseInt(eDate.substring(4, 6));
		int ed = Integer.parseInt(eDate.substring(6, 8));
		int eh = Integer.parseInt(eDate.substring(8, 10));
		int en = Integer.parseInt(eDate.substring(10, 12));
		int es = Integer.parseInt(eDate.substring(12, 14));

		java.util.GregorianCalendar s = new java.util.GregorianCalendar(sy, sm - 1, sd, sh, sn, ss);
		java.util.GregorianCalendar e = new java.util.GregorianCalendar(ey, em - 1, ed, eh, en, es);

		return (e.getTime().getTime() - s.getTime().getTime()) / 1000;
	}
/**
 * 특정 날짜와 오늘 날짜간의 시간수 반환
 *
 * @param sDate 시작 날짜 (yyyyMMddHHmmss)
 * @return long
 *
 */
	public static long getDifferTime(String sDate) {
		return getDifferTime(sDate, date("yyyyMMddHHmmss"));
	}
/**
 * 오늘 날짜에 입력된 일수를 더한 날짜
 *
 * @param addDay 더할 일수
 * @return string
 *
 */
	public static String getAddDay(int addDay) {
		java.util.Calendar cal = java.util.Calendar.getInstance(java.util.Locale.KOREA);

		cal.add(java.util.Calendar.DATE, addDay);

		return (new java.text.SimpleDateFormat("yyyyMMdd")).format(cal.getTime());
	}
/**
 * 특정 날짜에 입력된 일수를 더한 날짜
 *
 * @param sDate 날짜 (yyyyMMdd)
 * @param addDay 더할 일수
 * @return string
 *
 */
	public static String getAddDay(String sDate, int addDay) {
		java.util.Calendar cal = java.util.Calendar.getInstance(java.util.Locale.KOREA);
		int y = Integer.parseInt(sDate.substring(0, 4));
		int m = Integer.parseInt(sDate.substring(4, 6));
		int d = Integer.parseInt(sDate.substring(6, 8));

		cal.set(y, m - 1, d);
		cal.add(java.util.Calendar.DATE, addDay);

		return (new java.text.SimpleDateFormat("yyyyMMdd")).format(cal.getTime());
	}
/**
 * 현재 날짜가 올해의 몇째주에 해당하는지 계산
 *
 * @return int
 *
 */
	public static int getWeekOfYear() {
		return java.util.Calendar.getInstance(java.util.Locale.KOREA).get(java.util.Calendar.WEEK_OF_YEAR);
	}
/**
 * 특정 날짜가 해당 년의 몇째주에 해당하는지 계산
 *
 * @param sDate 날짜 (yyyyMMdd)
 * @return int
 *
 */
	public static int getWeekOfYear(String sDate) {
		java.util.Calendar cal = java.util.Calendar.getInstance(java.util.Locale.KOREA);
		int y = Integer.parseInt(sDate.substring(0, 4));
		int m = Integer.parseInt(sDate.substring(4, 6));
		int d = Integer.parseInt(sDate.substring(6, 8));

		cal.set(y, m - 1, d);

		return cal.get(java.util.Calendar.WEEK_OF_YEAR);
	}
/**
 * 현재 날짜가 현재 월의 몇째주에 해당하는지 계산
 *
 * @return int
 *
 */
	public static int getWeekOfMonth() {
		return java.util.Calendar.getInstance(java.util.Locale.KOREA).get(java.util.Calendar.WEEK_OF_MONTH);
	}
/**
 * 특정 날짜가 해당 월의 몇째주에 해당하는지 계산
 *
 * @param sDate 날짜 (yyyyMMdd)
 * @return int
 *
 */
	public static int getWeekOfMonth(String sDate) {
		java.util.Calendar cal = java.util.Calendar.getInstance(java.util.Locale.KOREA);
		int y = Integer.parseInt(sDate.substring(0, 4));
		int m = Integer.parseInt(sDate.substring(4, 6));
		int d = Integer.parseInt(sDate.substring(6, 8));

		cal.set(y, m - 1, d);

		return cal.get(java.util.Calendar.WEEK_OF_MONTH);
	}
/**
 * 오늘 요일
 *
 * @return int
 *
 */
	public static int getDayOfWeek() {
		return java.util.Calendar.getInstance(java.util.Locale.KOREA).get(java.util.Calendar.DAY_OF_WEEK);
	}
/**
 * 특정 날짜의 요일
 *
 * @param sDate 날짜 (yyyyMMdd)
 * @return int
 *
 */
	public static int getDayOfWeek(String sDate) {
		java.util.Calendar cal = java.util.Calendar.getInstance(java.util.Locale.KOREA);
		int y = Integer.parseInt(sDate.substring(0, 4));
		int m = Integer.parseInt(sDate.substring(4, 6));
		int d = Integer.parseInt(sDate.substring(6, 8));

		cal.set(y, m - 1, d);

		return cal.get(java.util.Calendar.DAY_OF_WEEK);
	}
}