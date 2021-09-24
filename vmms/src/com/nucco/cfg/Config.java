package com.nucco.cfg;

/**
 * Config.java
 *
 * 설정 인터페이스
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public interface Config {
/**
 * get properties :: string
 *
 * @param key key
 * @return string
 *
 */
	public String getString(String key);
/**
 * get properties :: boolean
 *
 * @param key keyword
 * @return boolean
 *
 */
	public boolean getBoolean(String key);
/**
 * get properties :: int
 *
 * @param key keyword
 * @return int
 *
 */
	public int getInt(String key);
/**
 * get properties :: long
 *
 * @param key keyword
 * @return long
 *
 */
	public long getLong(String key);
/**
 * get properties :: java.util.Properties
 *
 * @return java.util.Properties
 *
 */
	public java.util.Properties getProperties();
/**
 * get properties
 *
 * @param key keyword
 * @return string
 *
 */
	public String get(String key);
/**
 * put properties
 *
 * @param key keyword
 * @param val value
 *
 */
	public void put(String key, String val);
/**
 * get last modification time
 *
 * @return long
 *
 */
	public long lastModified();
}