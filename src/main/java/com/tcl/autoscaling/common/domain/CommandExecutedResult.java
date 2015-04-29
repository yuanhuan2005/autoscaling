package com.tcl.autoscaling.common.domain;

public class CommandExecutedResult
{
	/**
	 * 命令执行结果返回码，0表示成功，非0表示失败
	 */
	private int exitValue;

	/**
	 * 返回消息，当命令执行成功时返回输出信息，执行失败时返回错误信息
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
