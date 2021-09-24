package com.nucco.cfg;

/**
 * GeneralConfig.java
 *
 * 일반 설정
 *
 * 작성일 - 2008/04/25, 정원광
 *
 */

public class GeneralConfig implements Config {
/**
 * configuration properties
 *
 */
	protected java.util.Properties properties = null;
/**
 * configuration file
 *
 */
	protected java.io.File source = null;
/**
 * last modification time
 *
 */
	protected long lastModified = 0;
/**
 *
 */
	public GeneralConfig() {
		this.properties = new java.util.Properties();
	}
/**
 * @param source configuration file
 *
 */
	public GeneralConfig(java.io.File source) {
		java.io.FileInputStream stream = null;
		java.io.BufferedInputStream buffer = null;

		try {
			stream = new java.io.FileInputStream(source);
			buffer = new java.io.BufferedInputStream(stream);

			this.properties = new java.util.Properties();
			this.properties.load(buffer);
			this.lastModified = source.lastModified();
		} catch (Exception e) {
//			System.out.println("failed set " + source.getName() + " :: " + e.getMessage());
		} finally {
			try {
				if (stream != null) {
					stream.close();
					stream = null;
				}
			} catch (Exception e_) {
			}

			try {
				if (buffer != null) {
					buffer.close();
					buffer = null;
				}
			} catch (Exception e_) {
			}
		}

		this.source = source;
	}
/**
 * get properties :: string
 *
 * @param key keyword
 * @return string
 *
 */
	public String getString(String key) {
		String returnValue = null;

		try {
			returnValue = com.nucco.lib.StringEx.charset(this.properties.getProperty(key, ""));
		} catch(Exception e) {
//			System.out.println("failed get " + this.source.getName() + " (string) :: " + key);
		}

		return returnValue;
	}
/**
 * get properties :: boolean
 *
 * @param key keyword
 * @return boolean
 *
 */
	public boolean getBoolean(String key) {
		boolean returnValue = false;

		try {
			returnValue = (new Boolean(this.properties.getProperty(key))).booleanValue();
		} catch(Exception e){
//			System.out.println("failed get " + this.source.getName() + " (boolean) :: " + key);
		}

		return returnValue;
	}
/**
 * get properties :: int
 *
 * @param key keyword
 * @return int
 *
 */
	public int getInt(String key) {
		int returnValue = 0;

		try {
			returnValue = Integer.parseInt(this.properties.getProperty(key));
		} catch(Exception e){
//			System.out.println("failed get " + this.source.getName() + " (int) :: " + key);
		}

		return returnValue;
	}
/**
 * get properties :: long
 *
 * @param key keyword
 * @return long
 *
 */
	public long getLong(String key) {
		long returnValue = 0;

		try {
			returnValue = Long.parseLong(this.properties.getProperty(key));
		} catch(Exception e){
//			System.out.println("failed get " + this.source.getName() + " (long) :: " + key);
		}

		return returnValue;
	}
/**
 * get properties :: java.util.Properties
 *
 * @return java.util.Properties
 *
 */
	public java.util.Properties getProperties() {
		return this.properties;
	}
/**
 * get properties
 *
 * @param key keyword
 * @return string
 *
 */
	public String get(String key) {
		return this.getString(key);
	}
/**
 * put properties :: string
 *
 * @param key keyword
 * @param val value
 *
 */
	public void put(String key, String val) {
		try {
			this.properties.put(key, com.nucco.lib.StringEx.charset(val, com.nucco.Common.CHARSET, "ISO-8859-1"));
		} catch(Exception e) {
//			System.out.println("failed put " + key + " (string) :: " + val);
		}
	}
/**
 * put properties :: boolean
 *
 * @param key keyword
 * @param val value
 *
 */
	public void put(String key, boolean val) {
		try {
			this.properties.put(key, String.valueOf(val));
		} catch( Exception e ) {
//			System.out.println("failed put " + key + " (boolean) :: " + val);
		}
	}
/**
 * put properties :: int
 *
 * @param key keyword
 * @param val value
 *
 */
	public void put(String key, int val) {
		try {
			this.properties.put(key, String.valueOf(val));
		} catch( Exception e ) {
//			System.out.println("failed put " + key + " (int) :: " + val);
		}
	}
/**
 * put properties :: long
 *
 * @param key keyword
 * @param val value
 *
 */
	public void put(String key, long val) {
		try {
			this.properties.put(key, String.valueOf(val));
		} catch( Exception e ) {
//			System.out.println("failed put " + key + " (long) :: " + val);
		}
	}
/**
 * get last modification time
 *
 * @return long
 *
 */
	public long lastModified() {
		return this.lastModified;
	}
}
