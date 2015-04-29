package com.tcl.autoscaling.awss3.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tcl.autoscaling.common.service.CommonService;

public class AwsS3Service
{
	final static private Log DEBUGGER = LogFactory.getLog(AwsS3Service.class);

	/**
	 * �� ��/Ͱ��/����·������ �����·���н�����Ͱ��
	 * 
	 * @param s3FilePath
	 *            ��/Ͱ��/����·������ �����·��
	 * @return Ͱ��
	 */
	public static String parseBucketName(String s3FilePath)
	{
		if (CommonService.isStringNull(s3FilePath))
		{
			return null;
		}

		String tmpS3FilePath = s3FilePath;
		if (s3FilePath.indexOf("/") == 0)
		{
			tmpS3FilePath = s3FilePath.substring(s3FilePath.indexOf("/") + 1);
		}
		String bucketName = tmpS3FilePath.substring(0, tmpS3FilePath.indexOf("/"));
		return bucketName;
	}

	/**
	 * �� ��/Ͱ��/����·������ �����·���н���������·����
	 * 
	 * @param s3FilePath
	 *            ��/Ͱ��/����·������ �����·��
	 * @return ����·����
	 */
	public static String parseObjectPath(String s3FilePath)
	{

		if (CommonService.isStringNull(s3FilePath))
		{
			return null;
		}

		String tmpS3FilePath = s3FilePath;
		if (s3FilePath.indexOf("/") == 0)
		{
			tmpS3FilePath = s3FilePath.substring(s3FilePath.indexOf("/") + 1);
		}

		String key = tmpS3FilePath.substring(tmpS3FilePath.indexOf("/") + 1);
		return key;
	}

	/**
	 * ����Ԥǩ����S3 URL������ʱ���������ļ�ָ��
	 * 
	 * @param bucketName
	 *            Ͱ��
	 * @param key
	 *            ����·����
	 * @return ��ǩ����S3ȫ·��URL��HTTPЭ�飩��
	 *         ���磺http://rideo.s3.amazonaws.com/video/test/test.mp4?
	 *         Expires=1380345878&AWSAccessKeyId=AKIAI57VEFB4FVT7TQNA
	 *         &Signature=8kOgGMkSkynLMzEySiUB6506lGc%3D
	 */
	private static String genPresignedUrl(String bucketName, String key)
	{
		// Ĭ�Ϲ���ʱ��Ϊ24Сʱ
		long s3PresignedUrlExpirationSeconds = 60 * 60 * 24;
		String s3PresignedUrlExpirationSecondsStr = CommonService
		        .getAutoScalingConfValue("s3PresignedUrlExpirationSeconds");
		if (CommonService.isStringNotNull(s3PresignedUrlExpirationSecondsStr))
		{
			s3PresignedUrlExpirationSeconds = Long.valueOf(s3PresignedUrlExpirationSecondsStr);
		}
		Date expiration = new Date(new Date().getTime() + 1000 * s3PresignedUrlExpirationSeconds);

		while (key.startsWith("/"))
		{
			key = key.replaceFirst("/", "");
		}

		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		URL newUrl = s3.generatePresignedUrl(bucketName, key, expiration);
		if (null == newUrl || CommonService.isStringNull(newUrl.toString()))
		{
			return null;
		}

		// ���ɵ���httpsЭ���URL��ת��ΪhttpЭ��
		String s3FullPath = newUrl.toString();
		if (s3FullPath.startsWith("https"))
		{
			s3FullPath = s3FullPath.replaceFirst("https", "http");
		}

		return s3FullPath;
	}

	/**
	 * ��ȡS3�ӿڵ��ÿͻ���
	 * 
	 * @return S3�ӿڵ��ÿͻ���
	 */
	public static AmazonS3 getAmazonS3Client()
	{
		if (null == AwsS3Client.getInstance() || null == AwsS3Client.getInstance().getAmazonS3())
		{
			String awsAccessKeyId = CommonService.getAutoScalingConfValue("awsAccessKeyId");
			String awsSecretAccessKey = CommonService.getAutoScalingConfValue("awsSecretAccessKey");
			BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
			return new AmazonS3Client(credentials);
		}

		return AwsS3Client.getInstance().getAmazonS3();
	}

	/**
	 * �����ϴ�֮����ļ������û����ɶ�
	 * 
	 * @param s3FilePath
	 *            S3�е��ļ�·�������磺/rideo/video/test.mp4
	 */
	public static void setObjectPublicRead(String s3FilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to setObjectPublicRead " + s3FilePath);
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		String bucketName = AwsS3Service.parseBucketName(s3FilePath);
		String key = AwsS3Service.parseObjectPath(s3FilePath);

		try
		{
			s3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
			AwsS3Service.DEBUGGER.info("successfully set " + s3FilePath + " to public read");
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		AwsS3Service.DEBUGGER.debug("end to setObjectPublicRead " + s3FilePath);
	}

	/**
	 * ����S3�е��ļ�
	 * 
	 * @param srcFilePath
	 *            S3Դ�ļ�·�������磺/src/tmp/test_clip.mp4
	 * @param descFilePath
	 *            S3Ŀ���ļ�·�������磺/rideo/video/test_clip.mp4
	 * @return �������
	 */
	public static CopyObjectResult copyFile(String srcFilePath, String descFilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to copy file " + srcFilePath + " to " + descFilePath);
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		CopyObjectResult copyObjectResult = null;

		// �������
		if (CommonService.isStringNull(srcFilePath) || CommonService.isStringNull(descFilePath))
		{
			AwsS3Service.DEBUGGER.error("input parameter is null");
			AwsS3Service.DEBUGGER.debug("end to copy file");
			return copyObjectResult;
		}

		try
		{
			String srcBucketName = AwsS3Service.parseBucketName(srcFilePath);
			String srcKey = AwsS3Service.parseObjectPath(srcFilePath);
			String descBucketName = AwsS3Service.parseBucketName(descFilePath);
			String descKey = AwsS3Service.parseObjectPath(descFilePath);
			copyObjectResult = s3.copyObject(srcBucketName, srcKey, descBucketName, descKey);
			if (null != copyObjectResult)
			{
				AwsS3Service.DEBUGGER.info("successfully copy " + srcFilePath + " to " + descFilePath);
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Failed to copy file. Exception: " + e.toString());
			AwsS3Service.DEBUGGER.debug("end to copy file");
			return copyObjectResult;
		}

		AwsS3Service.DEBUGGER.debug("end to copy file");
		return copyObjectResult;
	}

	/**
	 * �ϴ��ļ���S3
	 * 
	 * @param localFilePath
	 *            �����ļ�·�������磺/tmp/test_clip.mp4
	 * @param s3FilePath
	 *            S3�ļ�·�������磺/rideo/video/test_clip.mp4
	 * @return �ϴ����
	 */
	public static PutObjectResult uploadFile(String localFilePath, String s3FilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to upload file " + localFilePath + " to " + s3FilePath);
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		PutObjectResult putObjectResult = null;
		String bucketName = AwsS3Service.parseBucketName(s3FilePath);
		String key = AwsS3Service.parseObjectPath(s3FilePath);
		try
		{
			putObjectResult = s3.putObject(bucketName, key, new File(localFilePath));
			if (null != putObjectResult)
			{
				AwsS3Service.DEBUGGER.info("successfully upload " + localFilePath + " to " + s3FilePath);
			}
		}
		catch (AmazonServiceException e)
		{
			AwsS3Service.DEBUGGER.error("Failed to upload file. Exception: " + e.toString());
		}
		catch (AmazonClientException e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		AwsS3Service.DEBUGGER.debug("end to upload file");
		return putObjectResult;
	}

	/**
	 * ��S3�д���һ�����ļ��У�������һ����/��β�Ŀն���
	 * 
	 * @param s3DirPath
	 *            S3·��
	 * 
	 * @return ������
	 */
	public static PutObjectResult createS3EmptyDir(String s3DirPath)
	{
		AwsS3Service.DEBUGGER.debug("begin to create empty folder  " + s3DirPath);

		if (CommonService.isStringNull(s3DirPath))
		{
			AwsS3Service.DEBUGGER.error("s3DirPath is null");
			AwsS3Service.DEBUGGER.debug("end to create empty folder  " + s3DirPath);
			return null;
		}

		if (!s3DirPath.endsWith("/"))
		{
			s3DirPath += "/";
		}

		PutObjectResult result = null;
		try
		{
			InputStream input = new ByteArrayInputStream("".getBytes("UTF-8"));
			result = AwsS3Service.getAmazonS3Client().putObject(AwsS3Service.parseBucketName(s3DirPath),
			        AwsS3Service.parseObjectPath(s3DirPath), input, null);
			if (null != result)
			{
				AwsS3Service.DEBUGGER.debug("successfully create empty folder  " + s3DirPath);
			}
			else
			{
				AwsS3Service.DEBUGGER.debug("failed to create empty folder  " + s3DirPath);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
			AwsS3Service.DEBUGGER.debug("end to create empty folder  " + s3DirPath);
			return null;
		}

		AwsS3Service.DEBUGGER.debug("end to create empty folder  " + s3DirPath);
		return result;
	}

	// �Ѿ��ϴ����ļ��б�
	private static List<String> uploadedFilesList = new ArrayList<String>();

	/**
	 * ɾ���Ѿ��ϴ����ļ�
	 */
	private static void deleteUploadedFiles()
	{
		// ��ʱ�����ļ��ϴ�ʧ�ܣ��������ļ����ϴ���Ϊʧ�ܣ�������ǰ�ϴ��ɹ����ļ�ɾ��
		if (null != AwsS3Service.uploadedFilesList && !AwsS3Service.uploadedFilesList.isEmpty())
		{
			for (int j = 0; j < AwsS3Service.uploadedFilesList.size(); j++)
			{
				AwsS3Service.deleteObject(AwsS3Service.uploadedFilesList.get(j));
			}
		}
	}

	/**
	 * 
	 * �ϴ��ļ��е�S3
	 * 
	 * @param localDirPath
	 *            �ļ���·��
	 * @param originalLocalDirPath
	 *            ԭʼ���ļ���·��
	 * @param s3DirPath
	 *            ��S3�е��ļ���·��
	 * @return true��ʾ�ϴ��ɹ���false��ʾ�ϴ�ʧ��
	 */
	public static boolean uploadDir(String localDirPath, String originalLocalDirPath, String s3DirPath)
	{
		if (CommonService.isStringNull(localDirPath) || CommonService.isStringNull(originalLocalDirPath)
		        || CommonService.isStringNull(s3DirPath))
		{
			AwsS3Service.DEBUGGER.debug("localDirPath=" + localDirPath);
			AwsS3Service.DEBUGGER.debug("originalLocalDirPath=" + originalLocalDirPath);
			AwsS3Service.DEBUGGER.debug("s3DirPath=" + s3DirPath);
			AwsS3Service.DEBUGGER.error("parameter is null");
			return false;
		}

		if (!originalLocalDirPath.endsWith("/"))
		{
			originalLocalDirPath += "/";
		}

		if (!s3DirPath.endsWith("/"))
		{
			s3DirPath += "/";
		}

		localDirPath = localDirPath.replaceAll("\\\\", "/");
		File dir = new File(localDirPath);
		File[] files = dir.listFiles();

		if (null == files)
		{
			AwsS3Service.DEBUGGER.info("It is empty in " + localDirPath + ", no need to upload");
			return true;
		}

		for (int i = 0; i < files.length; i++)
		{
			String localFilePath = files[i].getAbsolutePath();
			String s3FilePath = localFilePath.replaceAll("\\\\", "/");
			s3FilePath = s3FilePath.replaceFirst(originalLocalDirPath, s3DirPath);

			// �ݹ��ϴ����ļ���
			if (files[i].isDirectory())
			{
				AwsS3Service.createS3EmptyDir(s3FilePath);
				AwsS3Service.uploadDir(files[i].getAbsolutePath(), originalLocalDirPath, s3DirPath);
			}
			else
			{
				// �ϴ��ļ�
				PutObjectResult uploadResult = AwsS3Service.uploadFile(localFilePath, s3FilePath);
				if (null == uploadResult)
				{
					AwsS3Service.deleteUploadedFiles();
					return false;
				}
			}

			AwsS3Service.uploadedFilesList.add(s3FilePath);
		}

		return true;
	}

	/**
	 * ��S3�����ļ�������
	 * 
	 * @param localFilePath
	 *            �����ļ�·�������磺/tmp/test_clip.mp4
	 * @param s3FilePath
	 *            S3�ļ�·�������磺/rideo/video/test_clip.mp4
	 * @return S3����
	 */
	public static ObjectMetadata downloadFile(String localFilePath, String s3FilePath)
	{
		AwsS3Service.DEBUGGER.debug("begin to download file " + s3FilePath + " to " + localFilePath);

		ObjectMetadata objectMetadata = null;
		AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
		String bucketName = AwsS3Service.parseBucketName(s3FilePath);
		String key = AwsS3Service.parseObjectPath(s3FilePath);
		try
		{
			GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
			objectMetadata = s3.getObject(getObjectRequest, new File(localFilePath));
			if (null != objectMetadata)
			{
				AwsS3Service.DEBUGGER.info("successfully download " + s3FilePath + " to " + localFilePath);
			}
		}
		catch (AmazonServiceException e)
		{
			AwsS3Service.DEBUGGER.error("Failed to download file. Exception: " + e.toString());
		}
		catch (AmazonClientException e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		AwsS3Service.DEBUGGER.debug("end to download file");
		return objectMetadata;
	}

	/**
	 * ��S3�ϵ�Ŀ¼�µ��ļ�����Ŀ¼ȫ�����ص�����
	 * 
	 * @param localDirPath
	 *            �����ļ���
	 * @param s3DirPath
	 *            S3�ϵ�Ŀ¼
	 */
	public static void downloadDir(String localDirPath, String s3DirPath)
	{
		if (CommonService.isStringNull(localDirPath) || CommonService.isStringNull(s3DirPath))
		{
			AwsS3Service.DEBUGGER.debug("localDirPath=" + localDirPath);
			AwsS3Service.DEBUGGER.debug("s3DirPath=" + s3DirPath);
			AwsS3Service.DEBUGGER.error("parameter is null");
			return;
		}

		if (!localDirPath.endsWith("/"))
		{
			localDirPath += "/";
		}

		if (!s3DirPath.endsWith("/"))
		{
			s3DirPath += "/";
		}

		AmazonS3 s3Client = AwsS3Service.getAmazonS3Client();
		String bucketName = AwsS3Service.parseBucketName(s3DirPath);
		String keyDir = AwsS3Service.parseObjectPath(s3DirPath);
		ObjectListing objectList = s3Client.listObjects(bucketName, keyDir);
		if (null == objectList)
		{
			return;
		}

		List<S3ObjectSummary> s3ObjectSummaryList = objectList.getObjectSummaries();
		for (int i = 0; i < s3ObjectSummaryList.size(); i++)
		{
			String key = s3ObjectSummaryList.get(i).getKey();
			String localFilePath = localDirPath + key.replaceFirst(keyDir, "");
			String s3FilePath = "/" + bucketName + "/" + key;
			if (CommonService.isStringNull(key))
			{
				continue;
			}

			if (key.endsWith("/"))
			{
				// S3������/��β����ʾ��һ���ļ��У��ڱ��ش������ļ��У�����������ļ��в����ڵĻ���һ��������
				File localFile = new File(localFilePath);
				localFile.mkdirs();
			}
			else
			{
				// �����ļ�
				AwsS3Service.downloadFile(localFilePath, s3FilePath);
			}
		}
	}

	/**
	 * ���S3�����Ƿ����
	 * 
	 * @param �������·��
	 * @return true��ʾ�Ǵ��ڵģ�false��ʾ�ǲ����ڵ�
	 */
	public static boolean isObjectExist(String objectPath)
	{
		try
		{
			int secondSlash = objectPath.indexOf('/', 1);
			String bucketName = objectPath.substring(1, secondSlash);
			String key = objectPath.substring(secondSlash + 1);
			while (key.startsWith("/"))
			{
				key = key.replaceFirst("/", "");
			}

			// ��ȡS3�����Ԫ����
			ObjectMetadata objectMetadata = AwsS3Service.getAmazonS3Client().getObjectMetadata(bucketName, key);
			if (null != objectMetadata)
			{
				return true;
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
			return false;
		}

		return false;
	}

	/**
	 * ���S3�����Ƿ��������˿ɶ���
	 * 
	 * @param bucketName
	 *            Ͱ��
	 * @param key
	 *            ����·��
	 * @return true��ʾ�������˿ɶ��ģ�false��ʾ���������˿ɶ���
	 */
	public static boolean isObjectPublicRead(String bucketName, String key)
	{
		try
		{
			AmazonS3 s3 = AwsS3Service.getAmazonS3Client();
			while (key.startsWith("/"))
			{
				key = key.replaceFirst("/", "");
			}

			// ��ȡS3�����ACL
			AccessControlList s3ObjectAcl = s3.getObjectAcl(bucketName, key);
			if (null == s3ObjectAcl || null == s3ObjectAcl.getGrants() || s3ObjectAcl.getGrants().isEmpty())
			{
				return false;
			}

			// ����ACL������Ƿ���������û��ɶ���Ȩ��
			Set<Grant> grantsSet = s3ObjectAcl.getGrants();
			Iterator<Grant> grantsIterator = grantsSet.iterator();
			Grant grant = null;
			while (grantsIterator.hasNext())
			{
				grant = grantsIterator.next();

				// �ҵ��������û��Ŀɶ�Ȩ��
				if (grant.getGrantee().getIdentifier().endsWith("AllUsers")
				        && Permission.Read.getHeaderName().equals(grant.getPermission().getHeaderName()))
				{
					return true;
				}
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}

		return false;
	}

	/**
	 * ��Ͱ��+�����������·����ȡS3��ȫ·��
	 * 
	 * @param inputPath
	 *            ���·������ʽΪ��/Ͱ��/�����������磺/rideo/video/test/test.mp4
	 * @return �ܹ����ʵ�S3ȫ·�������磺http://rideo.s3.amazonaws.com/video/test/test.mp4
	 *         ���ߴ�ǩ���͹���ʱ��ĵ�ַhttp://
	 *         rideo.s3.amazonaws.com/video/test/test.mp4?Expires=1380345878
	 *         &AWSAccessKeyId
	 *         =AKIAI57VEFB4FVT7TQNA&Signature=8kOgGMkSkynLMzEySiUB6506lGc%3D
	 */
	public static String genS3InputPath(String inputPath)
	{
		if (CommonService.isStringNull(inputPath))
		{
			return null;
		}

		// ����Ͱ��
		String bucketName = AwsS3Service.parseBucketName(inputPath);
		if (CommonService.isStringNull(bucketName))
		{
			return null;
		}

		// ��������·��
		String key = AwsS3Service.parseObjectPath(inputPath);
		if (CommonService.isStringNull(bucketName))
		{
			return null;
		}

		// ƴ��S3��HOST��URI���õ����ַ���Ϊ:
		// http://rideo.s3.amazonaws.com/video/test/test.mp4
		String s3InputPath = "http://" + bucketName + ".s3.amazonaws.com";
		if (!key.startsWith("/"))
		{
			key = "/" + key;
		}
		s3InputPath += key;

		// ���ö����Ƿ��������˿ɶ���Ȩ�ޣ����û�еĻ���Ҫ����ǩ��
		if (!AwsS3Service.isObjectPublicRead(bucketName, key))
		{
			s3InputPath = AwsS3Service.genPresignedUrl(bucketName, key);
		}

		return s3InputPath;
	}

	/**
	 * ɾ��S3��һ������
	 * 
	 * @param s3FilePath
	 *            S3�����ļ�·��
	 */
	public static void deleteObject(String s3FilePath)
	{
		try
		{
			if (CommonService.isStringNull(s3FilePath))
			{
				return;
			}
			AmazonS3 s3Client = AwsS3Service.getAmazonS3Client();
			String bucketName = AwsS3Service.parseBucketName(s3FilePath);
			String key = AwsS3Service.parseObjectPath(s3FilePath);
			s3Client.deleteObject(bucketName, key);
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}
	}

	/**
	 * ɾ��S3��һ����/��β�Ķ����Լ�����������ļ�
	 * 
	 * @param s3DirPath
	 *            S3�ļ���·������/��β�Ķ���
	 */
	public static void deleteFolderAndSubfiles(String s3DirPath)
	{
		try
		{
			if (CommonService.isStringNull(s3DirPath))
			{
				return;
			}
			AmazonS3 s3Client = AwsS3Service.getAmazonS3Client();
			String bucketName = AwsS3Service.parseBucketName(s3DirPath);
			String key = AwsS3Service.parseObjectPath(s3DirPath);

			ObjectListing list = s3Client.listObjects(bucketName, key);
			List<S3ObjectSummary> osList = list.getObjectSummaries();
			for (int i = 0; i < osList.size(); i++)
			{
				String s3ObjectPath = "/" + osList.get(i).getBucketName() + "/" + osList.get(i).getKey();
				AwsS3Service.DEBUGGER.debug("deleting " + s3ObjectPath);
				s3Client.deleteObject(osList.get(i).getBucketName(), osList.get(i).getKey());
			}
		}
		catch (Exception e)
		{
			AwsS3Service.DEBUGGER.error("Exception: " + e.toString());
		}
	}
}
