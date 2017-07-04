package upem.jarret.server.job;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

import upem.jarret.utils.CodeProtocol.AnswerKind;
import upem.jarret.utils.ConvertTimeUnit;
import upem.jarret.utils.FileUtils;
import upem.jarret.utils.Logs;
import upem.jarret.utils.Parse;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class ManageTask {
	private 			    boolean isVerbose = false;
	private 		final	Random rand = new Random();
	private 		final	ArrayList<Job> listInfoJob;
	private 		final	HashSet<Job> listJobEnded;
	private			final 	HashSet<Path> listFilesPath;
	private 		final 	HashSet<Path> listDirPath;
	private	static			boolean toClose = false;
	private static	final 	long AUTO_RELOAD_TIME = ConvertTimeUnit.minutesToMillis(30);
	private 		final 	ReentrantLock lock = new ReentrantLock();
	private			final 	Thread autoReloadJobsFromDirsAndFiles;
	private			final	String outPath;
	private			final	long outMaxsizeFile;
	private 		final	String logPath;
	private 		final	String logErrorFileName;
	private 		final	String logFileName;


	/************************************************* PUBLIC *************************************************/

	/**
	 * Create ManageTask
	 * @param outputPath
	 * @param maxsizeFile
	 * @param logPath
	 * @param logFileName
	 * @param logErrorFileName
	 * @throws IOException
	 */
	public ManageTask(String outputPath, long maxsizeFile, String logPath, String logFileName, String logErrorFileName) throws IOException{

		listInfoJob = new ArrayList<>();
		listFilesPath = new HashSet<>();
		listDirPath = new HashSet<>();
		listJobEnded = new HashSet<>();

		this.logErrorFileName = Objects.requireNonNull(logErrorFileName);
		this.logFileName = Objects.requireNonNull(logFileName);
		this.logPath = Objects.requireNonNull(logPath);
		outPath = Objects.requireNonNull(outputPath);
		outMaxsizeFile = maxsizeFile;
		autoReloadJobsFromDirsAndFiles = createAutoReloadThread();
		autoReloadJobsFromDirsAndFiles.start();
	}

	/**
	 * 
	 * @return String with the informations of jobs active and ended, and the number of tasks ended or not in this job
	 */
	public String infoManageTask(){
		
		int nbOfTasksComplete = 0;
		int nbOfTasksTotal = 0;
		StringBuilder sb = new StringBuilder();
		sb.append("\t---- Info Jobs ---");
		for(Job job : listJobEnded){
			sb.append("\n\t Job ").append(job.getJobId()).append(" - Tasks Done : All!");
			nbOfTasksComplete += job.numberOfTaskComplete();
			nbOfTasksTotal += job.getJobTaskNumber();
		}
		int nbOfJobsComplete = listJobEnded.size();
		int nbOfJobsTotal = listJobEnded.size();
		ArrayList<Job> jobsActifs;
		lock.lock();
		try{
			jobsActifs = new ArrayList<Job>(listInfoJob);
		} finally { lock.unlock(); }
		Job previousJob = null;
		int nbOfOcc = 0;
		for(Job job : jobsActifs){
			nbOfOcc ++;
			if(! job.equals(previousJob) ){
				if(previousJob != null)
					sb.append(" appeared ").append(nbOfOcc).append(" time");
				nbOfOcc = 0;
				sb.append("\n\t Job ").append(job.getJobId()).append(" - Tasks Done : ").append(job.numberOfTaskComplete())
				.append("/").append(job.getJobTaskNumber());
				nbOfTasksComplete += job.numberOfTaskComplete();
				nbOfTasksTotal += job.getJobTaskNumber();
				nbOfJobsTotal ++;
			}
			previousJob = job;
		}
		if(previousJob != null)
			sb.append(" appeared ").append(nbOfOcc+1).append(" time");
		sb.append("\n\tNumber of Jobs Completed : ").append(nbOfJobsComplete).append("/").append(nbOfJobsTotal)
		.append("\n\tNumber of Tasks Completed : ").append(nbOfTasksComplete).append("/").append(nbOfTasksTotal);
		return sb.append('\n').toString();

	}

	/**
	 * Get the number of Job ended
	 * @return numberOfJobEnded
	 */
	public int numberJobsEnded(){
		
		lock.lock();
		try{
			return listJobEnded.size();
		} finally { lock.unlock(); }
	}

	/**
	 * Load the Jobs not actually contained in the ManageTask from all paths already registry
	 * @return the number of new jobs added
	 * @throws IOException
	 */
	public int loadJobsFromAllPaths() throws IOException{

		int nbNewJobs = 0;
		lock.lock();
		try{
			for(Path p : listDirPath)
				addFilesFromPath(p);
			for(Path p : listFilesPath)
				nbNewJobs += loadJobsFromOneFile(p);		
		} finally { lock.unlock(); }
		return nbNewJobs;
	}

	/**
	 * Active the verbose mode of ManageTask
	 */
	public void setIsVerbose(){ isVerbose = true; }

	/**
	 * Close the Thread who reloaded automatically the new jobs from all paths already registry
	 */
	public void closeAutoReload(){
		
		toClose = true;
		try {
			autoReloadJobsFromDirsAndFiles.join(AUTO_RELOAD_TIME + ConvertTimeUnit.minutesToMillis(5));
		} catch (InterruptedException e) { e.printStackTrace(); }
		autoReloadJobsFromDirsAndFiles.interrupt();
	}

	/**
	 * Registry the file contain in the path passed in parameter
	 * @param path
	 * @return the number of new files added from this path
	 * @throws IOException
	 */
	public int addFilesFromPath(Path path) throws IOException{

		int i = 0;
		List<Path> listPath = FileUtils.filesFromDirectory(path);
		for(Path p : listPath)
			if(addFilePath(p) != null)
				i++;
		return i;
	}

	/**
	 * Get a tasks not ended in the ManageTask
	 * @return Optional<task> or Optional.empty if all tasks are already ended
	 */
	public Optional<Task> getOneTaskUnfinished(){
		
		lock.lock();
		try{
			StringBuilder sb = new StringBuilder().append("\nsearch a new task to get ...");	
			if(0 != listInfoJob.size()){
				int i = rand.nextInt(listInfoJob.size());
				sb.append("- get job :" + listInfoJob.get(i));
				logAndPrintIfVerbose(sb.append('\n').toString());
				Optional<Task> task = listInfoJob.get(i).getOneTask();
				return task;
			}
			sb.append("- Not found a new task to get");
			logAndPrintIfVerbose(sb.append('\n').toString());
			return Optional.empty();
		} finally { lock.unlock(); }
	}

	/**
	 * Set a tasks contains identify by the parameters
	 * @param jobId
	 * @param taskValue
	 * @param jsonResult
	 * @return true if the tasks is not already ended and now his ended, false otherwise
	 */
	public boolean endTask(long jobId, int taskValue, String jsonResult){

		try{
			JSONObject json = new JSONObject(jsonResult);
			Object answer = json.get(AnswerKind.ANSWER.toString());
			return endTaskType(jobId, taskValue, jsonResult, AnswerKind.ANSWER, json, answer);

		} catch (JSONException e){
			try{
				JSONObject json = new JSONObject(jsonResult);
				Object error = json.get(AnswerKind.ERROR.toString());
				return endTaskType(jobId, taskValue, jsonResult, AnswerKind.ERROR, json, error);
			} catch (JSONException jex){
				logErrorAndPrintIfVerbose("Error:" + jex + "\n- end job :" + jsonResult);
				return false;
			}
		}
	}

	/************************************************* PRIVATE *************************************************/

	private boolean endTaskType(long jobId, int taskValue, String jsonResult, AnswerKind type, JSONObject json, Object extractData) throws JSONException{

		boolean res = false;
		json.remove(type.toString());
		Task task = null;
		try{
			task = Task.createTaskFromJSONBlock(json.toString());
		}catch(JSONException jsone){
			logErrorAndPrintIfVerbose(jsone.toString());
			return false;
		}
		if(task.getJobId() != jobId || task.getTask() != taskValue){
			logAndPrintIfVerbose("-- end task -- : pb format");
			return false;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("search task\n");
		lock.lock();
		try{
			sb.append("...");
			for(Job job : listJobEnded){
				sb.append(".");
				if(job.containsTask(task)){
					logAndPrintIfVerbose(sb.append('\n').toString());
					return true;
				}
			}
			boolean ifJobEnded = false;
			sb.append("\n...");
			for(Job job : listInfoJob){
				sb.append(".");
				if(job.containsTask(task)){
					res = true;
					if(job.endTask(task)){
						sb.append("- end of task:" + task.getTask() + " from job:" + job.getJobId() +"\n\t with " + type + ":" + extractData);			
						writeAnswerToFile(task, jsonResult, json.getString("ClientId"));
					}
					if(job.jobEnded())
						ifJobEnded = true;
					break;
				}
			}
			logAndPrintIfVerbose(sb.append('\n').toString());
			if(ifJobEnded)
				transfertToEndedJobs();
		} finally { lock.unlock(); }
		return res;
	}

	private boolean transfertToEndedJobs(){

		StringBuilder sb = new StringBuilder();
		sb.append("Transfert Ended jobs\n");
		boolean transfert = false;
		for(int i = 0; i < listInfoJob.size(); ){
			sb.append(".");
			if(listInfoJob.get(i).jobEnded()){

				if(!transfert){
					System.out.println("transfert <"+ listInfoJob.get(i).getJobId() + "> to section jobs done");
					sb.append("\n- end of job:" + listInfoJob.get(i));
					listJobEnded.add(listInfoJob.remove(i));
				} else 
					listInfoJob.remove(i);
				transfert = true;
				i = 0;
			}else
				i++;
		}
		logAndPrintIfVerbose(sb.append('\n').toString());
		return transfert;
	}

	private Path addFilePath(Path filePath){

		lock.lock();
		try{
			if(listFilesPath.contains(filePath))
				return null;
			if(!listFilesPath.add(filePath))
				return null;
			logAndPrintIfVerbose("add Path:" + filePath);
			return filePath;
		} finally { lock.unlock(); }
	}

	private boolean writeAnswerToFile(Task task, String answer, String from){

		String path = outPath + task.getJobId();
		logAndPrintIfVerbose("Writting to :" + path + "...");
		try {	
			FileUtils.openAndWriteFile(path, task.getJobId() + "(" + task.getTask() + ")"+ from + ".json", answer, outMaxsizeFile);
			logAndPrintIfVerbose("... Wroten to :" + path);
			return true;
		} catch (IOException e) {
			logErrorAndPrintIfVerbose("Error write answer to :" + path +"\n.... Error :" + e);
			return false;
		}
	}

	private int loadJobsFromOneFile(Path filePath) throws IOException{

		int nbNewJobs = 0;
		if(filePath == null)
			return nbNewJobs;
		addFilePath(filePath);
		String json = Parse.parseFileToString(filePath);
		List<String> jsonBlocks = Parse.parseStringByBlockJSON(json);
		for(String block: jsonBlocks){
			try{
				List<Job> job = Job.createJobFromJSONBlock(block);
				if(addDistinctJobs(job))
					nbNewJobs++;
			}catch(JSONException jsonE){
				logErrorAndPrintIfVerbose(jsonE.toString());
			}
		}
		return nbNewJobs;
	}

	private boolean addDistinctJobs(List<Job> jobs){

		logAndPrintIfVerbose("Previously the number of jobs is:" + (listInfoJob.size() + listJobEnded.size())
				+ " and " + listJobEnded.size() + " jobs is already done!");
		lock.lock();
		try{
			if(jobs == null)
				return false;
			if(listJobEnded.contains(jobs.get(0)) || listInfoJob.contains(jobs.get(0)))
				return false;
			boolean res = true;

			for(Job job : jobs)
				res = res & listInfoJob.add(job);
			System.out.println("\nAdd Job :" + jobs.get(0));
			logAndPrintIfVerbose("Now the number of jobs is:" + (listInfoJob.size() + listJobEnded.size())
					+ " and " + listJobEnded.size() + " jobs is already done!");
			return res;
		} finally { lock.unlock(); }
	}

	private void logAndPrintIfVerbose(String message){ 		
		if(isVerbose)
			System.out.println('\n' + message);
		try {
			Logs.writeLog(logPath, LocalDate.now() + " " + logFileName, message);
		} catch (IOException e) { e.printStackTrace(); }
	}

	public void logErrorAndPrintIfVerbose(String message){
		if(isVerbose)
			System.err.println('\n' + message);
		try {
			Logs.writeLog(logPath, LocalDate.now() + " " + logErrorFileName, message);
		} catch (IOException e) { e.printStackTrace(); }
	}

	private Thread createAutoReloadThread() {

		return new Thread(() -> {
			while(!toClose && !Thread.interrupted()){
				long reloadTime = AUTO_RELOAD_TIME;
				try {
					Thread.sleep(reloadTime);
				} catch (InterruptedException e) { e.printStackTrace(); }
				try {
					loadJobsFromAllPaths();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
	}

}
