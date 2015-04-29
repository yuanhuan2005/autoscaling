package com.tcl.autoscaling.transcode.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.tcl.autoscaling.awsec2.service.AwsEC2Service;
import com.tcl.autoscaling.awsses.service.AwsSesService;
import com.tcl.autoscaling.awssqs.service.AwsSqsService;
import com.tcl.autoscaling.common.service.CommonService;
import com.tcl.autoscaling.listener.service.MessageListenerDeamon;

public class TranscodeInstanceService
{
	final static private Log DEBUGGER = LogFactory.getLog(MessageListenerDeamon.class);

	/**
	 * ����Ƿ���Ҫ�����µ������
	 * 
	 * @param approximateNumberOfMessages
	 *            SQS��Ϣ����
	 * @return true��ʾ��Ҫ�����µ��������false��ʾ����Ҫ�����������
	 */
	private static boolean needToLanchNewInstances(int approximateNumberOfMessages)
	{
		int transcodeMonitorQueueTotalNumberThreshold = Integer.valueOf(CommonService
		        .getAutoScalingConfValue("transcodeMonitorQueueTotalNumberThreshold"));
		TranscodeInstanceService.DEBUGGER.debug("transcodeMonitorQueueTotalNumberThreshold="
		        + transcodeMonitorQueueTotalNumberThreshold);
		if (approximateNumberOfMessages >= transcodeMonitorQueueTotalNumberThreshold)
		{
			TranscodeInstanceService.DEBUGGER
			        .info("current queue mesaages is great than threshold, need to launch new instances");
			return true;
		}

		return false;
	}

	/**
	 * ��ȡSQS��Ϣ����
	 * 
	 * @return ��Ϣ����
	 */
	private static int getApproximateNumberOfMessages()
	{
		String transcodeMonitorQueueURL = CommonService.getAutoScalingConfValue("transcodeMonitorQueueURL");
		AmazonSQS sqs = AwsSqsService.getAmazonSqsClient();
		GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest();
		getQueueAttributesRequest.setQueueUrl(transcodeMonitorQueueURL);
		Collection<String> attributeNames = new ArrayList<String>();

		attributeNames.add("ApproximateNumberOfMessages");
		getQueueAttributesRequest.setAttributeNames(attributeNames);
		GetQueueAttributesResult getQueueAttributesResult = sqs.getQueueAttributes(getQueueAttributesRequest);
		int approximateNumberOfMessages = Integer.valueOf(getQueueAttributesResult.getAttributes().get(
		        "ApproximateNumberOfMessages"));
		TranscodeInstanceService.DEBUGGER.debug("approximateNumberOfMessages=" + approximateNumberOfMessages);

		return approximateNumberOfMessages;
	}

	/**
	 * �����ʼ�֪ͨ�������µ������
	 * 
	 * @param toEmailAddress
	 *            Email��ַ
	 * @param message
	 *            �ʼ���Ϣ
	 */
	private static void sendNotificationEmails(String toEmailAddress, String subject, String message)
	{
		TranscodeInstanceService.DEBUGGER.debug("begin to send notification emails");
		TranscodeInstanceService.DEBUGGER.debug("toEmailAddress : " + toEmailAddress);
		TranscodeInstanceService.DEBUGGER.debug("message : " + message);
		try
		{
			AwsSesService.sendEmail(toEmailAddress, subject, message);
		}
		catch (Exception e)
		{
			TranscodeInstanceService.DEBUGGER.error("Exception: " + e.toString());
		}

		TranscodeInstanceService.DEBUGGER.debug("end to send notification emails");
	}

	private static String getInstanceName(Instance instance)
	{
		String instanceName = "";
		if (null != instance.getTags() && !instance.getTags().isEmpty())
		{
			for (Tag tag : instance.getTags())
			{
				if ("Name".equalsIgnoreCase(tag.getKey()))
				{
					instanceName = tag.getValue();
				}
			}
		}

		return instanceName;
	}

	/**
	 * ��ȡ�ϸ���������иո�ɾ�����������ID�б�
	 * 
	 * @param notTerminatedInstanceIdsList
	 *            δɾ���������ID�б�
	 * @param lastTimeInstanceIdsList
	 *            �ϴμ�¼��������б�
	 * @return
	 */
	private static List<String> getTerminatedInstanceIdsList(List<String> notTerminatedInstanceIdsList,
	        List<String> lastTimeInstanceIdsList)
	{
		List<String> terminatedInstanceIdsList = new ArrayList<String>();

		TranscodeInstanceService.DEBUGGER.debug("notTerminatedInstanceIdsList: "
		        + notTerminatedInstanceIdsList.toString());
		TranscodeInstanceService.DEBUGGER.debug("lastTimeInstanceIdsList: " + lastTimeInstanceIdsList.toString());

		for (String lastTimeInstanceId : lastTimeInstanceIdsList)
		{
			if (CommonService.isStringNull(lastTimeInstanceId))
			{
				continue;
			}

			boolean terminatedFlag = true;
			for (String notTerminatedInstanceId : notTerminatedInstanceIdsList)
			{
				if (lastTimeInstanceId.equals(notTerminatedInstanceId))
				{
					terminatedFlag = false;
					break;
				}
			}

			if (terminatedFlag)
			{
				terminatedInstanceIdsList.add(lastTimeInstanceId);
			}
		}

		return terminatedInstanceIdsList;
	}

	/**
	 * ����ǰ�������ID�б�д���ļ�
	 */
	private static void updateLastTimeInstanceIdsFile(List<String> notTerminatedInstanceIdsList)
	{
		String path = CommonService.getWebInfPath() + "/WEB-INF/classes/instancesIds.txt";

		// ����ɾ����ID�ļ�
		File file = new File(path);
		if (file.exists())
		{
			file.delete();
		}

		FileWriter fw = null;
		PrintWriter pw = null;
		try
		{
			fw = new FileWriter(path, true);
			pw = new PrintWriter(fw);
			for (String instanceId : notTerminatedInstanceIdsList)
			{
				pw.println(instanceId);
			}
			pw.close();
			fw.close();
		}
		catch (IOException e)
		{
			TranscodeInstanceService.DEBUGGER.error("Exception: " + e.toString());
		}
		finally
		{
			try
			{
				if (null != pw)
				{
					pw.close();
				}
				if (null != fw)
				{
					fw.close();
				}
			}
			catch (Exception e)
			{
				TranscodeInstanceService.DEBUGGER.error("Exception: " + e.toString());
			}
		}
	}

	/**
	 * ���ϴεļ�¼�л�ȡ�����ID�б�
	 * 
	 * @return �ϴεļ�¼�������ID�б�
	 */
	private static List<String> getLastTimeInstanceIdsList()
	{
		List<String> lastTimeInstanceIdsList = new ArrayList<String>();

		String path = CommonService.getWebInfPath() + "/WEB-INF/classes/instancesIds.txt";
		TranscodeInstanceService.DEBUGGER.debug("instancesIds file path=" + path);
		File file = new File(path);
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;

			// һ�ζ���һ�У�ֱ������nullΪ�ļ�����
			while ((tempString = reader.readLine()) != null)
			{
				lastTimeInstanceIdsList.add(tempString);
			}
			reader.close();
		}
		catch (IOException e)
		{
			TranscodeInstanceService.DEBUGGER.error("Exception: " + e.toString());
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e1)
				{
				}
			}
		}

		return lastTimeInstanceIdsList;
	}

	/**
	 * ��ȡδɾ���������ID�б�
	 * 
	 * @return δɾ���������ID�б�
	 */
	private static List<String> getNotTerminatedInstanceIdsList()
	{
		List<String> instanceIdsList = new ArrayList<String>();
		List<Instance> allTranscodingInstancesList = TranscodeInstanceService.getAllTranscodingInstancesList();
		if (null == allTranscodingInstancesList || allTranscodingInstancesList.isEmpty())
		{
			return instanceIdsList;
		}

		for (Instance instance : allTranscodingInstancesList)
		{
			if (null == instance)
			{
				continue;
			}

			if (!"terminated".equals(instance.getState().getName()))
			{
				instanceIdsList.add(instance.getInstanceId());
			}
		}

		return instanceIdsList;
	}

	/**
	 * ��ȡ����������б�
	 * 
	 * @return ���е�������б�
	 */
	private static List<Instance> getAllTranscodingInstancesList()
	{
		List<Instance> allTranscodingInstancesList = new ArrayList<Instance>();

		try
		{
			DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
			Collection<Filter> filters = new ArrayList<Filter>();
			Filter filter = new Filter();
			filter.setName("image-id");
			Collection<String> values = new ArrayList<String>();
			String imageId = CommonService.getAutoScalingConfValue("transcodeImageIdToLanchInstances");
			values.add(imageId);
			filter.setValues(values);
			filters.add(filter);
			describeInstancesRequest.setFilters(filters);
			DescribeInstancesResult describeInstancesResult = AwsEC2Service.getAmazonEC2Client().describeInstances(
			        describeInstancesRequest);

			if (null != describeInstancesResult && null != describeInstancesResult.getReservations()
			        && !describeInstancesResult.getReservations().isEmpty())
			{
				List<Reservation> reservationList = describeInstancesResult.getReservations();
				for (Reservation reservation : reservationList)
				{
					if (null == reservation)
					{
						continue;
					}

					List<Instance> instancesList = reservation.getInstances();
					if (null == instancesList || instancesList.isEmpty())
					{
						continue;
					}

					for (Instance instance : instancesList)
					{
						allTranscodingInstancesList.add(instance);
					}
				}
			}
		}
		catch (AmazonServiceException e)
		{
			TranscodeInstanceService.DEBUGGER.error("Exception getMessage: " + e.getMessage());
		}

		return allTranscodingInstancesList;
	}

	/**
	 * ��ȡ�ƶ�״̬��������б�
	 * 
	 * @param stateName
	 *            �����״̬
	 * @return ����������������б�
	 */
	private static String getInstancesListString(String stateName)
	{
		StringBuilder messageBuilder = new StringBuilder();
		int totalNum = 0;
		String emptyStr = "        empty";

		List<Instance> instancesList = TranscodeInstanceService.getAllTranscodingInstancesList();
		if (null == instancesList || instancesList.isEmpty())
		{
			return emptyStr;
		}

		for (Instance instance : instancesList)
		{
			if (null == instance)
			{
				continue;
			}

			if (stateName.equals(instance.getState().getName()))
			{
				totalNum++;
				messageBuilder.append("    No." + totalNum + ": InstanceId=" + instance.getInstanceId() + ", ");

				messageBuilder.append("InstanceName=" + TranscodeInstanceService.getInstanceName(instance) + ", ");
				messageBuilder.append("InstanceType=" + instance.getInstanceType() + ", ");
				messageBuilder.append("PublicIpAddress=" + instance.getPublicIpAddress() + ", ");
				messageBuilder.append("State=" + instance.getState().getName() + ", ");
				messageBuilder.append("LaunchTime=" + instance.getLaunchTime() + "\n");
			}
		}

		if (0 == totalNum)
		{
			return emptyStr;
		}

		return messageBuilder.toString();
	}

	/**
	 * ��ȡScaleUpʱ���ʼ���Ϣ
	 * 
	 * @param transcodeInstanceLanchNumber
	 *            �������������
	 * @return �ʼ���Ϣ
	 */
	private static String getSuccessScaleUpEmailBodyMsg(int transcodeInstanceLanchNumber)
	{
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder
		        .append("    Since instances in transcoding cluster are not enough to do transcoding jobs, we need to launch new EC2 instances. ");
		messageBuilder
		        .append("New instances will be terminated automatically when they are not in use for a few minutes.\n\n");
		messageBuilder.append("    We will launch " + transcodeInstanceLanchNumber
		        + " new instances in transcoding cluster: \n");
		messageBuilder.append(TranscodeInstanceService.getInstancesListString("pending"));
		messageBuilder.append("\n\n    Running instances in transcoding cluster: \n");
		messageBuilder.append(TranscodeInstanceService.getInstancesListString("running"));
		messageBuilder.append("\n\n    Terminated instances in transcoding cluster: \n");
		messageBuilder.append(TranscodeInstanceService.getInstancesListString("terminated"));
		messageBuilder.append("\n\n\n");
		messageBuilder.append("--\n");
		messageBuilder.append("TCL Web Service Team\n");

		return messageBuilder.toString();
	}

	/**
	 * ��ȡ�ɹ�ScaleDownʱ����Ϣ
	 * 
	 * @param terminatedInstanceIdsList
	 *            �Ѿ�ɾ�����������ID�б�
	 * @return �ʼ���Ϣ
	 */
	private static String getSuccessScaleDownEmailBodyMsg(List<String> terminatedInstanceIdsList)
	{
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append("    Some instances has been terminated automatically in the past few minutes.\n");
		messageBuilder.append("    Instance ID(s):\n");

		for (String terminatedInstanceId : terminatedInstanceIdsList)
		{
			messageBuilder.append("      " + terminatedInstanceId + "\n");
		}

		messageBuilder.append("\n\n    Running instances in transcoding cluster: \n");
		messageBuilder.append(TranscodeInstanceService.getInstancesListString("running"));
		messageBuilder.append("\n\n    Terminated instances in transcoding cluster: \n");
		messageBuilder.append(TranscodeInstanceService.getInstancesListString("terminated"));
		messageBuilder.append("\n\n\n");
		messageBuilder.append("--\n");
		messageBuilder.append("TCL Web Service Team\n");

		return messageBuilder.toString();
	}

	/**
	 * �����ʼ�֪ͨ
	 * 
	 * @param emailBodyMsg
	 *            �ʼ�����
	 */
	private static void sendEmailNotificationToEveryone(String subject, String emailBodyMsg)
	{
		String notificationEmails = CommonService.getAutoScalingConfValue("notificationEmails");
		if (CommonService.isStringNull(notificationEmails))
		{
			TranscodeInstanceService.DEBUGGER.debug("notificationEmails is not set, no need to send emails");
			return;
		}

		String[] toEmailAddresses = notificationEmails.split(";");
		for (int i = 0; i < toEmailAddresses.length; i++)
		{
			String toEmailAddress = toEmailAddresses[i];
			StringBuilder messageBuilder = new StringBuilder();
			messageBuilder.append("Hi " + toEmailAddress.substring(0, toEmailAddress.indexOf("@")) + ",\n");
			messageBuilder.append(emailBodyMsg);
			TranscodeInstanceService.sendNotificationEmails(toEmailAddress, subject, messageBuilder.toString());
		}
	}

	/**
	 * ��С�Զ�������
	 */
	private static void doAutoScalingDown()
	{
		TranscodeInstanceService.DEBUGGER.debug("begin to handle with transcode instances auto scaling down");

		List<String> notTerminatedInstanceIdsList = TranscodeInstanceService.getNotTerminatedInstanceIdsList();
		List<String> lastTimeInstanceIdsList = TranscodeInstanceService.getLastTimeInstanceIdsList();

		// ����Ƿ��������ɾ�������еĻ����ʼ�֪ͨ
		List<String> terminatedInstanceIdsList = TranscodeInstanceService.getTerminatedInstanceIdsList(
		        notTerminatedInstanceIdsList, lastTimeInstanceIdsList);

		// �б�Ϊ����ֱ���˳�
		if (null == terminatedInstanceIdsList || terminatedInstanceIdsList.isEmpty())
		{
			// �����ϴε������ID�б��ļ�
			TranscodeInstanceService.updateLastTimeInstanceIdsFile(notTerminatedInstanceIdsList);
			TranscodeInstanceService.DEBUGGER.debug("end to handle with transcode instances auto scaling down");
			return;
		}

		// �����ϴε������ID�б��ļ�
		TranscodeInstanceService.updateLastTimeInstanceIdsFile(notTerminatedInstanceIdsList);

		// ��⵽��������Զ�ɾ������,���ʼ�֪ͨ
		String emailBodyMsg = TranscodeInstanceService.getSuccessScaleDownEmailBodyMsg(terminatedInstanceIdsList);
		String subject = "Terminate instances for transcoding cluster";
		TranscodeInstanceService.sendEmailNotificationToEveryone(subject, emailBodyMsg);
		TranscodeInstanceService.DEBUGGER.debug("end to handle with transcode instances auto scaling down");
	}

	/**
	 * �����ض������������������������
	 * 
	 * @param approximateNumberOfMessages
	 *            SQS��Ϣ����
	 * @return ���������
	 */
	private static int genTranscodeInstanceLanchNumber(int approximateNumberOfMessages)
	{
		if (1 > approximateNumberOfMessages)
		{
			return 0;
		}

		int transcodeInstanceLanchNumber = Integer.valueOf(CommonService
		        .getAutoScalingConfValue("transcodeInstanceLanchNumber"));
		int transcodeMaxInstancesNum = Integer.valueOf(CommonService
		        .getAutoScalingConfValue("transcodeMaxInstancesNum"));
		int newTranscodeInstanceLanchNumber = transcodeInstanceLanchNumber;
		int transcodeMonitorQueueTotalNumberThreshold = Integer.valueOf(CommonService
		        .getAutoScalingConfValue("transcodeMonitorQueueTotalNumberThreshold"));

		// ��������������ĸ�������SQS��Ϣ�����ϴ�ʱ���ഴ��һЩ�����ͬʱ����
		int multiple = approximateNumberOfMessages / transcodeMonitorQueueTotalNumberThreshold / 3;
		if (0 == multiple)
		{
			multiple = 1;
		}
		TranscodeInstanceService.DEBUGGER.debug("multiple=" + multiple);
		newTranscodeInstanceLanchNumber = multiple * transcodeInstanceLanchNumber;
		if (newTranscodeInstanceLanchNumber > transcodeMaxInstancesNum)
		{
			newTranscodeInstanceLanchNumber = transcodeMaxInstancesNum;
		}

		TranscodeInstanceService.DEBUGGER.debug("newTranscodeInstanceLanchNumber=" + newTranscodeInstanceLanchNumber);
		return newTranscodeInstanceLanchNumber;
	}

	/**
	 * ��չ�Զ�������
	 */
	private static void doAutoScalingUp()
	{
		TranscodeInstanceService.DEBUGGER.debug("begin to handle with transcode instances auto scaling up");

		try
		{
			int approximateNumberOfMessages = TranscodeInstanceService.getApproximateNumberOfMessages();
			boolean checkResult = TranscodeInstanceService.needToLanchNewInstances(approximateNumberOfMessages);
			TranscodeInstanceService.DEBUGGER.debug("checkResult: " + checkResult);
			if (!checkResult)
			{
				TranscodeInstanceService.DEBUGGER.debug("no need to launch new instances");
				TranscodeInstanceService.DEBUGGER.debug("end to handle with transcode instances auto scaling up");
				return;
			}

			// ��Ҫ�����µ������
			int transcodeInstanceLanchNumber = TranscodeInstanceService
			        .genTranscodeInstanceLanchNumber(approximateNumberOfMessages);
			int successNum = AwsEC2Service.launchNewInstances(transcodeInstanceLanchNumber);
			if (successNum > 0)
			{
				// ����������ɹ��ˣ������ʼ�֪ͨ
				String emailBodyMsg = TranscodeInstanceService.getSuccessScaleUpEmailBodyMsg(successNum);
				String subject = "Lanch new instances for transcoding cluster";
				TranscodeInstanceService.sendEmailNotificationToEveryone(subject, emailBodyMsg);

				// sleepһ��ʱ��ȴ��´μ��
				int transcodeCoolDownTimeInSeconds = Integer.valueOf(CommonService
				        .getAutoScalingConfValue("transcodeCoolDownTimeInSeconds"));
				TranscodeInstanceService.DEBUGGER
				        .debug("going to sleep " + transcodeCoolDownTimeInSeconds + " seconds");
				Thread.sleep(transcodeCoolDownTimeInSeconds * 1000);
			}
		}
		catch (InterruptedException e)
		{
			TranscodeInstanceService.DEBUGGER.error("Exception: " + e.toString());
		}
		catch (AmazonServiceException e)
		{
			TranscodeInstanceService.DEBUGGER.error("Exception getMessage: " + e.getMessage());
		}
		catch (Exception e)
		{
			TranscodeInstanceService.DEBUGGER.error("Exception getMessage: " + e.getMessage());
		}

		TranscodeInstanceService.DEBUGGER.debug("end to handle with transcode instances auto scaling up");
	}

	/**
	 * �����Զ�����
	 */
	public static void doAutoScaling()
	{
		// ��չ�Զ�������
		TranscodeInstanceService.doAutoScalingUp();

		// ��С�Զ�������
		TranscodeInstanceService.doAutoScalingDown();
	}

}
