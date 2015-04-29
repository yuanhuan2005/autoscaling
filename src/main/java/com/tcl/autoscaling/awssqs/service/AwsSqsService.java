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
 * ����AWS��SQS����Ŀͻ�����
 * 
 * @author yuanhuan
 * 
 */
public class AwsSqsService
{
	final static private Log DEBUGGER = LogFactory.getLog(AwsSqsService.class);

	/**
	 * ��ȡSQS�ӿڵ��õĿͻ���
	 * 
	 * @return SQS�ӿڵ��õĿͻ���
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
	 * �������ļ��л�ȡȷ����Ϣ���е�URL
	 * 
	 * @return ȷ����Ϣ���е�URL
	 */
	public static String getResponseSqsURL()
	{
		String responseQueueURL = CommonService.getAutoScalingConfValue("responseQueueURL");

		return responseQueueURL;
	}

	/**
	 * �������ļ��л�ȡrequestQueueURL���е�URL
	 * 
	 * @return requestQueueURL ���е�URL
	 */
	public static String getRequestSqsURL()
	{
		String requestQueueURL = CommonService.getAutoScalingConfValue("requestQueueURL");

		return requestQueueURL;
	}

	/**
	 * ��Queue�н���һ����Ϣ
	 * 
	 * @param queueUrl
	 *            Queue��ַ
	 * @return ��Ϣ����
	 */
	public static Message receiveMessage(String queueUrl)
	{
		AwsSqsService.DEBUGGER.debug("enter receiveMessage");

		List<Message> messageList = null;
		Message message = null;

		try
		{
			// ������Ϣ
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
	 * ��queue��ɾ��һ����Ϣ
	 * 
	 * @param sqs
	 *            SQS�ͻ���
	 * @param queueUrl
	 *            Queue��ַ
	 * @param receiptHandle
	 *            ����ƾ��
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
	 * ��Queue������Ϣ
	 * 
	 * @param sqs
	 *            SQS�ͻ���
	 * @param queueUrl
	 *            Queue��ַ
	 * @param messageBody
	 *            ��Ϣ��
	 * @return ���ͽ��
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
