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

			// 转码虚拟机自动伸缩
			TranscodeInstanceService.doAutoScaling();

			MessageListenerDeamon.DEBUGGER.info("end to check auto scaling");

			// 获取消息请求检查时间间隔，单位：秒
			int queueMessageCheckDuration = 60;
			String queueMessageCheckDurationStr = CommonService.getAutoScalingConfValue("queueMessageCheckDuration");
			if (CommonService.isStringNotNull(queueMessageCheckDurationStr))
			{
				queueMessageCheckDuration = Integer.valueOf(queueMessageCheckDurationStr);
			}

			// 等待一段时间之后再检查
			MessageListenerDeamon.DEBUGGER.debug("going to sleep " + queueMessageCheckDuration + " seconds");
			CommonService.doSleep(queueMessageCheckDuration);
		}
	}
}
