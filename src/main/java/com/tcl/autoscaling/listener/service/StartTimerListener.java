package com.tcl.autoscaling.listener.service;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class StartTimerListener implements ServletContextListener
{
	private MessageListenerDeamon messageListenerDeamon = null;

	/**
	 * ����һ����ʼ������������һ������������
	 */
	public StartTimerListener()
	{
		super();
	}

	/**
	 * ��Web�������е�ʱ���Զ�����Timer
	 */
	@Override
	public void contextInitialized(ServletContextEvent e)
	{
		if (null == messageListenerDeamon)
		{
			messageListenerDeamon = new MessageListenerDeamon();
		}

		messageListenerDeamon.start();
	}

	/**
	 * �÷������������� ��ʵ��
	 */
	@Override
	public void contextDestroyed(ServletContextEvent e)
	{
		if (null != messageListenerDeamon)
		{
			messageListenerDeamon.interrupt();
		}
	}
}
