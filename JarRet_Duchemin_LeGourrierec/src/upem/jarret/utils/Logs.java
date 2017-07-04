package upem.jarret.utils;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class Logs {
	
	/**
	 * Write the message in the log File passed in parameters
	 * @param logPath
	 * @param fileName
	 * @param message
	 * @throws IOException
	 */
	public static void writeLog(String logPath, String fileName, String message) throws IOException{ FileUtils.openAndWriteFile(logPath, fileName, "\n" + LocalDateTime.now().toLocalTime() + " :\n" + message + "\n"); }
}
