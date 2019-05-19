package com.arcticworks.important;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * @author Arctic
 */
public class SVNUpdater {
	private static final String LOCAL_PROJECT_BASE_SRC = "E:/JAVA-WORKSPACE/CAE/CAE-Base/src/";
	private static final String LOCAL_PROJECT_WEB_SRC = "E:/JAVA-WORKSPACE/CAE/CAE-Web/src/";
	private static final String LOCAL_PROJECT_ROOT = "E:/JAVA-WORKSPACE/CAE-update_only/CAE/WebRoot/";
	private static final String LOCAL_UPDATE = "C:/Users/Arctic/Desktop/update/";
	private static final String REMOTE_UPDATE = "C:/Users/Webadmin0227/Desktop/update/";
	private static final String REMOTE_BACKUP = "D:/cae_bak/cae_project/cae2019-05-14~05-17/";
	private static final String REMOTE_PROJECT = "D:/MemberDBceshi/webapps/cae/";
	private static final String REMOTE_PROJECT_PRODUCTION = "D:/MemberDB/webapps/cae/";
	private static final String SVN_UPDATE = "U";
	private static final String SVN_ADD = "A";
	private static final String SVN_DELETE = "D";
	
	//entrance method
	public synchronized void generateCMD(String xlsxFilePath) {
		List<String[]> result = processList(getListFromXLSX(xlsxFilePath));
		File localUpdate = new File(LOCAL_UPDATE + "01-LOCAL_PACK.CMD");
		File remoteBackup = new File(LOCAL_UPDATE + "02-REMOTE_BACKUP.CMD");
		File remoteUpdate = new File(LOCAL_UPDATE + "03-REMOTE_UPDATE.CMD");
		File remoteProductionUpdate = new File(LOCAL_UPDATE + "04-REMOTE_PRODUCTION_UPDATE.CMD");
		File readMe = new File(LOCAL_UPDATE + "99-DONT_FORGET_TO_CHECK_CONFIG_FILE.TXT");
		if (result.size() > 0) {
			try {
				refreshFile(localUpdate);
				refreshFile(remoteBackup);
				refreshFile(remoteUpdate);
				refreshFile(remoteProductionUpdate);
				refreshFile(readMe);
				List<String> addList = new ArrayList<String>();
				List<String> updateList = new ArrayList<String>();
				List<String> deleteList = new ArrayList<String>();
				// group by svn type
				for (String[] ele : result) {
					String updateType = ele[0];
					String path = ele[1];
					if (updateType.equalsIgnoreCase(SVN_ADD)) {
						addList.add(path);
					} else if (updateType.equalsIgnoreCase(SVN_UPDATE)) {
						updateList.add(path);
					} else if (updateType.equalsIgnoreCase(SVN_DELETE)) {
						deleteList.add(path);
					}
				}
				writeLocalUpdateCmd(localUpdate, addList, updateList);
				writeRemoteBackupCmd(remoteBackup, updateList, deleteList);
				writeRemoteUpdateCmd(remoteUpdate, addList, updateList, deleteList);
				writeRemoteProductionUpdateCmd(remoteProductionUpdate, addList, updateList, deleteList);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * write cmd for local project to local update folder
	 */
	private void writeLocalUpdateCmd(File file, List<String> addList, List<String> updateList) throws Exception {
		FileWriter writer = new FileWriter(file);
		StringBuilder sBuilder = new StringBuilder(80);
		sBuilder.append(getUpdateOrBackUpCmd(LOCAL_PROJECT_ROOT, LOCAL_UPDATE, addList,LOCAL_UPDATE));
		sBuilder.append(getUpdateOrBackUpCmd(LOCAL_PROJECT_ROOT, LOCAL_UPDATE, updateList,LOCAL_UPDATE));
		writer.write(sBuilder.toString());
		writer.write("pause");
		writer.close();
	}

	/**
	 * write cmd for remote project to remote backup folder
	 */
	private void writeRemoteBackupCmd(File file, List<String> updateList, List<String> deleteList) throws Exception {
		FileWriter writer = new FileWriter(file);
		StringBuilder sBuilder = new StringBuilder(80);
		sBuilder.append(getUpdateOrBackUpCmd(REMOTE_PROJECT, REMOTE_BACKUP, updateList,REMOTE_UPDATE));
		sBuilder.append(getUpdateOrBackUpCmd(REMOTE_PROJECT, REMOTE_BACKUP, deleteList,REMOTE_UPDATE));
		writer.write(sBuilder.toString());
		writer.write("pause");
		writer.close();
	}

	/**
	 * write cmd for remote update folder to remote test
	 */
	private void writeRemoteUpdateCmd(File file, List<String> addList, List<String> updateList, List<String> deleteList)
			throws Exception {
		FileWriter writer = new FileWriter(file);
		StringBuilder sBuilder = new StringBuilder(80);
		// update test
		sBuilder.append(getUpdateOrBackUpCmd(REMOTE_UPDATE, REMOTE_PROJECT, addList,REMOTE_UPDATE));
		sBuilder.append(getUpdateOrBackUpCmd(REMOTE_UPDATE, REMOTE_PROJECT, updateList,REMOTE_UPDATE));
		sBuilder.append(getUpdateDeleteCmd(REMOTE_PROJECT, deleteList,REMOTE_UPDATE));
		writer.write(sBuilder.toString());
		writer.write("pause");
		writer.close();
	}

	/**
	 * write cmd for remote update folder to remote production
	 */
	private void writeRemoteProductionUpdateCmd(File file, List<String> addList, List<String> updateList,
			List<String> deleteList) throws Exception {
		FileWriter writer = new FileWriter(file);
		StringBuilder sBuilder = new StringBuilder(80);
		// update production
		sBuilder.append(getUpdateOrBackUpCmd(REMOTE_UPDATE, REMOTE_PROJECT_PRODUCTION, addList,REMOTE_UPDATE));
		sBuilder.append(getUpdateOrBackUpCmd(REMOTE_UPDATE, REMOTE_PROJECT_PRODUCTION, updateList,REMOTE_UPDATE));
		sBuilder.append(getUpdateDeleteCmd(REMOTE_PROJECT_PRODUCTION, deleteList,REMOTE_UPDATE));
		writer.write(sBuilder.toString());
		writer.write("pause");
		writer.close();
	}

	/**
	 * advance create string cmd for file copy
	 */
	private StringBuilder getUpdateOrBackUpCmd(String startPrefix, String targetPrefix, List<String> pathList, String logPath)
			throws Exception {
		StringBuilder sBuilder = new StringBuilder(80);
		for (String ele : pathList) {
			// local root to local update
			String cmd = getCopyCmd(startPrefix, targetPrefix, ele,logPath);
			sBuilder.append(cmd);
		}
		return sBuilder;
	}

	/**
	 * advance create string cmd for file delete
	 */
	private StringBuilder getUpdateDeleteCmd(String prefix, List<String> pathList, String logPath) throws Exception {
		StringBuilder sBuilder = new StringBuilder(80);
		for (String ele : pathList) {
			// local root to local update
			String cmd = getDeleteCmd(prefix, ele,logPath);
			sBuilder.append(cmd);
		}
		return sBuilder;
	}

	/**
	 * base create string cmd for file copy
	 */
	private String getCopyCmd(String startPathPrefix, String targetPathPrefix, String path,String logPath) throws Exception {
		String cmd = "ECHO F|XCOPY " + startPathPrefix + path + " " + targetPathPrefix + path;
		cmd = cmd.replaceAll("/", "\\\\");
		cmd = cmd.replace("ECHO F|XCOPY ", "ECHO F|XCOPY /y ");
		cmd = cmd+getLogCmd(logPath);
		cmd = cmd + "\r\n";
		return cmd;
	}

	/**
	 * base create string cmd for file delete
	 */
	private String getDeleteCmd(String pathPrefix, String path,String logPath) throws Exception {
		String cmd = "DEL " + pathPrefix + path;
		cmd = cmd.replaceAll("/", "\\\\");
		cmd = cmd+getLogCmd(logPath);
		cmd = cmd + "\r\n";
		return cmd;
	}
	
	private String getLogCmd(String logPath) throws Exception {
		StringBuilder sBuilder = new StringBuilder(80);
		sBuilder.append(" >> \"");
		sBuilder.append(logPath);
		sBuilder.append("log.txt");
		sBuilder.append("\"");
		return sBuilder.toString();
	}

	/**
	 * read xlsx and convert to list
	 */
	private List<String[]> getListFromXLSX(String xlsxFilePath) {
		List<String[]> result = new ArrayList<String[]>();
		if (xlsxFilePath.endsWith(".xlsx") || xlsxFilePath.endsWith(".XLSX")) {
			try {
				File xlsxFile = new File(xlsxFilePath);
				Workbook workbook = WorkbookFactory.create(xlsxFile);
				Sheet sheet = workbook.getSheetAt(0);
				for (Iterator<Row> i = sheet.iterator(); i.hasNext();) {
					Row row = i.next();
					String[] cellArray = new String[2];
					int counter = 0;
					for (Iterator<Cell> c = row.cellIterator(); c.hasNext();) {
						if (counter < 2) {
							Cell cell = c.next();
							int index = cell.getColumnIndex();
							cellArray[index] = cell.getStringCellValue();
							counter++;
						} else {
							break;
						}
					}
					result.add(cellArray);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * replace various string
	 */
	private List<String[]> processList(List<String[]> list) {
		List<String[]> processedList = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			String[] ele = list.get(i);
			String path = ele[1];
			int index = path.lastIndexOf(".");
			// skip folders
			if (index == -1 || index == path.length() - 1) {
				continue;
			} else {
				path = path.replaceAll(".java", ".class");
				path = path.replaceAll(LOCAL_PROJECT_BASE_SRC, LOCAL_PROJECT_ROOT + "WEB-INF/classes/");
				path = path.replaceAll(LOCAL_PROJECT_WEB_SRC, LOCAL_PROJECT_ROOT + "WEB-INF/classes/");
				path = path.replaceAll(LOCAL_PROJECT_ROOT, "");
				ele[1] = path;
				processedList.add(ele);
			}
		}
		return processedList;
	}

	/**
	 * create file if not exist
	 */
	private void refreshFile(File file) {
		try {
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SVNUpdater updater = new SVNUpdater();
		updater.generateCMD("C:/Users/Arctic/Desktop/update/update.xlsx");
	}
}