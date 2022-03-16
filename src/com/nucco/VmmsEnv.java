/**
 * <p>Package: com.nucco</p>
 * <p>Title: VmmsEnv.java</p>
 * <p>Description: 환경변수에 대한 singleton 객체</p>
 * <p>Copyright: Copyright (c) 2011. 6. 2.</p>
 * <p>Company: UBC</p>
 * <p>History  : 2011. 6. 2., UBC, v1.0, 최초작성 </p>
 * <p>created by Moonbong Choi</p
 * <p>modified by Moonbong Choi</p>
 * @author Moonbong Choi
 * @version 1.0
 */
package com.nucco;
import java.io.File;
import java.util.*;

/**
 * @author manbong_cp_ubc
 *
 */
public class VmmsEnv {
	
	/**
	 * singleton instance 객체
	 */
	private static VmmsEnv uniqueInstance = new VmmsEnv();

	/**
	 * singleton instance 내부에 Object 등록소
	 */
	private java.util.Hashtable<String, Properties> uniqueRegister;
	
	/**
	 * 초기화한 환경파일 경로 및 파일명정보.
	 */
	private String strEnvFileName;
	/**
	 * 환경파일 초기화 시간정보
	 */
	protected long lastModified = 0;
	
	
    /**
     * 생성자
     */
    private VmmsEnv() {
    	uniqueRegister = new java.util.Hashtable<String, Properties>();
    } 
     
    /**
     * 환경정보 instance를 반환한다.
     * @return 환경정보 instance
     */
    public static VmmsEnv getInstance() { 
       return uniqueInstance; 
    }
    
    /**
     * 환경파일 초기화 시간정보를 반환한다.
     * @return 환경파일 초기화 시간정보.
     */
    public long getLastModified()
    {
    	return lastModified;
    }
    
    /**
     * 초기화한 환경파일 경로+화일명정보를 반환한다.
     * @return 환경파일 경로+화일명
     */
    public String getstrEnvFileName()
    {
    	return strEnvFileName;
    }
    
    /**
     * 환경변수명에 해당하는 값을 String으로 반환한다.
     * @param strName 환경변수 명
     * @return String 환경변수 값
     */
    public String get(String strName)
    {
    	java.util.Properties propEnv = uniqueRegister.get("ENVProp");
    	
    	return propEnv.getProperty(strName);
    }
    
    /**
     *  환경변수명에 해당하는 값을 int로  반환한다.
     * @param strName 환경변수 명
     * @return int 환경변수 값
     */
    public int getInt(String strName)
    {
    	java.util.Properties propEnv = uniqueRegister.get("ENVProp");
    	
    	return Integer.parseInt(propEnv.getProperty(strName));
    }
    
    /**
     * 환경파일객체가 초기화 되었는지 값을 반환한다.
     * @param strParamEnvFileName 환경파일명
     * @return 환경파일 초기화 true, 미초기화시 false
     */
    public boolean checkInit(String strParamEnvFileName)
    {
    	java.util.Properties propEnv = uniqueRegister.get("ENVProp");
    	if(propEnv == null){
    		return false;
    	}
    	else
    	{    	
    		return true;
    	}
    }
    
    /**
     * 환경파일을 초기화 한다.
     * 
     * @param strParamEnvFileName 환경파일 경로+명칭
     */
    public void Init(String strParamEnvFileName)
    {
    	System.out.println("---------------------ENVProp init start!-----------------------");
    	uniqueRegister.remove("ENVProp");
		java.io.FileInputStream stream = null;
		java.io.BufferedInputStream buffer = null;

    	java.util.Properties propEnv = new Properties();
    	strEnvFileName = strParamEnvFileName;
		try {
			
			File file = new File(strEnvFileName);
			stream = new java.io.FileInputStream(file);
			buffer = new java.io.BufferedInputStream(stream);


	    	propEnv.load(buffer);
			lastModified = file.lastModified();
			
			uniqueRegister.put("ENVProp", propEnv);
			
		} catch (Exception e) {
			e.printStackTrace();
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
		
		System.out.println("---------------------ENVProp init End!-----------------------");
    }
}
