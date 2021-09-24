package com.nucco;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nucco.cfg.GeneralConfig;
import com.nucco.lib.StringEx;
import com.nucco.lib.base64.Base64;
import com.nucco.lib.db.DBLibrary;
import com.nucco.lib.http.URLReader;
import com.nucco.lib.security.Cryptograph;

import oracle.jdbc.OracleTypes;

public class GlobalConfig {
/**
 * com.nucco.this.cfg.GeneralConfig
 *
 */
	private GeneralConfig cfg = null;
/**
 * javax.servlet.http.HttpServletRequest
 *
 */
	private HttpServletRequest request;
/**
 * javax.servlet.http.HttpServletResponse
 *
 */
	private HttpServletResponse response;
/**
 * javax.servlet.http.HttpSession
 *
 */
	private HttpSession session;
/**
 * org.apache.log4j.Logger
 *
 */
	private Logger logger = null;
/**
 * 허가된 메뉴 목록
 *
 */
	public ArrayList<GeneralConfig> mMenu;
	public ArrayList<GeneralConfig> sMenu;
/**
 * @param request javax.servlet.http.HttpServletRequest
 * @param response javax.servlet.http.HttpServletResponse
 * @param session javax.servlet.http.HttpSession
 *
 */
	public GlobalConfig(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		this(request, response, session, "web.properties", null);
	}
/**
 * @param request javax.servlet.http.HttpServletRequest
 * @param response javax.servlet.http.HttpServletResponse
 * @param session javax.servlet.http.HttpSession
 * @param location = 현재 위치
 *
 */
	public GlobalConfig(HttpServletRequest request, HttpServletResponse response, HttpSession session, String location) {
		this(request, response, session, "web.properties", location);
	}
/**
 * @param request javax.servlet.http.HttpServletRequest
 * @param response javax.servlet.http.HttpServletResponse
 * @param session javax.servlet.http.HttpSession
 * @param properties = 설정 파일 명
 * @param location = 현재 위치
 *
 */
	public GlobalConfig(HttpServletRequest request, HttpServletResponse response, HttpSession session, String properties, String location) {
	// set root's absolute path
		String root = request.getSession().getServletContext().getRealPath("");

	// set config file
		String source = root + "/WEB-INF/" + properties;

		if (StringEx.inArray(request.getHeader("host"), "vmms.nblis.com".split(";"))) {
			source += ".dev";
		} else if (StringEx.inArray(request.getHeader("host"), "210.180.58.126;210.180.58.126:8081;200.200.100.10".split(";"))) {
			source += ".ubc";
		}
	/*
	// set java.io.File
		File file = new File(source);

	// create configuration
		this.cfg = new GeneralConfig(file);
	*/
		if(!com.nucco.VmmsEnv.getInstance().checkInit(source))
		{
			com.nucco.VmmsEnv.getInstance().Init(source);
		}

		// create configuration
		this.cfg = new GeneralConfig();
		this.cfg.put("root", root);

	// set request / response / session
		this.request = request;
		this.response = response;
		this.session = session;

	// set log4j
		//PropertyConfigurator.configure(this.cfg.get("config.log4j"));
		PropertyConfigurator.configure(com.nucco.VmmsEnv.getInstance().get("config.log4j"));
		this.logger = Logger.getLogger(this.getClass());

	// set user
		this.cfg.put("user.seq", 0);         // 회원번호
		this.cfg.put("user.id", "");         // 아이디
		this.cfg.put("user.name", "");       // 이름
		this.cfg.put("user.auth", 0);        // 권한
		this.cfg.put("user.company", 0);     // 소속
		this.cfg.put("user.organ", 0);       // 조직
		this.cfg.put("user.organ.depth", 0); // 조직 깊이
		this.cfg.put("user.operator", "N");  // 자판기 운영자 여부
		this.cfg.put("user.hash", "");       // 로그인 해쉬 데이터

	// set page access level
		this.cfg.put("ENABLE_I", "N");
		this.cfg.put("ENABLE_U", "N");
		this.cfg.put("ENABLE_D", "N");
		this.cfg.put("ENABLE_S", "N");

		try {
		// -- 2011.05.19, 계정 인증 정보를 쿠키에서 세션으로 변경
		//	String user = (new Cryptograph()).decrypt(Base64.decode(CookieEx.get(request, "MEM")));
			String user = (new Cryptograph()).decrypt(Base64.decode((String) request.getSession().getAttribute("MEM")));
		// --

			if (!StringEx.isEmpty(user)) {
				long seq = StringEx.str2long(StringEx.parse(user, "SEQ"));              // 회원번호
				String id = StringEx.decode(StringEx.parse(user, "ID"));                // 아이디
				String name = StringEx.decode(StringEx.parse(user, "NAME"));            // 이름
				long auth = StringEx.str2long(StringEx.parse(user, "AUTH"));            // 권한
				long company = StringEx.str2long(StringEx.parse(user, "COMPANY"));      // 소속
				long organ = StringEx.str2long(StringEx.parse(user, "ORGAN"));          // 조직
				int organDepth = StringEx.str2int(StringEx.parse(user, "ORGAN.DEPTH")); // 조직 깊이
				String operator = StringEx.decode(StringEx.parse(user, "OPERATOR"));    // 이름
				String hash = StringEx.decode(StringEx.parse(user, "HASH"));            // 로그인 해쉬 데이터
				long time = StringEx.str2long(StringEx.parse(user, "TIME"));
				String ip = StringEx.decode(StringEx.parse(user, "IP"));

				if (!(seq == 0 || StringEx.isEmpty(id) || StringEx.isEmpty(name))) {
					DBLibrary dbLib = new DBLibrary(this.logger);
					Connection conn = null;
					CallableStatement cs = null;
					PreparedStatement ps = null;
					ResultSet rs = null;

					//if ((System.currentTimeMillis() - time) / 1000.0 <= 60 * this.cfg.getInt("cookie.expire")) { // {설정}분동안 페이지 이동이 없으면 로그아웃
					if ((System.currentTimeMillis() - time) / 1000.0 <= 60 * com.nucco.VmmsEnv.getInstance().getInt("cookie.expire")) { // {설정}분동안 페이지 이동이 없으면 로그아웃
						if (ip.equals(request.getRemoteAddr())) { // 로그인 아이피 체크
							String cookie = Base64.encode((new Cryptograph()).encrypt("SEQ=" + seq
								+ "&ID=" + StringEx.encode(id)
								+ "&NAME=" + StringEx.encode(name)
								+ "&AUTH=" + auth
								+ "&COMPANY=" + company
								+ "&ORGAN=" + organ
								+ "&ORGAN.DEPTH=" + organDepth
								+ "&OPERATOR=" + StringEx.encode(operator)
								+ "&HASH=" + StringEx.encode(hash)
								+ "&TIME=" + System.currentTimeMillis()
								+ "&IP=" + StringEx.encode(ip)));

						// -- 2011.05.19, 계정 인증 정보를 쿠키에서 세션으로 변경
						//	CookieEx.set(response, "MEM", cookie, "/");
							session.setAttribute("MEM", cookie);
							session.setMaxInactiveInterval(60 * com.nucco.VmmsEnv.getInstance().getInt("cookie.expire"));
						// --

							this.cfg.put("user.seq", seq);
							this.cfg.put("user.id", id);
							this.cfg.put("user.name", name);
							this.cfg.put("user.auth", auth);
							this.cfg.put("user.company", company);
							this.cfg.put("user.organ", organ);
							this.cfg.put("user.organ.depth", organDepth);
							this.cfg.put("user.operator", operator);
							this.cfg.put("user.hash", hash);

							// ----------------------------------------------- // 허가된 메뉴 목록 //

							if (!StringEx.isEmpty(location)) {
								conn = dbLib.getConnection(com.nucco.VmmsEnv.getInstance().get("db.jdbc.name"), com.nucco.VmmsEnv.getInstance().get("db.jdbc.host"), com.nucco.VmmsEnv.getInstance().get("db.jdbc.user"), com.nucco.VmmsEnv.getInstance().get("db.jdbc.pass"));

								if (conn == null) {
									return;
								}

								this.mMenu = new ArrayList<GeneralConfig>();
								this.sMenu = new ArrayList<GeneralConfig>();

								try {
									System.out.println("auth : "+auth+"location : "+location+"company : "+company);
									cs = dbLib.prepareCall(conn, "{ CALL PG_AUTH.MENU (?, ?, ?, ?) }");
									cs.setLong(1, auth);
									cs.setString(2, location);
									cs.setString(3, company == 0 ? "Y" : "N");
									cs.registerOutParameter(4, OracleTypes.CURSOR);
									cs.execute();

									rs = (ResultSet) cs.getObject(4);

									while (rs.next()) {
										GeneralConfig c = new GeneralConfig();

										c.put("SEQ", rs.getString("SEQ"));
										c.put("PARENT_SEQ", rs.getString("PARENT_SEQ"));
										c.put("DEPTH", rs.getInt("DEPTH"));
										c.put("NAME", rs.getString("NAME"));
										c.put("SRC", rs.getString("SRC"));
										c.put("ENABLE_I", rs.getString("ENABLE_I"));
										c.put("ENABLE_U", rs.getString("ENABLE_U"));
										c.put("ENABLE_D", rs.getString("ENABLE_D"));
										c.put("ENABLE_S", rs.getString("ENABLE_S"));
										c.put("IS_SELECTED", rs.getString("IS_SELECTED"));
										if (rs.getString("IS_SELECTED").equals("Y")) {
											this.cfg.put("ENABLE_I", rs.getString("ENABLE_I"));
											this.cfg.put("ENABLE_U", rs.getString("ENABLE_U"));
											this.cfg.put("ENABLE_D", rs.getString("ENABLE_D"));
											this.cfg.put("ENABLE_S", rs.getString("ENABLE_S"));
										}

										if (rs.getInt("DEPTH") == 0) {
											this.mMenu.add(c);
										} else {
											this.sMenu.add(c);
										}
									}
								} catch (Exception e) {
									this.logger.error(e);
								} finally {
									dbLib.close(rs);
									dbLib.close(cs);
								}

								dbLib.close(conn);
							}
						}
					} else {
						conn = dbLib.getConnection(com.nucco.VmmsEnv.getInstance().get("db.jdbc.name"), com.nucco.VmmsEnv.getInstance().get("db.jdbc.host"), com.nucco.VmmsEnv.getInstance().get("db.jdbc.user"), com.nucco.VmmsEnv.getInstance().get("db.jdbc.pass"));

						if (conn == null) {
							return;
						}

						try {
//20160221 INDEX 힌트 추가
//							ps = dbLib.prepareStatement(conn, "UPDATE TB_USER_LOG SET OUT_DATE = TO_CHAR(SYSDATE, 'YYYYMMDD'), OUT_TIME = TO_CHAR(SYSDATE, 'HH24MISS'), OUT_IP = ? WHERE SEQ = ? AND HASH_DATA = ?");
							ps = dbLib.prepareStatement(conn,
									"UPDATE /*+ INDEX(A) */ TB_USER_LOG"
										+ " SET OUT_DATE = TO_CHAR(SYSDATE, 'YYYYMMDD'),"
											+ " OUT_TIME = TO_CHAR(SYSDATE, 'HH24MISS'),"
											+ " OUT_IP = ?"
										+ " WHERE SEQ = ?"
											+ " AND HASH_DATA = ?"
								);
							ps.setString(1, request.getRemoteAddr());
							ps.setLong(2, seq);
							ps.setString(3, hash);
							ps.executeUpdate();
						} catch (Exception e) {
							this.logger.error(e);
						} finally {
							dbLib.close(ps);
						}

						dbLib.close(conn);
					}
				}
			}
		} catch (Exception e) {
			this.logger.error(e);
		}
	}
/**
 * get HttpServletRequest
 *
 * @return javax.servlet.http.HttpServletRequest
 *
 */
	public HttpServletRequest getRequest() {
		return this.request;
	}
/**
 * get HttpServletResponse
 *
 * @return javax.servlet.http.HttpServletResponse
 *
 */
	public HttpServletResponse getResponse() {
		return this.response;
	}
/**
 * get HttpSession
 *
 * @return javax.servlet.http.HttpSession
 *
 */
	public HttpSession getSession() {
		return this.session;
	}
/**
 * get properties :: string
 *
 * @param key keyword
 * @return string
 *
 */
	public String getString(String key) {
		return this.cfg.getString(key);
	}
/**
 * get properties :: boolean
 *
 * @param key keyword
 * @return boolean
 *
 */
	public boolean getBoolean(String key) {
		return this.cfg.getBoolean(key);
	}
/**
 * get properties :: int
 *
 * @param key keyword
 * @return int
 *
 */
	public int getInt(String key) {

		//return this.cfg.getInt(key);
		String strReturnval = com.nucco.VmmsEnv.getInstance().get(key);

		if(strReturnval == null)
		{
			strReturnval = this.getString(key);
		}

		return Integer.parseInt(strReturnval);
	}
/**
 * get properties :: long
 *
 * @param key keyword
 * @return long
 *
 */
	public long getLong(String key) {
		return this.cfg.getLong(key);
	}
/**
 * get properties :: java.util.Properties
 *
 * @return java.util.Properties
 *
 */
	public Properties getProperties() {
		return this.cfg.getProperties();
	}
/**
 * get properties
 *
 * @param key keyword
 * @return string
 *
 */
	public String get(String key) {
		//return this.getString(key);
		String strReturnval = com.nucco.VmmsEnv.getInstance().get(key);
		if(strReturnval == null)
		{
			strReturnval = this.getString(key);
		}
		return strReturnval;
	}
/**
 * put properties :: string
 *
 * @param key keyword
 * @param val value
 * @return void
 *
 */
	public void put(String key, String val) {
		this.cfg.put(key, val);
	}
/**
 * put properties :: boolean
 *
 * @param key keyword
 * @param val value
 * @return void
 *
 */
	public void put(String key, boolean val) {
		this.cfg.put(key, val);
	}
/**
 * put properties :: int
 *
 * @param key keyword
 * @param val value
 * @return void
 *
 */
	public void put(String key, int val) {
		this.cfg.put(key, val);
	}
/**
 * put properties :: long
 *
 * @param key keyword
 * @param val value
 * @return void
 *
 */
	public void put(String key, long val) {
		this.cfg.put(key, val);
	}
/**
 * 로그인 페이지 읽기
 *
 * @return string
 *
 */
	public String login() {
		String goUrl = this.request.getRequestURI() + (this.request.getQueryString() == null ? "" : "?" + this.request.getQueryString());

		if (request.getMethod().equals("POST")) {
			goUrl = "";
		}

		return this.login(goUrl);
	}
/**
 * 로그인 페이지 읽기
 *
 * @param goUrl 이동할 경로
 * @return string
 *
 */
	public String login(String goUrl) {
		//return URLReader.read(com.nucco.VmmsEnv.getInstance().get("url") + com.nucco.VmmsEnv.getInstance().get("script.login") + "?goUrl=" + StringEx.encode(goUrl));
		System.out.println("goUrl : [" + goUrl + "]");
		System.out.println("loginurl : [" + com.nucco.VmmsEnv.getInstance().get("inurl") + com.nucco.VmmsEnv.getInstance().get("script.login") + "?goUrl=" + StringEx.encode(goUrl) + "]");
		return URLReader.read(com.nucco.VmmsEnv.getInstance().get("inurl") + com.nucco.VmmsEnv.getInstance().get("script.login") + "?goUrl=" + StringEx.encode(goUrl));
	}
/**
 * 로그인 여부
 *
 * @return boolean
 *
 */
	public boolean isLogin() {
		return this.cfg.getLong("user.seq") > 0;
	}
/**
 * 접근 가능 여부
 *
 * @return boolean
 *
 */
	public boolean isAuth() {
		return this.isAuth("S");
	}
/**
 * 접근 가능 여부
 *
 * @param mode 체크 유형
 * @return boolean
 *
 */
	public boolean isAuth(String mode) {
		return this.cfg.get("ENABLE_" + mode).equals("Y");
	}
}
