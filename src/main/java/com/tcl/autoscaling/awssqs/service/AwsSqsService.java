package com.tcl.autoscaling.awssqs.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.tcl.autoscaling.common.service.CommonService;

/**
 * 调用AWS的SQS服务的客户端类
 * 
 * @author yuanhuan
 * 
 */
public class AwsSqsService
{
	final static private Log DEBUGGER = LogFactory.getLog(AwsSqsService.class);

	/**
	 * 获取SQS接口调用的客户端
	 * 
	 * @return SQS接口调用的客户端
	 */
	public static AmazonSQS getAmazonSqsClient()
	{
		if (null == AwsSqsClient.getInstance() || null == AwsSqsClient.getInstance().getAmazonSQS())
		{
			String awsAccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
			String awsSecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
			BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
			return new AmazonSQSClient(credentials);
		}

		return AwsSqsClient.getInstance().getAmazonSQS();
	}

	/**
	 * 从配置文件中获取确认消息队列的URL
	 * 
	 * @return 确认消息队列的URL
	 */
	public static String getResponseSqsURL()
	{
		String responseQueueURL = CommonService.getAutoScalingConfValue("responseQueueURL");

		return responseQueueURL;
	}

	/**
	 * 从配置文件中获取requestQueueURL队列的URL
	 * 
	 * @return requestQueueURL 队列的URL
	 */
	public static String getRequestSqsURL()
	{
		String requestQueueURL = CommonService.getAutoScalingConfValue("requestQueueURL");

		return requestQueueURL;
	}

	/**
	 * 从Queue中接收一个消息
	 * 
	 * @param queueUrl
	 *            Queue地址
	 * @return 消息对象
	 */
	public static Message receiveMessage(String queueUrl)
	{
		AwsSqsService.DEBUGGER.debug("enter receiveMessage");

		List<Message> messageList = null;
		Message message = null;

		try
		{
			// 接收消息
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
			receiveMessageRequest.setMaxNumberOfMessages(1);
			receiveMessageRequest.setVisibilityTimeout(43200);
			Collection<String> attributeNames = new ArrayList<String>();
			attributeNames.add("All");
			receiveMessageRequest.setAttributeNames(attributeNames);
			receiveMessageRequest.setQueueUrl(queueUrl);
			AmazonSQS sqs = AwsSqsService.getAmazonSqsClient();
			ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest);
			if (null == receiveMessageResult)
			{
				AwsSqsService.DEBUGGER.info("Failed to get receiveMessageResult: receiveMessageResult is null");
				AwsSqsService.DEBUGGER.debug("end receiveMessage");
				return message;
			}

			messageList = receiveMessageResult.getMessages();
			if (null == messageList || messageList.isEmpty())
			{
				AwsSqsService.DEBUGGER.info("message list is empty");
				AwsSqsService.DEBUGGER.debug("end receiveMessage");
				return message;
			}

			AwsSqsService.DEBUGGER.info("at least 1 messages received");
			AwsSqsService.DEBUGGER.info("begin to handle with this message");
			AwsSqsService.DEBUGGER.debug("message list size is " + messageList.size());
			AwsSqsService.DEBUGGER.debug("messageList : " + messageList);
		}
		catch (Exception e)
		{
			AwsSqsService.DEBUGGER.error("Failed to receiveMessage. Exception: " + e.toString());
			AwsSqsService.DEBUGGER.debug("end receiveMessage");
			return message;
		}

		AwsSqsService.DEBUGGER.debug("end receiveMessage");
		return messageList.get(0);
	}

	/**
	 * 从queue中删除一条消息
	 * 
	 * @param sqs
	 *            SQS客户端
	 * @param queueUrl
	 *            Queue地址
	 * @param receiptHandle
	 *            处理凭据
	 */
	public static void deleteMessage(String queueUrl, String receiptHandle)
	{
		AwsSqsService.DEBUGGER.debug("enter deleteMessage");

		try
		{
			// Delete a message from queue
			DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest();
			deleteMessageRequest.setQueueUrl(queueUrl);
			deleteMessageRequest.setReceiptHandle(receiptHandle);
			AmazonSQS sqs = AwsSqsService.getAmazonSqsClient();
			sqs.deleteMessage(deleteMessageRequest);
			AwsSqsService.DEBUGGER.info("massage is deleted successfully");
		}
		catch (Exception e)
		{
			AwsSqsService.DEBUGGER.error("Failed to deleteMessage. Exception: " + e.toString());
		}

		AwsSqsService.DEBUGGER.debug("end deleteMessage");
	}

	/**
	 * 给Queue发送消息
	 * 
	 * @param sqs
	 *            SQS客户端
	 * @param queueUrl
	 *            Queue地址
	 * @param messageBody
	 *            消息体
	 * @return 发送结果
	 */
	public static SendMessageResult sendMessage(String queueUrl, String messageBody)
	{
		AwsSqsService.DEBUGGER.debug("enter sendMessage");
		AwsSqsService.DEBUGGER.debug("messageBody: " + messageBody);

		SendMessageResult sendMessageResult = null;
		try
		{
			// Send a message to queue
			SendMessageRequest sendMessageRequest = new SendMessageRequest();
			sendMessageRequest.setQueueUrl(queueUrl);
			sendMessageRequest.setMessageBody(messageBody);
			AmazonSQS sqs = AwsSqsService.getAmazonSqsClient();
			sendMessageResult = sqs.sendMessage(sendMessageRequest);
			if (null != sendMessageResult)
			{
				AwsSqsService.DEBUGGER.info("massage is sent successfully");
				AwsSqsService.DEBUGGER.debug("sendMessageResult: " + sendMessageResult.toString());
			}
			else
			{
				AwsSqsService.DEBUGGER.info("failed to send massage");
			}
		}
		catch (Exception e)
		{
			AwsSqsService.DEBUGGER.error("failed to send message. Exception: " + e.toString());
			return null;
		}

		AwsSqsService.DEBUGGER.debug("end sendMessage");
		return sendMessageResult;
	}

	public static String getFaceDetectionBatFileName()
	{
		String faceDetectionBatFileName = CommonService.getAutoScalingConfValue("faceDetectionBatFileName");
		return faceDetectionBatFileName;
	}

	public static String getFaceDetectionProjectRootPath()
	{
		String faceDetectionRoot = CommonService.getAutoScalingConfValue("faceDetectionProjectRoot");
		return faceDetectionRoot;
	};

	public static String getFaceDetectionKeyFramesPercent()
	{
		String percent = CommonService.getAutoScalingConfValue("faceDetectionKeyFramesPercent");
		return percent;
	}

	public static String getFaceDetectionMaunalLabelPersonEmail()
	{
		String email = CommonService.getAutoScalingConfValue("faceDetectionMaunalLabelPersonEmail");
		return email;
	}
}
