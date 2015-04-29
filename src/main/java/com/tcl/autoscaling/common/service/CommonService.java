package com.tcl.autoscaling.common.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcl.autoscaling.common.domain.CommandExecutedResult;

public class CommonService
{
	final static private Log DEBUGGER = LogFactory.getLog(CommonService.class);

	private static String outputMsg = "";
	private static String errorMsg = "";

	/**
	 * �������������ƻ�ȡ��Ӧ��ֵ
	 * 
	 * @param propertitesFile
	 *            property file
	 * @param confKey
	 *            ����������
	 * @return confValue ������ֵ
	 */
	public static String getBaseConfValue(String propertitesFile, String confKey)
	{
		String confValue = "";

		InputStream inputStream = new CommonService().getClass().getClassLoader().getResourceAsStream(propertitesFile);
		Properties p = new Properties();

		try
		{
			p.load(inputStream);
			confValue = p.getProperty(confKey);
		}
		catch (Exception e)
		{
			confValue = "";
		}
		finally
		{
			try
			{
				if (null != inputStream)
				{
					inputStream.close();
				}
			}
			catch (IOException e)
			{
				confValue = "";
			}
		}

		return confValue;
	}

	/**
	 * ��ͨJAVA��ȡ WEB��Ŀ�µ�WEB-INFĿ¼·��
	 * 
	 * @return WEB-INFĿ¼·��
	 */
	public static String getWebInfPath()
	{
		URL url = new CommonService().getClass().getProtectionDomain().getCodeSource().getLocation();
		String path = url.toString();
		int index = path.indexOf("WEB-INF");

		if (index == -1)
		{
			index = path.indexOf("classes");
		}

		if (index == -1)
		{
			index = path.indexOf("bin");
		}

		path = path.substring(0, index);

		if (path.startsWith("zip"))
		{
			// ��class�ļ���war��ʱ����ʱ����zip:D:/...������·��
			path = path.substring(4);
		}
		else if (path.startsWith("file"))
		{
			// ��class�ļ���class�ļ���ʱ����ʱ����file:/D:/...������·��
			path = path.substring(6);
		}
		else if (path.startsWith("jar"))
		{
			// ��class�ļ���jar�ļ�����ʱ����ʱ����jar:file:/D:/...������·��
			path = path.substring(10);
		}
		try
		{
			path = URLDecoder.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}

		if (1 == CommonService.getOperatingSystemType())
		{
			path = "/" + path;
		}
		return path;
	}

	/**
	 * �������������ƻ�ȡ��Ӧ��ֵ
	 * 
	 * @param confKey
	 *            ����������
	 * @return confValue ������ֵ
	 */
	public static String getAutoScalingConfValue(String confKey)
	{
		return CommonService.getBaseConfValue("autoscaling.propertites", confKey);
	}

	/**
	 * ִ�б���Windows����Linux�������ȡ����ֵ��ִ�н����Ϣ
	 * 
	 * @param cmd
	 *            ��ִ�е�����
	 * @return �����ֵ
	 */
	public static CommandExecutedResult execLocalCommand(String cmd)
	{
		CommandExecutedResult commandExecutedResult = new CommandExecutedResult();
		int exitValue = -1;
		String resultMessage = "";
		commandExecutedResult.setExitValue(exitValue);
		commandExecutedResult.setResultMessage(resultMessage);
		Process process = null;

		try
		{
			process = Runtime.getRuntime().exec(cmd);

			// ��ȡCPU�ĺ���������ǵ���CPU�Ļ����Ͳ��������߳�ȥ�������ʹ������ˣ�ֻ��
			if (1 == Runtime.getRuntime().availableProcessors())
			{
				InputStream stderrStream = process.getErrorStream();
				InputStream stdoutStream = process.getInputStream();
				InputStreamReader errStreamReader = new InputStreamReader(stderrStream);
				InputStreamReader outStreamReader = new InputStreamReader(stdoutStream);
				BufferedReader errBufReader = new BufferedReader(errStreamReader);
				BufferedReader outBufReader = new BufferedReader(outStreamReader);
				String errLine = null;
				String outLine = null;
				while (null != (errLine = errBufReader.readLine()) || null != (outLine = outBufReader.readLine()))
				{
					if (null != errLine && !"".equals(errLine))
					{
						CommonService.DEBUGGER.debug(errLine);
						CommonService.errorMsg += errLine;
					}
					if (null != outLine && !"".equals(outLine))
					{
						CommonService.DEBUGGER.debug(outLine);
						CommonService.outputMsg += outLine;
					}
				}
			}
			else
			{
				// ��ȡ���̵ı�׼������
				final InputStream inputStream = process.getInputStream();

				// ��ȡ���̵Ĵ�����
				final InputStream errorStream = process.getErrorStream();

				// �����������ؽ���
				Thread outputStreamThread = new Thread()
				{
					@Override
					public void run()
					{
						BufferedReader br1 = new BufferedReader(new InputStreamReader(inputStream));
						try
						{
							String line1 = null;
							CommonService.outputMsg = "";
							while ((line1 = br1.readLine()) != null)
							{
								if (line1 != null)
								{
									CommonService.outputMsg += line1;
									CommonService.DEBUGGER.debug(line1);
								}
							}
						}
						catch (IOException e)
						{
							CommonService.DEBUGGER.error("Exception: " + e.toString());
						}
						finally
						{
							try
							{
								inputStream.close();
							}
							catch (IOException e)
							{
								CommonService.DEBUGGER.error("Exception: " + e.toString());
							}
						}
					}
				};
				outputStreamThread.start();

				// ������������ؽ���
				Thread errorStreamThread = new Thread()
				{
					@Override
					public void run()
					{
						BufferedReader br2 = new BufferedReader(new InputStreamReader(errorStream));
						try
						{
							String line2 = null;
							CommonService.errorMsg = "";
							while ((line2 = br2.readLine()) != null)
							{
								if (line2 != null)
								{
									CommonService.errorMsg += line2;
									CommonService.DEBUGGER.debug(line2);
								}
							}
						}
						catch (IOException e)
						{
							CommonService.DEBUGGER.error("Exception: " + e.toString());
						}
						finally
						{
							try
							{
								errorStream.close();
							}
							catch (IOException e)
							{
								CommonService.DEBUGGER.error("Exception: " + e.toString());
							}
						}
					}
				};
				errorStreamThread.start();

				// �ȴ�������ʹ�������ؽ��̽���
				while (outputStreamThread.isAlive() || errorStreamThread.isAlive())
				{
					// ֻ�ǵȴ���������������
					;
				}
			}

			// ��ȡ����ֵ
			exitValue = process.waitFor();
			if (0 == exitValue)
			{
				resultMessage = CommonService.outputMsg;
			}
			else
			{
				resultMessage = CommonService.errorMsg;
			}
		}
		catch (Exception e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}
		finally
		{
			if (null != process)
			{
				process.destroy();
			}
		}

		commandExecutedResult.setExitValue(exitValue);
		commandExecutedResult.setResultMessage(resultMessage);
		return commandExecutedResult;
	}

	/**
	 * ��ȡ����ϵͳ����
	 * 
	 * @return ����ϵͳ����: 0��ʾwindows, 1��ʾLinux
	 */
	public static int getOperatingSystemType()
	{
		int osType = 0;
		String osName = System.getProperty("os.name");
		if (osName.toLowerCase().indexOf("windows") < 0)
		{
			osType = 1;
		}

		return osType;
	}

	/**
	 * �ж��ַ����Ƿ�Ϊ��
	 * 
	 * @param str
	 *            �ַ���
	 * @return true��ʾΪ�գ�false��ʾ�ǿ�
	 */
	public static boolean isStringNull(String str)
	{
		boolean result = false;

		if (null == str || "".equals(str))
		{
			return true;
		}

		return result;
	}

	/**
	 * �ж��ַ����Ƿ�Ϊ��
	 * 
	 * @param str
	 *            �ַ���
	 * @return true��ʾ��Ϊ�գ�false��ʾΪ��
	 */
	public static boolean isStringNotNull(String str)
	{
		return !CommonService.isStringNull(str);
	}

	/**
	 * �ж��ļ��Ƿ����
	 * 
	 * @param filePath
	 *            �ļ�·��
	 * @return true��ʾ���ڣ�false��ʾ������
	 */
	public static boolean isFileExisted(String filePath)
	{
		File file = new File(filePath);
		if (!file.exists())
		{
			return false;
		}

		return true;
	}

	/**
	 * �ж�Ŀ¼�Ƿ��д
	 * 
	 * @param Ŀ¼
	 * @return true��ʾ��д��false��ʾ����д
	 */
	public static boolean isDirWritable(String dir)
	{
		File file = new File(dir);
		if (!file.exists())
		{
			CommonService.DEBUGGER.error(dir + " not found");
			return false;
		}

		if (!file.isDirectory())
		{
			CommonService.DEBUGGER.error(dir + " not a directory");
			return false;
		}

		if (!file.canWrite())
		{
			CommonService.DEBUGGER.error(dir + " cat not write");
			return false;
		}

		return true;
	}

	/**
	 * ɾ���ļ�
	 * 
	 * @param filePath
	 * @return true��ʾɾ���ɹ���false��ʾɾ��ʧ��
	 */
	public static boolean deleteFile(String filePath)
	{
		try
		{
			File file = new File(filePath);
			if (!file.exists())
			{
				return true;
			}

			return file.delete();
		}
		catch (Exception e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}

		return false;
	}

	/**
	 * ��Ŀ¼�ĺ�����ϱ�Ҫ��б��
	 * 
	 * @param dirPath
	 *            Ŀ¼·��
	 * @return ����б�ܵ�Ŀ¼·��
	 */
	public static String addSlashToDirPathIfNecessary(String dirPath)
	{
		if (CommonService.isStringNull(dirPath))
		{
			return dirPath;
		}

		String finalDirPath = dirPath;
		if (!dirPath.endsWith("/"))
		{
			finalDirPath = dirPath + "/";
		}

		return finalDirPath;
	}

	/**
	 * д���ļ�
	 * 
	 * @param filePath
	 *            �ļ�·��
	 * @param fileContent
	 *            �ļ�����
	 * @return true��ʾд��ɹ���false��ʾʧ��
	 */
	public static boolean writeFile(String filePath, String fileContent)
	{

		FileWriter fw = null;
		try
		{
			fw = new FileWriter(filePath, true);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(fileContent);
			pw.close();
			fw.close();
			return true;
		}
		catch (IOException e)
		{
			CommonService.DEBUGGER.error("Exception : " + e.toString());
		}

		return false;
	}

	/**
	 * ��ȡ�ļ�����
	 * 
	 * @param filePath
	 *            �ļ�·��
	 * @return �ļ�����
	 */
	public static String getFileFormat(String filePath)
	{
		String fileFormat = "";

		// �����ǿռ��
		if (CommonService.isStringNull(filePath))
		{
			CommonService.DEBUGGER.error("filePath is null");
			return fileFormat;
		}

		// ����ļ��Ƿ�������
		if (filePath.indexOf(".") < 0)
		{
			CommonService.DEBUGGER.error("failed to get format from filePath");
			return fileFormat;
		}

		fileFormat = filePath.substring(filePath.lastIndexOf(".") + 1);

		return fileFormat;
	}

	/**
	 * �������ļ����������µ��ļ�·��
	 * 
	 * @param filePath
	 *            �ļ�·�������磺/tmp/test.mp4
	 * @param newFormat
	 *            �����ͣ����磺flv
	 * @return ���ļ�·�������磺/tmp/test.flv
	 */
	public static String genFilePathWithNewFormat(String filePath, String newFormat)
	{
		String newFilePath = "";

		// �����ǿռ��
		if (CommonService.isStringNull(filePath))
		{
			CommonService.DEBUGGER.error("filePath is null");
			return newFilePath;
		}

		// �����ǿռ��
		if (CommonService.isStringNull(filePath))
		{
			CommonService.DEBUGGER.error("filePath is null");
			newFilePath = filePath;
			return newFilePath;
		}

		// ����ļ��Ƿ�������
		if (filePath.indexOf(".") < 0)
		{
			newFilePath = filePath + "." + newFormat;
			return newFilePath;
		}

		String filePathWithoutFormat = filePath.substring(0, filePath.lastIndexOf(".") + 1);
		newFilePath = filePathWithoutFormat + newFormat;

		return newFilePath;
	}

	/**
	 * ɾ��Ŀ¼
	 * 
	 * @param dirPath
	 *            Ŀ¼·��
	 */
	public static void deleteFolder(String dirPath)
	{
		if (CommonService.isStringNull(dirPath))
		{
			return;
		}

		try
		{
			File folder = new File(dirPath);
			File[] files = folder.listFiles();
			if (null != files)
			{
				for (File f : files)
				{
					if (f.isDirectory())
					{
						CommonService.deleteFolder(f.getAbsolutePath());
					}
					else
					{
						f.delete();
					}
				}
			}
			folder.delete();
		}
		catch (Exception e)
		{
			CommonService.DEBUGGER.error("Exception: " + e.toString());
		}
	}

	/**
	 * ����������
	 * 
	 * @param seconds
	 *            ����
	 */
	public static void doSleep(long seconds)
	{
		try
		{
			Thread.sleep(seconds * 1000);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
	}
}
