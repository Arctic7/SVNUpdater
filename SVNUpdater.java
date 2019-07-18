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
	private static final String LOCAL_BASE_SRC = "E:/JAVA-WORKSPACE/CAE-update_only/BASE/src/";
	private static final String LOCAL_WEB_SRC = "E:/JAVA-WORKSPACE/CAE-update_only/CAE/src/";
	private static final String LOCAL_WEB_ROOT = "E:/JAVA-WORKSPACE/CAE-update_only/CAE/WebRoot/";
	private static final String LOCAL_UPDATE = "C:/Users/Arctic/Desktop/update/";
	private static final String REMOTE_UPDATE = "C:/Users/Webadmin0227/Desktop/update/";
	private static final String REMOTE_BACKUP = "D:/cae_bak/cae_project/cae2019-07-18/";
	private static final String REMOTE_TEST = "D:/MemberDBceshi/webapps/cae/";
	private static final String REMOTE_PRODUCTION = "D:/MemberDB/webapps/cae/";
	private static final String SVN_UPDATE = "U";
	private static final String SVN_ADD = "A";
	private static final String SVN_DELETE = "D";
	private static final String WRITE_MODE_LOCAL_PACK = "local";
	private static final String WRITE_MODE_BACKUP = "backup";
	private static final String WRITE_MODE_TEST_UPDATE = "test";
	private static final String WRITE_MODE_PRODUCTION_UPDATE = "production";
	
	/**
	 * entrance method
	 */
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
				List<String[]> addList = new ArrayList<String[]>();
				List<String[]> updateList = new ArrayList<String[]>();
				List<String[]> deleteList = new ArrayList<String[]>();
				// group by update type
				for (String[] ele : result) {
					String updateType = ele[0];
					if (updateType.equalsIgnoreCase(SVN_ADD)) {
						addList.add(ele);
					} else if (updateType.equalsIgnoreCase(SVN_UPDATE)) {
						updateList.add(ele);
					} else if (updateType.equalsIgnoreCase(SVN_DELETE)) {
						deleteList.add(ele);
					}
				}
				writeCmd(localUpdate, WRITE_MODE_LOCAL_PACK, addList, updateList, deleteList);
				writeCmd(remoteBackup, WRITE_MODE_BACKUP, addList, updateList, deleteList);
				writeCmd(remoteUpdate, WRITE_MODE_TEST_UPDATE, addList, updateList, deleteList);
				writeCmd(remoteProductionUpdate, WRITE_MODE_PRODUCTION_UPDATE, addList, updateList, deleteList);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * method for write cmd into file, writeMode must be local constant 
	 */
	private void writeCmd(File file,String writeMode,List<String[]> addList,List<String[]> updateList,List<String[]> deleteList) throws Exception{
		FileWriter writer = new FileWriter(file);
		StringBuilder sBuilder = new StringBuilder(80);
		switch (writeMode) {
		case WRITE_MODE_LOCAL_PACK:
			sBuilder.append(batchCopyCmd(LOCAL_WEB_ROOT, LOCAL_UPDATE, addList,LOCAL_UPDATE));
			sBuilder.append(batchCopyCmd(LOCAL_WEB_ROOT, LOCAL_UPDATE, updateList,LOCAL_UPDATE));
			break;
		case WRITE_MODE_BACKUP:
			sBuilder.append(batchCopyCmd(REMOTE_TEST, REMOTE_BACKUP, updateList,REMOTE_UPDATE));
			sBuilder.append(batchCopyCmd(REMOTE_TEST, REMOTE_BACKUP, deleteList,REMOTE_UPDATE));
			break;
		case WRITE_MODE_TEST_UPDATE:
			sBuilder.append(batchCopyCmd(REMOTE_UPDATE, REMOTE_TEST, addList,REMOTE_UPDATE));
			sBuilder.append(batchCopyCmd(REMOTE_UPDATE, REMOTE_TEST, updateList,REMOTE_UPDATE));
			sBuilder.append(batchDeleteCmd(REMOTE_TEST, deleteList,REMOTE_UPDATE));
			break;
		case WRITE_MODE_PRODUCTION_UPDATE:
			sBuilder.append(batchCopyCmd(REMOTE_UPDATE, REMOTE_PRODUCTION, addList,REMOTE_UPDATE));
			sBuilder.append(batchCopyCmd(REMOTE_UPDATE, REMOTE_PRODUCTION, updateList,REMOTE_UPDATE));
			sBuilder.append(batchDeleteCmd(REMOTE_PRODUCTION, deleteList,REMOTE_UPDATE));
			break;
		}
		writer.write(sBuilder.toString());
		writer.write("pause");
		writer.close();
	}

	/**
	 * advance method for create batch copy cmd
	 */
	private StringBuilder batchCopyCmd(String startPrefix, String targetPrefix, List<String[]> pathList, String logPathPrefix)
			throws Exception {
		StringBuilder sBuilder = new StringBuilder(80);
		for (String[] ele : pathList) {
			String cmd = getCopyCmd(ele[2],startPrefix, targetPrefix, ele[1],logPathPrefix);
			sBuilder.append(cmd);
		}
		return sBuilder;
	}

	/**
	 * advanced method for create delete cmd
	 */
	private StringBuilder batchDeleteCmd(String prefix, List<String[]> pathList, String logPathPrefix) throws Exception {
		StringBuilder sBuilder = new StringBuilder(80);
		for (String[] ele : pathList) {
			String cmd = getDeleteCmd(prefix, ele[1],logPathPrefix);
			sBuilder.append(cmd);
		}
		return sBuilder;
	}

	/**
	 * base method for create copy cmd and log
	 */
	private String getCopyCmd(String pathType, String startPathPrefix, String targetPathPrefix, String path,String logPathPrefix) throws Exception {
		String cmd = "ECHO "+pathType+"|XCOPY " + startPathPrefix + path + " " + targetPathPrefix + path;
		cmd = cmd.replaceAll("/", "\\\\");
		cmd = cmd.replace("|XCOPY ", "|XCOPY /y ");
		cmd = cmd+getLogCmd(logPathPrefix);
		cmd = cmd + "\r\n";
		return cmd;
	}

	/**
	 * base method for create delete cmd and log
	 */
	private String getDeleteCmd(String pathPrefix, String path,String logPathPrefix) throws Exception {
		String cmd = "rd/s/q " + pathPrefix + path;
		cmd = cmd.replaceAll("/", "\\\\");
		cmd = cmd+getLogCmd(logPathPrefix);
		cmd = cmd + "\r\n";
		return cmd;
	}
	/**
	 * base method for create log cmd
	 */
	private String getLogCmd(String logPathPrefix) throws Exception {
		StringBuilder sBuilder = new StringBuilder(80);
		sBuilder.append(" >> ");
		sBuilder.append(logPathPrefix);
		sBuilder.append("log.txt");
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
	 * replace various string and add file or directory flag
	 * string[0] is update type, string[1] is path, string[2] is path type, D(directory) or F(file)
	 */
	private List<String[]> processList(List<String[]> list) {
		List<String[]> processedList = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			String[] ele = list.get(i);
			String[] eleCopy = new String[3];
			if(ele.length<3) {
				for(int j=0;j<ele.length;j++) {
					eleCopy[j] = ele[j];
				}
				if(isFile(eleCopy[1])) {
					eleCopy[2] = "F";
				}else {
					eleCopy[2] = "D";
				}
				String path = eleCopy[1];
				path = path.replaceAll(".java", ".class");
				path = path.replaceAll(LOCAL_BASE_SRC, LOCAL_WEB_ROOT + "WEB-INF/classes/");
				path = path.replaceAll(LOCAL_WEB_SRC, LOCAL_WEB_ROOT + "WEB-INF/classes/");
				path = path.replaceAll(LOCAL_WEB_ROOT, "");
				eleCopy[1] = path;
				processedList.add(eleCopy);
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
	/**
	 * simple file check, if a directory has a name like 'com.aw.test.txt' then it will be treated as file not directory
	 * @param path file path
	 * @return if is a file return true, else return false
	 */
	private boolean isFile(String path) {
		boolean result = true;
		int index = path.lastIndexOf(".");
		if(index==-1) {
			result=false;
		}else if(index>-1) {
			String rest = path.substring(index, path.length());
			if(rest.contains("/")||rest.contains("\\")) {
				result=false;
			}
		}
		return result;
	}

	public static void main(String[] args) {
		String xlsxUpdateFile = "C:/Users/Arctic/Desktop/update/update.xlsx";
		SVNUpdater updater = new SVNUpdater();
		updater.generateCMD(xlsxUpdateFile);
	}
}