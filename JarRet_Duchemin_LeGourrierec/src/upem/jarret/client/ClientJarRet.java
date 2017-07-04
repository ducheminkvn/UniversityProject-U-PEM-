package upem.jarret.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import upem.jarret.client.http.HTTPHeader;
import upem.jarret.client.http.HTTPReader;
import upem.jarret.utils.CodeProtocol.AnswerKind;
import upem.jarret.utils.CodeProtocol.Error;
import upem.jarret.utils.CodeProtocol.HTTPProtocolCode;
import upem.jarret.utils.ConvertTimeUnit;
import upem.jarret.utils.JsonUtils;
import upem.jarret.utils.Logs;
import upem.jarret.worker.Worker;
import upem.jarret.worker.WorkerFactory;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class ClientJarRet {
	private					boolean verbose = false;
	private					boolean debug = false;
	/*HashMap <jobId, <workerVersion, worker>>*/
	private	HashMap<Long, HashMap<String, Worker>>	workersDownloaded = new HashMap<>();

	private static	final	Charset charsetASCII = Charset.forName("ASCII");
	private static	final	Charset charsetUTF8 = Charset.forName("UTF-8");
	private static	final 	int BUFF_OUT_MAX_SIZE = 4096;
	private static	final 	int BUFF_IN_SIZE= 1024;
	private			final	ByteBuffer bbIn = ByteBuffer.allocate(BUFF_IN_SIZE);
	private					ByteBuffer bbOut;
	private					ByteBuffer bbLastResult = null;
	private 		final	String clientId;
	private 				SocketChannel sc;
	private 		final	String resource;
	private 		final	InetSocketAddress server;
	private					String defaulLogPath;
	private 				HTTPReader reader;
	private 				HTTPHeader header;

	/************************************* PUBLIC *************************************/

	/**
	 * Enable the ClientJarRet's verbose display mode 
	 */
	public void setVerbose(){ this.verbose = true; };

	/**
	 * Enable the ClientJarRet's debug display mode 
	 */
	public void setDebug() { this.debug = true;}

	public ClientJarRet(SocketChannel sc, String clientId, String resource, InetSocketAddress server){

		this.sc = Objects.requireNonNull(sc);
		if(Objects.requireNonNull(clientId).length() > 20)
			this.clientId = clientId.substring(0, 20);
		else
			this.clientId = clientId;
		this.resource = Objects.requireNonNull(resource);
		this.server = Objects.requireNonNull(server);
		defaulLogPath = "./output/logsClient/" + this.clientId +"/";
		reader = new HTTPReader(sc,bbIn);
		System.out.println("\nYour Client Id are : " + this.clientId);
	}

	/**
	 * Launch the ClientJarRet
	 * @throws IOException
	 * @throws JSONException
	 */
	public void launch() throws IOException,JSONException {		

		while(!Thread.interrupted()){

			if(bbLastResult != null && bbOut != null && bbOut.position()!=0){
				if(!reconnectionIfFail(()->writeBBOut()))
					return;
				bbLastResult = null;
			}
			if(!reconnectionIfFail(()->askNewTask()))
				return;
			
			displayIfDebug("Reading to Socket ...");

			displayIfDebug(header.toString());

			displayIfDebug("Verify Code HTTP from server answer ...");
			if( !verifyHTTPCodeIsOK(header.getCode()) ) { 
				logAndDisplayIfVerbose("Isn't OK Server Answer - Code HTTP : " + header.getCode());
				continue; 
			}

			displayIfDebug("Reading ... " + header.getContentLength() + "bytes");
			if( header.getContentLength() < 0){
				logErrorAndDisplayIfVerbose("Content-Length is negatif :" + header.getContentLength() + "bytes");
				resetConnection();
				continue;
			}
			ByteBuffer content = reader.readBytes(header.getContentLength());
			content.flip();
			displayIfDebug("Bytes reading transfert to ByteBuffer" + content);


			JSONObject jsonWorkerDescribe = new JSONObject(charsetUTF8.decode(content).toString());

			logAndDisplayIfVerbose("Data :\n" + jsonWorkerDescribe);

			if(jsonWorkerDescribe.keySet().contains("ComeBackInSeconds")){
				try {
					System.out.println("ComeBack Start " + LocalDateTime.now());
					int comeBackDuration = jsonWorkerDescribe.getInt("ComeBackInSeconds");
					System.out.println("ComeBack Duration : " + comeBackDuration 
							+ "\nComeBack Start " + LocalDateTime.now());
					Thread.sleep(comeBackDuration * 1000);
				} catch (JSONException | InterruptedException e) {}
				System.out.println("ComeBack End " + LocalDateTime.now());

				continue;	
			}

			long jobId =  jsonWorkerDescribe.getLong("JobId");
			String workerVersion = jsonWorkerDescribe.getString("WorkerVersion");
			String workerURL = jsonWorkerDescribe.getString("WorkerURL");
			String workerClassName = jsonWorkerDescribe.getString("WorkerClassName");
			int taskNumber = jsonWorkerDescribe.getInt("Task");
			
			try {
				displayIfDebug("Search Worker ...");

				Worker worker;
				HashMap<String, Worker> mapJobId = workersDownloaded.get(jobId);
				if(mapJobId == null){
					displayIfDebug("New Worker ...");

					mapJobId = new HashMap<>();
					worker = WorkerFactory.getWorker(workerURL, workerClassName);
					mapJobId.put(workerVersion, worker);
					workersDownloaded.put(jobId, mapJobId);
				} else {
					worker = mapJobId.get(workerVersion);
					if(worker == null){
						displayIfDebug("New Worker ...");

						worker = WorkerFactory.getWorker(workerURL, workerClassName);
						mapJobId.put(workerVersion, worker);
					}
				}

				logAndDisplayIfVerbose("Worker is working ...");

				String jsonAnswer = worker.compute(taskNumber);
				if(jsonAnswer == null){
					//"Error" : "Computation error"
					logErrorAndDisplayIfVerbose("Error:Computation error");
					bbOut = createAnswerPacket(jobId, workerVersion, workerURL, workerClassName, taskNumber, AnswerKind.ERROR, Error.ERROR_COMPUTATION.toString());
					bbLastResult = null;
				} else {
					if(!JsonUtils.isJSONValid(jsonAnswer)){
						// "Error" : "Answer is not valid JSON"
						logErrorAndDisplayIfVerbose("Error:Answer is not valid JSON");
						bbOut = createAnswerPacket(jobId, workerVersion, workerURL, workerClassName, taskNumber, AnswerKind.ERROR, Error.ERROR_ANSWER_NOT_VALID_JSON.toString());
						bbLastResult = null;

					} else {
						if(JsonUtils.jsonContainsObject( new JSONObject(jsonAnswer) )){
							// "Error" : "Answer is nested"
							logErrorAndDisplayIfVerbose("Error:Answer is nested");
							bbOut = createAnswerPacket(jobId, workerVersion, workerURL, workerClassName, taskNumber, AnswerKind.ERROR, Error.ERROR_ANSWER_NESTED.toString());	
							bbLastResult = null;
						} else { 
							// Normal State 
							logAndDisplayIfVerbose("State:compute is alright");
							bbOut = createAnswerPacket(jobId, workerVersion, workerURL, workerClassName, taskNumber, AnswerKind.ANSWER, jsonAnswer);
							bbLastResult = createAnswerPacket(jobId, workerVersion, workerURL, workerClassName, taskNumber, AnswerKind.ANSWER, jsonAnswer);
						}
					}
				}
			} catch (Exception e) {
				// "Error" : "Computation error"
				logErrorAndDisplayIfVerbose("Error:Computation error");
				bbOut = createAnswerPacket(jobId, workerVersion, workerURL, workerClassName, taskNumber, AnswerKind.ERROR, Error.ERROR_COMPUTATION.toString());
				bbLastResult = null;
			}
			if(!sendAnswer()){
				resetConnection();
			}
		}
	}

	/**
	 * Print in the Usage
	 */
	public static void usage(){ System.err.println("Usage : ClientJarRet [-d|--debug] [-v|--verbose] clientId host port");}


	/************************************* PRIVATE *************************************/


	private boolean verifyHTTPCodeIsOK(int code){

		if(code == HTTPProtocolCode.OK_CODE.getCode())
			return true;
		if(code == HTTPProtocolCode.BAD_REQUEST_CODE.getCode())
			logErrorAndDisplayIfVerbose("Bad Request - code : " + code);
		else
			logErrorAndDisplayIfVerbose("Bad Packet - Unkonwn code : " + code);
		return false;
	}

	private void resetConnection(){

		logAndDisplayIfVerbose("Reset Connection ...");
		try {
			silentlyClose(sc);
			sc = SocketChannel.open(server);
		} catch (IOException e) { logErrorAndDisplayIfVerbose("Reset Connection Error :" + e.toString());}
		if(bbLastResult != null && bbLastResult.position() != 0){
			displayIfDebug("Refilled bbOut with last Result ...");
			bbOut = ByteBuffer.allocate(bbLastResult.capacity());
			bbLastResult.flip();
			displayIfDebug("bbLastResult" + bbLastResult);
			bbOut.put(bbLastResult);
			displayIfDebug("bbLastResult" + bbLastResult);
			bbOut.flip();
			bbOut.compact();
		}
		bbIn.clear();
		reader = new HTTPReader(sc,bbIn);
	}

	private boolean askNewTask(){
		logAndDisplayIfVerbose("\n<{|------------\t Start ask new Task \t\t------------|}>\n");
		bbOut = createRequestTaskPacket(resource);	
		bbOut.compact();
		return writeBBOut() && readHeader();
	}

	private boolean readHeader(){
		try{
			header = reader.readHeader();
		} catch (IOException e){
			logErrorAndDisplayIfVerbose(e.toString());
			return false;
		}
		return true;
	}

	private boolean writeBBOut(){
		bbOut.flip();
		displayIfDebug("Write :" + bbOut);

		while(bbOut.hasRemaining())
			try{
				sc.write(bbOut);
			} catch (IOException io){
				logErrorAndDisplayIfVerbose("Write ERROR - " + io.toString());
				return false;
			}
		bbOut.compact();
		return true;
	}

	/**
	 * 
	 * @param resource
	 * @return ByteBuffer in read-mode
	 */
	private ByteBuffer createRequestTaskPacket(String resource){

		return charsetASCII.encode("GET Task HTTP/1.1\r\n"
				+ "Host: " + resource + "\r\n"
				+ "\r\n");
	}

	/**
	 * 
	 * @param jobId
	 * @param workerVersion
	 * @param workerURL
	 * @param workerClassName
	 * @param taskNumber
	 * @param answerKind
	 * @param jsonAnswer
	 * @return ByteBuffer in write-mode
	 */
	private ByteBuffer createAnswerPacket(long jobId, String workerVersion, String workerURL, String workerClassName, int taskNumber, AnswerKind answerKind, String jsonAnswer){

		ByteBuffer bb = ByteBuffer.allocate(BUFF_OUT_MAX_SIZE);

		ByteBuffer encodeContent = createJsonContent(jobId, workerVersion, workerURL, workerClassName, taskNumber, answerKind, jsonAnswer);

		int sizeContent = Long.BYTES + Integer.BYTES + encodeContent.remaining();

		ByteBuffer header = createHeaderPostAnswer(sizeContent);

		if(bb.capacity() <  header.remaining() + sizeContent){
			answerKind = AnswerKind.ERROR;
			jsonAnswer = Error.ERROR_ANSWER_TOO_LONG.toString();
			encodeContent = createJsonContent(jobId, workerVersion, workerURL, workerClassName, taskNumber, answerKind, jsonAnswer);

			sizeContent = Long.BYTES + Integer.BYTES + encodeContent.remaining();
			header = createHeaderPostAnswer(sizeContent);
		}

		bb.put(header);
		bb.order();
		bb.putLong(jobId);
		bb.putInt(taskNumber);
		bb.put(encodeContent);

		logAndDisplayIfVerbose("Computation result :\n"+jobId + "," +  workerVersion + "," + workerURL + "," + workerClassName + "," + taskNumber + "," + answerKind + "," + jsonAnswer);

		return bb;
	}
	/**
	 * 
	 * @param size
	 * @return ByteBuffer in read-mode
	 */
	private ByteBuffer createHeaderPostAnswer(int size) {

		return charsetASCII.encode("POST Answer HTTP/1.1\r\n"
				+ "Host: " + resource + "\r\n"
				+ "Content-Type: application/json\r\n"
				+ "Content-Length: "+ size +"\r\n" 
				+ "\r\n");
	}

	/**
	 * 
	 * @param jobId
	 * @param workerVersion
	 * @param workerURL
	 * @param workerClassName
	 * @param taskNumber
	 * @param answerKind
	 * @param jsonAnswer
	 * @return ByteBuffer in read-mode
	 */
	private ByteBuffer createJsonContent(long jobId, String workerVersion, String workerURL, String workerClassName, int taskNumber, AnswerKind answerKind, String jsonAnswer) {

		String json = "{"
				+"\"JobId\" : \""+jobId
				+"\", \"WorkerVersion\" : \""+workerVersion
				+"\", \"WorkerURL\" : \""+workerURL
				+"\", \"WorkerClassName\" : \""+workerClassName
				+"\", \"Task\" : \""+taskNumber
				+"\", \"ClientId\" : \""+clientId
				+"\", \""+answerKind+"\" : "+jsonAnswer
				+ "}";

		return charsetUTF8.encode(json);
	}		

	private void displayIfDebug(String message){
		if(debug)
			System.out.println('\n'+message);
	}

	private void logErrorAndDisplayIfVerbose(String message){

		if(verbose)
			System.err.println('\n' + message);
		try {
			Logs.writeLog(defaulLogPath, LocalDate.now() + " Client("+ clientId +")LogError.txt", message);
		} catch (IOException e) { e.printStackTrace(); }
	}

	private void logAndDisplayIfVerbose(String message){ 
		if(verbose)
			System.out.println('\n' + message);
		try {
			Logs.writeLog(defaulLogPath, LocalDate.now() + " Client("+ clientId +")Log.txt", message);
		} catch (IOException e) { e.printStackTrace(); }
	}

	@FunctionalInterface
	private interface RunnableIOException{

		public boolean run() throws IOException;
	}

	private boolean reconnectionIfFail(RunnableIOException r){

		long timeSleep = ConvertTimeUnit.secondsToMillis(2);
		int i = 0;
		while(i<3){
			i++;
			try{
				if(r.run())
					return true;
			}
			catch (IOException io){
				logErrorAndDisplayIfVerbose("Reconnection to server ...");
				logErrorAndDisplayIfVerbose(io.toString());
				resetConnection();
			}
			try {
				System.out.println("Reconnection tentative in " + timeSleep + " seconds");
				Thread.sleep(timeSleep);
			} catch (InterruptedException ie) {}
			timeSleep *= 2;
			resetConnection();
		}
		return false;
	}
	
	private boolean sendAnswer(){
		displayIfDebug("Answer Client transfert to ByteBuffer" + bbOut);

		if(!reconnectionIfFail(()->writeBBOut()))
			return false;

		displayIfDebug("Reading Last Header from Server ...");
		if(!readHeader())	
			return false;

		displayIfDebug("Verify Last Code from server ...");
		if(!verifyHTTPCodeIsOK(header.getCode()))
			logErrorAndDisplayIfVerbose("\n<{|************\t END - Code is a BAD_REQUEST_CODE \t************|}>\n");
		else
			logAndDisplayIfVerbose("\n<{|------------\t END - Code is a OK_CODE \t------------|}>\n");
		bbLastResult = null;
		return true;
	}

	private void silentlyClose(SocketChannel sc) {
		if (sc != null) {
			try {
				logAndDisplayIfVerbose("SilentlyClose ...");
				sc.close();
			} catch (IOException e) { logErrorAndDisplayIfVerbose(e.toString());}
		}
	}

	/************************************* MAIN *************************************/
	public static void main(String[] args) throws IOException {

		String clientId;
		String addrSrv;
		int port;

		if(args.length < 3 || args.length > 5){
			usage();
			return;
		} 

		int indexStartAt = args.length - 3;
		clientId = args[indexStartAt + 0];
		addrSrv = args[indexStartAt + 1];
		port = Integer.parseInt(args[indexStartAt + 2]);

		InetSocketAddress server = new InetSocketAddress(addrSrv,port);

		try (SocketChannel sc = SocketChannel.open(server)) {
			ClientJarRet client = new ClientJarRet(sc,clientId,addrSrv, server);
			boolean verboseMode = false;
			boolean debugMode = false;
			switch(args[0]){
			case "-d":
				debugMode = true;
				break;
			case "--debug":
				debugMode = true;
				break;
			case "-v":
				verboseMode = true;
				break;
			case "--verbose":
				verboseMode = true;
				break;
			case "-vd":
				verboseMode = true;
				debugMode = true;
				break;
			case "-dv":
				verboseMode = true;
				debugMode = true;
				break;
			default:
				if(args.length != 3){
					usage();
					return;
				}
				break;
			}
			if(args.length == 5){
				switch(args[1]){
				case "-d":
					if(debugMode){
						usage();
						return;
					}
					debugMode = true;
					break;
				case "--debug":
					if(debugMode){
						usage();
						return;
					}
					debugMode = true;
					break;
				case "-v":
					if(verboseMode){
						usage();
						return;
					}
					verboseMode = true;
					break;
				case "--verbose":
					if(verboseMode){
						usage();
						return;
					}
					verboseMode = true;
					break;
				default:
					usage();
					return;
				}
			}
			if(debugMode)
				client.setDebug();
			if(verboseMode)
				client.setVerbose();
			client.launch();
		}
	}
}