package com.nucco.lib.db;

/**
 * DBLibrary.java
 *
 * DB 라이브러리
 *
 * 작성일 - 2008/04/08, 정원광
 *
 */

public class DBLibrary {
/**
 * 일반
 *
 */
	public final int NORMAL = 0;
/**
 * 커밋
 *
 */
	public final int COMMIT = 1;
/**
 * 롤백
 *
 */
	public final int ROLLBACK = 2;
/**
 * org.apache.log4j.Logger
 *
 */
	private org.apache.log4j.Logger logger = null;
/**
 *
 */
	public DBLibrary() {
	}
/**
 * @param logger 로거
 *
 */
	public DBLibrary(org.apache.log4j.Logger logger) {
		this.logger = logger;
	}
/**
 * JNDI를 이용한 연결
 *
 * @param name javax.naming.Context name
 * @param jndi javax.sql.DataSource name
 * @return connection
 *
 */
	public java.sql.Connection getConnection(String name, String jndi) {
		java.sql.Connection conn = null;

		try {
			javax.naming.Context context = (javax.naming.Context) new javax.naming.InitialContext().lookup(name);
			javax.sql.DataSource ds = (javax.sql.DataSource) context.lookup(jndi);

			conn = ds.getConnection();
		} catch (Exception e) {
			if (this.logger != null) {
				this.logger.error(e);
			}
		}

		return conn;
	}
/**
 * 아이디/패스워드를 이용한 연결
 *
 * @param name jdbc
 * @param host host
 * @param user user id
 * @param pass password
 * @return connection
 *
 */
	public java.sql.Connection getConnection(String name, String host, String user, String pass) {
		java.sql.Connection conn = null;

		if ((user == null || user.equals("")) && (pass == null || pass.equals(""))) {
			conn = this.getConnection(name, host);
		} else {
			try {
				Class<?> cls = Class.forName(name);
				java.sql.DriverManager.registerDriver((java.sql.Driver) cls.newInstance());

				conn = java.sql.DriverManager.getConnection(host, user, pass);
			} catch (Exception e) {
				if (this.logger != null) {
					this.logger.error(e);
				}
			}
		}

		return conn;
	}
/**
 * 하나의 레코드셋 가져오기
 *
 * @param ps java.sql.PreparedStatement
 * @return string
 *
 */
	public String getResult(java.sql.PreparedStatement ps) {
		String returnValue = "";
		java.sql.ResultSet rs = null;

		try {
			rs = ps.executeQuery();

			if (rs.next()) {
				returnValue = rs.getString(1);
			}
		} catch (Exception e) {
			if (this.logger != null) {
				this.logger.error(e);
			}
		} finally {
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
			} catch (Exception e) {
			}
		}

		return returnValue;
	}
/**
 * 하나의 레코드셋 가져오기
 *
 * @param conn java.sql.Connection
 * @param sql 쿼리문
 * @return string
 *
 */
	public String getResult(java.sql.Connection conn, String sql) {
		String returnValue = "";
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;

		try {
			if (conn == null || conn.isClosed() == true) {
				return returnValue;
			}

			ps = this.prepareStatement(conn, sql);
			rs = ps.executeQuery();

			if (rs.next()) {
				returnValue = rs.getString(1);
			}
		} catch (Exception e) {
			if (this.logger != null) {
				this.logger.error(e);
			}
		} finally {
			try {
				if (ps != null) {
					ps.close();
					ps = null;
				}
			} catch (Exception e) {
			}

			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
			} catch (Exception e) {
			}
		}

		return returnValue;
	}
/**
 * CLOB 읽기
 *
 * @param rs 레코드셋
 * @param column 컬럼
 * @return string
 *
 */
	public String getClob(java.sql.ResultSet rs, String column) throws Exception {
		StringBuffer buffer = new StringBuffer();

		buffer.setLength(0);

		java.io.Reader read = rs.getCharacterStream(column);

		if (read == null) {
			return "";
		}

		char[] Buf = new char[1024];
		int i;

		while ((i = read.read(Buf, 0, 1024)) != -1) {
			buffer.append(Buf, 0, i);
		}

		read.close();

		return buffer.toString();
	}
/**
 * CLOB 쓰기
 *
 * @param conn java.sql.Connection
 * @param table 테이블
 * @param column 컬럼
 * @param where 검색절
 * @param contents 내용
 *
 */
	public void setClob(java.sql.Connection conn, String table, String column, String where, String contents) throws Exception {
		java.sql.PreparedStatement ps = this.prepareStatement(conn, "SELECT " + column + " FROM " + table + " WHERE " + where + " FOR UPDATE NOWAIT");
		java.sql.ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			oracle.sql.CLOB clob = (oracle.sql.CLOB) rs.getObject(column);
//20160220
//			java.io.BufferedWriter writer = new java.io.BufferedWriter(clob.getCharacterOutputStream());
//			writer.write(contents);
//			writer.close();
			clob.setString(1, contents);
		}

		rs.close();
		ps.close();
	}
/**
 * CallableStatement 생성
 *
 * @param conn java.sql.Connection
 * @param query 쿼리
 * @return java.sql.CallableStatement
 *
 */
	public java.sql.CallableStatement prepareCall(java.sql.Connection conn, String query) throws Exception {
		if (this.logger != null) {
			this.logger.debug(query);
		}

		return conn.prepareCall(query);
	}
/**
 * PreparedStatement 생성
 *
 * @param conn java.sql.Connection
 * @param query 쿼리
 * @return java.sql.PreparedStatement
 *
 */
	public java.sql.PreparedStatement prepareStatement(java.sql.Connection conn, String query) throws Exception {
		if (this.logger != null) {
			this.logger.debug(query);
		}

		return conn.prepareStatement(query);
	}
/**
 * 리소스 반환
 *
 * @param conn java.sql.Connection
 *
 */
	public void close(java.sql.Connection conn) {
		this.close(conn, NORMAL);
	}
/**
 * 리소스 반환
 *
 * @param conn java.sql.Connection
 * @param mode 커밋/롤백
 *
 */
	public void close(java.sql.Connection conn, int mode) {
		try {
			if (conn != null) {
				switch (mode) {
					case COMMIT :
						conn.commit();
						conn.setAutoCommit(true);
					case ROLLBACK :
						conn.rollback();
						conn.setAutoCommit(true);
				}

				conn.close();
				conn = null;
			}
		} catch (Exception e) {
		}
	}
/**
 * 리소스 반환
 *
 * @param cs java.sql.PreparedStatement
 *
 */
	public void close(java.sql.CallableStatement cs) {
		try {
			if (cs != null) {
				cs.close();
				cs = null;
			}
		} catch (Exception e) {
		}
	}
/**
 * 리소스 반환
 *
 * @param ps java.sql.PreparedStatement
 *
 */
	public void close(java.sql.PreparedStatement ps) {
		try {
			if (ps != null) {
				ps.close();
				ps = null;
			}
		} catch (Exception e) {
		}
	}
/**
 * 리소스 반환
 *
 * @param rs java.sql.ResultSet
 *
 */
	public void close(java.sql.ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
				rs = null;
			}
		} catch (Exception e) {
		}
	}
}