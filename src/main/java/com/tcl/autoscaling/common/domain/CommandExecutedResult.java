package com.tcl.autoscaling.common.domain;

public class CommandExecutedResult
{
	/**
	 * ����ִ�н�������룬0��ʾ�ɹ�����0��ʾʧ��
	 */
	private int exitValue;

	/**
	 * ������Ϣ��������ִ�гɹ�ʱ���������Ϣ��ִ��ʧ��ʱ���ش�����Ϣ
	 */
	private String resultMessage;

	public int getExitValue()
	{
		return exitValue;
	}

	public void setExitValue(int exitValue)
	{
		this.exitValue = exitValue;
	}

	public String getResultMessage()
	{
		return resultMessage;
	}

	public void setResultMessage(String resultMessage)
	{
		this.resultMessage = resultMessage;
	}

	@Override
	public String toString()
	{
		return "CommandExecutedResult [exitValue=" + exitValue + ", resultMessage=" + resultMessage + "]";
	}

}
