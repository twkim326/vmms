// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name:   WarningException.java

package com.nucco.lib;


public class WarningException extends Exception
{
	private static final long serialVersionUID = 1L;

	public WarningException(String ErrMsg)
    {
        super(ErrMsg);
        WarningMsg = "";
        WarningCode = "";
        WarningMsg = ErrMsg;
    }

    public WarningException(String ErrCd, String ErrMsg)
    {
        WarningMsg = "";
        WarningCode = "";
        WarningMsg = ErrMsg;
        WarningCode = ErrCd;
    }

    public String WarningMsg;
    public String WarningCode;
}
