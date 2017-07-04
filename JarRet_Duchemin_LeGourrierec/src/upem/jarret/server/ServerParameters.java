package upem.jarret.server;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class ServerParameters {

	private final int port;
	private final String logPath;
	private final String defaultInputPath;
	private final String filesAnswerPath;
	private final int maxSizeFileAnswer;
	private final int comeBackTime;
	
	/************************************* PRIVATE *************************************/

	private ServerParameters(int port, String logPath, String defaultInputPath, String filesAnswerPath, int maxSizeFileAnswer, int comeBackTime){
		
		this.port = port;
		this.logPath = logPath;
		this.defaultInputPath = defaultInputPath;
		this.filesAnswerPath = filesAnswerPath;
		this.maxSizeFileAnswer = maxSizeFileAnswer;
		this.comeBackTime = comeBackTime;
	}
	
	/************************************* PUBLIC *************************************/
	
	/**
	 * Create ServerParameters Object from a String in JSON format
	 * @param jsonFormat
	 * @return ServerParameters
	 * @throws JSONException
	 */
	public static ServerParameters createFromJsonFile(String jsonFormat) throws JSONException{

		JSONObject jsonO = new JSONObject(jsonFormat);
	
		int port = jsonO.getInt("Port");
		String logPath = jsonO.getString("LogsPath");
		String defaultInputPath = jsonO.getString("DefaultInputPath");
		String filesAnswerPath = jsonO.getString("FilesAnswerPath");
		int maxSizeFileAswer = jsonO.getInt("MaxSizeFileAnswer");
		int comeBackTime = jsonO.getInt("ComeBackTime");
		return new ServerParameters(port,logPath,defaultInputPath,filesAnswerPath,maxSizeFileAswer,comeBackTime);
	}

	/**
	 * Get the default input path in String format contend the files to load by default for the server
	 * @return A String represents the path for input files by default
	 */
	public String getDefaultInputPath() { return defaultInputPath; }

	/**
	 * Get the port for the server
	 * @return port
	 */
	public int getPort() { return port; }

	/**
	 * Get the path in String format contained the futur log for the server
	 * @return A String represents the the path for the log's files
	 */
	public String getLogPath() { return logPath; }

	/**
	 * Get the path in String format contained the futur answer received by the server
	 * @return A String represents the the path for the answer's files
	 */
	public String getFilesAnswerPath() { return filesAnswerPath; }

	/**
	 * Get size max by answer's file
	 * @return maxSizeFileAnswer for the answer's files
	 */
	public int getMaxSizeFileAnswer() { return maxSizeFileAnswer; }

	/**
	 * Get Come Back Time in seconds
	 * @return comeBackTime - represent by a int in seconds
	 */
	public int getComeBackTime() { return comeBackTime; }
}
