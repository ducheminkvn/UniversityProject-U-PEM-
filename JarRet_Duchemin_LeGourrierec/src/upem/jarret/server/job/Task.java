package upem.jarret.server.job;

import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class Task{
	
	private	final	long jobId;
	private	final	int task;
	private	final	String workerVersionNumber;
	private	final	String workerURL;
	private	final	String workerClassName;

	/**
	 * Create a Task from String in Block JSON Format
	 * @param blockJSON
	 * @return Task
	 * @throws JSONException - if the field in the block JSON is too few
	 */
	public static Task createTaskFromJSONBlock(String blockJSON){

		try{
			JSONObject json = new JSONObject(blockJSON);
			return new Task(
					json.getLong("JobId"),
					json.getInt("Task"),
					json.getString("WorkerVersion"),
					json.getString("WorkerURL"), 
					json.getString("WorkerClassName")
					);
		} catch (JSONException jex){ throw new JSONException("Bad creation of task:" + blockJSON); }
	}

	/**
	 * Create Task 
	 * @param jobId
	 * @param task
	 * @param workerVersionNumber
	 * @param workerURL
	 * @param workerClassName
	 */
	public Task(long jobId, int task, String workerVersionNumber, String workerURL, String workerClassName) {

		this.jobId = jobId;
		if(task < 0 ) throw new IllegalArgumentException(task + " must be positive");
		this.task = task;
		this.workerVersionNumber = Objects.requireNonNull(workerVersionNumber);
		this.workerURL = Objects.requireNonNull(workerURL);
		this.workerClassName = Objects.requireNonNull(workerClassName);	
	}

	@Override
	public boolean equals(Object o){

		if(!(o instanceof Task))
			return false;
		return this.toString().equals(o.toString());	
	}

	/**
	 * Representation of task in JSON format
	 * @return String with the task in JSON format
	 */
	public String jsonForPacket(){

		return "{"
				+"\"JobId\" : \""+jobId
				+"\", \"WorkerVersion\" : \""+workerVersionNumber
				+"\", \"WorkerURL\" : \""+workerURL
				+"\", \"WorkerClassName\" : \""+workerClassName
				+"\", \"Task\" : \""+task
				+ "\"}";
	}

	@Override
	public String toString(){
		
		return "{"
				+"\"JobId\" : \""+jobId
				+"\", \"Task\" : \""+task
				+"\", \"WorkerVersionNumber\" : \""+workerVersionNumber
				+"\", \"WorkerURL\" : \""+workerURL
				+"\", \"WorkerClassName\" : \""+workerClassName
				+ "\"}";
	}

	/**
	 * Get Job Id 
	 * @return jobId
	 */
	public long getJobId() { return jobId; }

	/**
	 * Get number task
	 * @return numberTask
	 */
	public int getTask() { return task; }

	/**
	 * Get worker version number
	 * @return workerVersionNumber
	 */
	public String getWorkerVersionNumber() { return workerVersionNumber; }

	/**
	 * Get worker URL
	 * @return workerURL
	 */
	public String getWorkerURL() { return workerURL; }

	/**
	 * Get worker class name
	 * @return workerClassname
	 */
	public String getWorkerClassName() { return workerClassName; }
}
