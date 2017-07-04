package upem.jarret.server.job;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */


public class Job{

	private	final	long jobId;
	private	final	int jobTaskNumber;
	private	final	String jobDescription;
	private	final	int jobPriority;
	private	final	String workerVersionNumber;
	private	final	String workerURL;
	private	final	String workerClassName;
	private final	BitSet tasksEnded;
	private final	Random rand = new Random();

	/************************************* PRIVATE *************************************/
	
	private Job(long jobId, int jobTaskNumber, String jobDescription, int jobPriority, String workerVersionNumber, String workerURL, String workerClassName) {

		this.jobId = jobId;
		this.jobTaskNumber = jobTaskNumber;
		this.jobDescription = Objects.requireNonNull(jobDescription);
		this.jobPriority = jobPriority;
		this.workerVersionNumber = Objects.requireNonNull(workerVersionNumber);
		this.workerURL = Objects.requireNonNull(workerURL);
		this.workerClassName = Objects.requireNonNull(workerClassName);	
		this.tasksEnded = new BitSet(jobTaskNumber);
	}

	/************************************* PUBLIC *************************************/

	
	/**
	 * Create a list contained priority-instance of the job describes in the String(Block JSON) passed in parameter
	 * @param String
	 * @return ArrayList<Job> with priority-instance of the Job
	 */
	public static ArrayList<Job> createJobFromJSONBlock(String blockJSON){

		try{
			JSONObject json = new JSONObject(blockJSON);
			int nb = json.getInt("JobPriority");
			if(nb <= 0 || json.getInt("JobTaskNumber") <= 0)
				return null;
			ArrayList<Job> jobs = new ArrayList<>(nb);
			Job job = new Job(
					json.getLong("JobId"),
					json.getInt("JobTaskNumber"),
					json.getString("JobDescription"),
					json.getInt("JobPriority"),
					json.getString("WorkerVersionNumber"),
					json.getString("WorkerURL"), 
					json.getString("WorkerClassName")
					);
			for(int i = nb - 1; i >= 0; i--)
				jobs.add(job);
			return jobs;
		} catch (JSONException j){ throw new JSONException("Bad creation of Job:" + blockJSON); }
	}

	/**
	 * Verify if Task is in the the Job
	 * @param task
	 * @return true if the Job contains the task, false otherwise
	 */
	public boolean containsTask(Task task){ return task.getTask() < jobTaskNumber && task.equals(new Task(jobId, task.getTask(), workerVersionNumber, workerURL, workerClassName)); }
	
	/**
	 * Get if all tasks in the job is ended
	 * @return true if all tasks is ended, false otherwise
	 */
	public boolean jobEnded(){ return tasksEnded.cardinality() == jobTaskNumber; }

	/*
	public boolean endTask(int task){ 

		if(task >= getJobTaskNumber() || tasksEnded.get(task))
			return false;
		tasksEnded.set(task); 
		return true;
	}*/

	/**
	 * Set the task passed in parameter to task ended
	 * @param task
	 * @return true if number of task is contained in the Job and if his not already ended, false otherwise
	 */
	public boolean endTask(Task task){ 
		
		if(!containsTask(task) || tasksEnded.get(task.getTask()))
			return false;
		tasksEnded.set(task.getTask()); 
		return true;
	}

	/**
	 * Retrieve one task in the job not ended
	 * @return Optional<Task> with a task not ended in the Job or Optional.empty if the job is ended(all the task in the Job is ended)
	 */
	public Optional<Task> getOneTask(){
		
		if(!jobEnded()){
			int task = rand.nextInt(jobTaskNumber);
			while(tasksEnded.get(task))
				task = rand.nextInt(jobTaskNumber);
			return Optional.ofNullable(new Task(jobId, task, workerVersionNumber, workerURL, workerClassName));
		} 
		return Optional.empty();
	}

	@Override
	public boolean equals(Object o){

		if(!(o instanceof Job))
			return false;
		return this.toString().equals(o.toString());	
	}

	@Override
	public String toString(){
		
		return "{"
				+"\"JobId\" : \""+jobId
				+"\", \"JobTaskNumber\" : \""+jobTaskNumber
				+"\", \"JobDescription\" : \""+jobDescription
				+"\", \"JobPriority\" : \""+jobPriority
				+"\", \"WorkerVersionNumber\" : \""+workerVersionNumber
				+"\", \"WorkerURL\" : \""+workerURL
				+"\", \"WorkerClassName\" : \""+workerClassName
				+ "\"}";

	}

	@Override
	public int hashCode(){ return (int) (jobId ^ jobTaskNumber ^ jobDescription.hashCode() ^ jobPriority ^ workerVersionNumber.hashCode() ^ workerURL.hashCode() ^ workerClassName.hashCode()); }

	/**
	 * Get the Job Id
	 * @return jobId
	 */
	public long getJobId() { return jobId; }

	/**
	 * Get the Number of tasks in the job
	 * @return jobtaskNumber
	 */
	public int getJobTaskNumber() { return jobTaskNumber; }

	/**
	 * Get the Description of the Job
	 * @return String contained the description of the job
	 */
	public String getJobDescription() { return jobDescription; }

	/**
	 * Get the priority of the job
	 * @return jobPriority
	 */
	public int getJobPriority() { return jobPriority; }

	/**
	 * Get the number of tasks ended in the Job
	 * @return numberOfTaskComplete
	 */
	public int numberOfTaskComplete() { return tasksEnded.cardinality(); }

	/**
	 * Get the worker version
	 * @return workerVersionNumber
	 */
	public String getWorkerVersionNumber() { return workerVersionNumber; }

	/**
	 * Get the worker URL
	 * @return workerURL
	 */
	public String getWorkerURL() { return workerURL; }

	/**
	 * Get worker class name
	 * @return workerClassName
	 */
	public String getWorkerClassName() { return workerClassName; }
	

}
