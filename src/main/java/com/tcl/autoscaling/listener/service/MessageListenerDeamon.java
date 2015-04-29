package com.tcl.autoscaling.listener.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcl.autoscaling.common.service.CommonService;
import com.tcl.autoscaling.transcode.service.TranscodeInstanceService;

public class MessageListenerDeamon extends Thread
{
	final static private Log DEBUGGER = LogFactory.getLog(MessageListenerDeamon.class);

	public MessageListenerDeamon()
	{
		setDaemon(true);
	}

	@Override
	public void run()
	{
		while (true)
		{
			MessageListenerDeamon.DEBUGGER.info("begin to check auto scaling");

			// ת��������Զ�����
			TranscodeInstanceService.doAutoScaling();

			MessageListenerDeamon.DEBUGGER.info("end to check auto scaling");

			// ��ȡ��Ϣ������ʱ��������λ����
			int queueMessageCheckDuration = 60;
			String queueMessageCheckDurationStr = CommonService.getAutoScalingConfValue("queueMessageCheckDuration");
			if (CommonService.isStringNotNull(queueMessageCheckDurationStr))
			{
				queueMessageCheckDuration = Integer.valueOf(queueMessageCheckDurationStr);
			}

			// �ȴ�һ��ʱ��֮���ټ��
			MessageListenerDeamon.DEBUGGER.debug("going to sleep " + queueMessageCheckDuration + " seconds");
			CommonService.doSleep(queueMessageCheckDuration);
		}
	}
}
