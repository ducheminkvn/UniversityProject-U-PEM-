package upem.jarret.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONException;

import upem.jarret.http.HTTPException;
import upem.jarret.server.http.HTTPReaderServer;
import upem.jarret.server.http.HeaderClientToServer;
import upem.jarret.server.job.ManageTask;
import upem.jarret.server.job.Task;
import upem.jarret.utils.CodeProtocol.AnswerKind;
import upem.jarret.utils.CodeProtocol.HTTPProtocolCode;
import upem.jarret.utils.ConvertTimeUnit;
import upem.jarret.utils.JsonUtils;
import upem.jarret.utils.Logs;
import upem.jarret.utils.Parse;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class ServerJarRet {

	 
	private static enum State{
		READ_MORE_FOR_HEADER_NEEDED,
		READ_HEADER,
		TASK,
		RESULT,
		WRITE_FULL,
		READ_MORE_FOR_RESULT_NEEDED,
		NEED_CLOSED;
	}	

	/******************************************************* START INNER-CLASS CONTEXT *******************************************************/

	class Context{

		private final	int idContext = numberOfContextCreated ++ ;
		private final 	ByteBuffer bbIn =  ByteBuffer.allocateDirect(BUFF_SIZE_IN);
		private final 	ByteBuffer bbOut =  ByteBuffer.allocateDirect(BUFF_SIZE_OUT);
		private 		HTTPReaderServer readerServer;
		private 		SocketChannel sc;
		private 		SelectionKey key;
		private 		HeaderClientToServer header = null;
		private 	   	long timeRemainingBeforeClose;
		private	final	long TIMEOUT = ConvertTimeUnit.secondsToMillis(15);
		private 		State state;
		private			boolean byteBufferOutIsFull = false;
		private final	int MAX_HEADER_CODE_SIZE = Integer.max(createHeaderAnswer(AnswerKind.ERROR).remaining(), createHeaderAnswer(AnswerKind.ANSWER).remaining());
		private			StringBuilder log = new StringBuilder();

		/******************************************************* PUBLIC CONTEXT *******************************************************/

		/**
		 * Set the flag for indicate the needed to close this Context to true
		 */
		public void setNeedClosed(){ 

			addLogAndDisplayIfVerbose(log,  "---Context " + idContext  + " --- Need to be closed this context");
			state = State.NEED_CLOSED;
		}

		/******************************************************* PRIVATE CONTEXT *******************************************************/

		private Context(SocketChannel sc, SelectionKey key) {

			readerServer = new HTTPReaderServer(bbIn); 
			resetTimeRemainingBeforeClose();
			state = State.READ_HEADER;
			this.sc = sc; 
			this.key = key;
		}

		private Context() {  

			readerServer = new HTTPReaderServer(bbIn); 
			resetTimeRemainingBeforeClose();
			state = State.READ_HEADER; 
			this.sc = null; 
			this.key = null; 
		}

		private Context setContext(SocketChannel sc, SelectionKey key) {  

			readerServer = new HTTPReaderServer(bbIn); 
			resetTimeRemainingBeforeClose();
			state = State.READ_HEADER; 
			this.sc = sc; 
			this.key = key; 
			return this;
		}

		private int getIdContext(){ return idContext; }

		private boolean needClosed(){ return state == State.NEED_CLOSED; }

		private boolean ByteBufferOutIsFull(){ return byteBufferOutIsFull; } 

		private void subInactiveTime(long time) { timeRemainingBeforeClose -= time; }

		private boolean isInactif() { return timeRemainingBeforeClose <= 0; }

		private void resetTimeRemainingBeforeClose(){ timeRemainingBeforeClose = TIMEOUT; }

		 
		private void doRead() throws IOException{

			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Read with state : " + state);
			if(ByteBufferOutIsFull()){
				updateInterestOps();
				return;
			}
			switch(state){
			case READ_MORE_FOR_HEADER_NEEDED:
				state = State.READ_HEADER;
				int n = sc.read(bbIn);
				process();
				if(-1 == n)
					throw new HTTPException();
				break;
			case READ_MORE_FOR_RESULT_NEEDED:
				state = State.RESULT;
				int ret = sc.read(bbIn);
				process();
				if(-1 == ret)
					throw new HTTPException();
				break;
			default:
				process();
				break;
			}
			updateInterestOps();
		}
	 
		private void doWrite() throws IOException {

			bbOut.flip();
			if(sc.write(bbOut) > 0)
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Write");
			bbOut.compact();
			byteBufferOutIsFull = false;
			process();
		}

		private void updateInterestOps(){

			addLogAndDisplayIfVerbose(log, "UpdateInterestOps ---Context " + getIdContext() + " --- :");
			int ops = 0;
			if(!ByteBufferOutIsFull() && bbIn.hasRemaining() && !needClosed()){
				ops |=  SelectionKey.OP_READ;
				addLogAndDisplayIfVerbose(log, "* READ *");
			}
			if(bbOut.position() != 0 ){
				ops |= SelectionKey.OP_WRITE;
				addLogAndDisplayIfVerbose(log, "* WRITE *");
			}
			if(ops == 0){ 
				if(needClosed()){
					addLogAndDisplayIfVerbose(log, "- need to be closed -");
					writeStandardLog(log);
				}
				Context context = (Context) key.attachment();
				if(context != null)
					contextUnused.add(context);
				silentlyClose(key.channel(), log);
				return;
			}
			key.interestOps(ops);
			writeStandardLog(log);
		}

		private void process() throws IOException{

			switch(state){
			case READ_HEADER:
				if(ByteBufferOutIsFull()){
					writeStandardLog(log);
					return;
				}
				processHeader();
				break;
			case TASK:
				if(ByteBufferOutIsFull()){
					writeStandardLog(log);
					return;
				}
				processTask();
				break;			
			case RESULT:
				if(ByteBufferOutIsFull()){
					writeStandardLog(log);
					return;
				}
				processResult();
				break;
			default:
				updateInterestOps();
				break;
			}
			writeStandardLog(log);
		}
		 
		private void processHeader() throws IOException{
			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Read Header ...");			
			if(header == null){
				Optional<HeaderClientToServer> headerOptional = readerServer.readHeader();
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Header is Created ?");
				if(!headerOptional.isPresent()){
					log = new StringBuilder();
					state = State.READ_MORE_FOR_HEADER_NEEDED;
					writeStandardLog(log);
					return;
				}
				header = headerOptional.get();			
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- YES");
			}
			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Read Header OK : \n" + header);
			switch(header.getTypeProtocol()){
			case "GET":
				state = State.TASK;
				process();
				return;
			case "POST":
				state = State.RESULT;
				process();
				return;
			default:
				break;	
			}	
		}
		 
		private void processTask() throws IOException{

			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- GET : ");
			String addressServer = header.getHost();
			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- From : " + addressServer);
			Optional<Task> job = jobs.getOneTaskUnfinished();
			ByteBuffer bbJson;
			ByteBuffer head;
			if(job.isPresent()){
				bbJson = createJsonContent(job.get());
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- job loaded: " + job);
			} else {
				bbJson = CHARSET_UTF8.encode("{ \"ComeBackInSeconds\" : \"" + serverParam.getComeBackTime() +"\" }");
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- ComeBack Loaded");
			}
			head = createHeaderGiveTask(bbJson.remaining());
			if(bbOut.remaining() < head.remaining() + bbJson.remaining()){
				byteBufferOutIsFull = true;
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- ByteBuffer for Output haven't enougth space " + bbOut.remaining() + head.remaining() + bbJson);
				writeStandardLog(log);
				return;
			}
			bbOut.put(head);
			bbOut.put(bbJson);
			header = null;
			state = State.READ_HEADER;
			process();
		}


		private ByteBuffer createHeaderGiveTask(int size){ 
			return CHARSET_ASCII.encode(
					"HTTP/1.1 200 OK\r\n"
							+ "Content-type: application/json; charset=utf-8\r\n"
							+ "Content-Length: " + size + "\r\n"
							+ "\r\n"); 
		}

		private ByteBuffer createJsonContent(Task task){ return CHARSET_UTF8.encode(task.jsonForPacket()); }

		 
		private void processResult() throws IOException{

			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- POST :");
			if(bbOut.remaining() < MAX_HEADER_CODE_SIZE){
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- ByteBuffer for Output haven't enougth space");
				writeStandardLog(log);
				byteBufferOutIsFull = true;
				return;
			}
			Optional<ByteBuffer> bb = readerServer.readBytes(header.getContentLength());
			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- ReadBytes ...");
			if(!bb.isPresent()){
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- ReadBytes need more bytes - actually:" + bbIn.position() + "bytes - need :" + header.getContentLength());
				writeStandardLog(log);
				state = State.READ_MORE_FOR_RESULT_NEEDED;
				return;
			}
			bb.get().flip();
			long jobId = bb.get().getLong();
			int taskValue = bb.get().getInt();
			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Received job : " + jobId + ", task : " + taskValue + "\n---Context " + idContext  + " --- Decode Answer ...");
			String json = CHARSET_UTF8.decode(bb.get()).toString();
			addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- ... " + json);
			if(JsonUtils.isJSONValid(json)){
				if(jobs.endTask(jobId, taskValue, json))
					System.out.println("Task: " + taskValue +" from job: "+jobId+" now is done !");
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Create Answer OK");
				bbOut.put(createHeaderAnswer(AnswerKind.ANSWER));
			} else {
				addLogAndDisplayIfVerbose(log, "---Context " + idContext  + " --- Create Answer Bad Request");
				bbOut.put(createHeaderAnswer(AnswerKind.ERROR));
			}
			header = null;
			state = State.READ_HEADER;
			process();
		}

		private ByteBuffer createHeaderAnswer(AnswerKind answer){ 

			if(answer == AnswerKind.ANSWER)
				return (CHARSET_ASCII.encode("HTTP/1.1 " + HTTPProtocolCode.OK_CODE + " OK\r\n\r\n")); 
			else
				return (CHARSET_ASCII.encode("HTTP/1.1 " + HTTPProtocolCode.BAD_REQUEST_CODE + " Bad Request\r\n\r\n")); 
		}
	}

	/******************************************************* END INNER-CLASS CONTEXT *******************************************************/

	private	static			boolean verbose = false;
	private 		final 	ServerSocketChannel serverSocketChannel;
	private 		final 	Selector selector;
	private 		final 	Set<SelectionKey> selectedKeys;
	private         final   Set<SelectionKey> keys;
	private 		final 	ServerParameters serverParam;
	private static	final 	int BUFF_SIZE_IN = 4096;
	private static	final 	int BUFF_SIZE_OUT = 1024;
	private static	final	Charset CHARSET_ASCII = Charset.forName("ASCII");
	private static	final	Charset CHARSET_UTF8 = Charset.forName("UTF-8");
	private 		final	ManageTask jobs;
	private         final   long comeBackTime; 
	private 		final   LinkedBlockingDeque<Command> bqCommand = new LinkedBlockingDeque<>();
	private static			String logPath;
	private static	final	String logErrorFileName = "ServerLogError.txt";
	private static	final	String logFileName = "ServerLog.txt";
	private static			int numberOfContextCreated = 0;
	private 				StringBuilder log = new StringBuilder();
	private 		final	LinkedList<Context> contextUnused = new LinkedList<>();

	private static enum Command{
		SHUTDOWN,
		SHUTDOWN_NOW,
		INFO;
	}

	/******************************************************* PUBLIC *******************************************************/

	/**
	 * Create a ServerJarRet
	 * @param serverSocketChannel
	 * @param serverParam
	 * @throws IOException
	 */
	public ServerJarRet(ServerSocketChannel serverSocketChannel,ServerParameters serverParam) throws IOException {

		System.out.println("Server Init ...");
		this.serverSocketChannel = serverSocketChannel;
		this.serverParam = serverParam;
		serverSocketChannel.bind( new InetSocketAddress(serverParam.getPort()) );
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		keys = selector.keys();
		logPath = serverParam.getLogPath();
		jobs = new ManageTask(serverParam.getFilesAnswerPath(), serverParam.getMaxSizeFileAnswer(), logPath, logFileName, logErrorFileName);
		comeBackTime = ConvertTimeUnit.secondsToMillis(serverParam.getComeBackTime());
		contextUnused.add(new Context(null, null));
		System.out.println("Server Init Finish");
	}

	/**
	 * Launch the ServerJarRet
	 * @throws IOException
	 */
	public void serve() throws IOException {

		long start = 0, end = 0;
		Command command;
		Thread consoleThread = createThreadConsole(System.in);

		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		consoleThread.start();
		while (!Thread.interrupted()) {
			start = System.currentTimeMillis();
			selector.select(comeBackTime/100);
			while((command = bqCommand.poll()) != null)
				handleCommand(command);
			if(keys.size() == 0){
				consoleThread.interrupt();
				jobs.closeAutoReload();
				return;
			}
			processSelectedKeys();
			end = System.currentTimeMillis();
			updateInactiveKeys(end - start);
			selectedKeys.clear();
			writeStandardLog(log);
		}
	}

	/**
	 * Set the ServerJarRet in verbose mode
	 */
	public void setIsVerbose(){ 

		jobs.setIsVerbose();
		verbose = true; 
	}

	/**
	 * Print the Usage
	 */
	public static void usage(){ System.err.println("Usage : ServerJarRet [-v|--verbose] pathConfig"); }

	/******************************************************* PRIVATE *******************************************************/

	private Thread createThreadConsole(InputStream in) {

		return new Thread(()->{
			Scanner scan = new Scanner(in);

			while(!Thread.interrupted() && scan.hasNextLine()){
				switch (scan.nextLine().toUpperCase()) {
				case "SHUTDOWN": 
					bqCommand.add(Command.SHUTDOWN);
					selector.wakeup();
					break;
				case "SHUTDOWN NOW": 
					bqCommand.add(Command.SHUTDOWN_NOW);
					selector.wakeup();
					scan.close();
					Thread.currentThread().interrupt();
					break;
				case "INFO": 
					bqCommand.add(Command.INFO);
					selector.wakeup();
					break;
				default:
					System.out.println("Unknown command");
					System.out.println("Commands Available :"
							+ "\n\t SHUTDOWN : Acceptation of new Connection is stopped"
							+ "\n\t SHUTDOWN NOW : Closed all connections and don't accept a new connection"
							+ "\n\t INFO : display job's info and connection\n");
					break;
				}
			}
			scan.close();
		});
	}

	private void updateInactiveKeys(long time) {

		for(SelectionKey key:keys)
			if(!(key.channel() instanceof ServerSocketChannel)){
				Context context = (Context) key.attachment();
				context.subInactiveTime(time);
				if(context.isInactif()){
					contextUnused.add(context);
					silentlyClose(key.channel(), log);
				}
			}
	}

	private void handleCommand(Command command) {

		switch (command) {
		case SHUTDOWN:
			killServerSocketChannel();
			System.out.println("\t ServerSocketChannel Closed"); 
			break;
		case SHUTDOWN_NOW:
			killServerSocketChannel();
			System.out.println("\t ServerSocketChannel Closed" 
					+ "\n\t " + killSocketsChannel() + " socket closed from SHUTDOWN NOW"
					+ "\n" + jobs.infoManageTask() + "\n");
			return;
		case INFO: 
			System.out.println("\tNumber of Connections : " + keys.size()
			+ "\n" + jobs.infoManageTask() + "\n");
			break;
		default:
			break;
		}
	}

	private void killServerSocketChannel() { silentlyClose(serverSocketChannel, log); }

	private int killSocketsChannel() {

		int nbKill = 0;
		for(SelectionKey key : keys){
			silentlyClose(key.channel(), log);
			nbKill ++;
		}
		return nbKill;
	}

	private void processSelectedKeys() throws IOException {

		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
				continue;
			}
			try{
				((Context) key.attachment()).resetTimeRemainingBeforeClose();
				if (key.isValid() && key.isWritable())
					((Context) key.attachment()).doWrite();
				if (key.isValid() && key.isReadable()) 
					((Context) key.attachment()).doRead();
				((Context) key.attachment()).resetTimeRemainingBeforeClose();
			}catch(IOException io){
				writeLogErrorAndDisplayIfVerbose(io.toString());
				Context context = (Context) key.attachment();
				if(context != null)
					contextUnused.add(context);
				silentlyClose(key.channel(), log);
			}
		}
	}

	private void doAccept(SelectionKey key) throws IOException {

		addLogAndDisplayIfVerbose(log, "DoAccept ...");
		SocketChannel sc = serverSocketChannel.accept();
		if(sc == null)
			return;
		sc.configureBlocking(false);
		SelectionKey client = sc.register(selector, SelectionKey.OP_READ);
		client.attach(contextUnused.poll().setContext(sc, client));
		if(contextUnused.isEmpty())
			contextUnused.add(new Context());
		System.out.println("\nAccepted a new Client :" + client + "\n" + "On Socket :" + client.channel());
		log.append("Accepted a new Client :" + client + "\n" + "On Socket :" + client.channel());
		writeStandardLog(log);
	}

	private void silentlyClose(SelectableChannel sc, StringBuilder log) {

		if (sc != null) {
			try {
				System.out.print("\nClose Socket :"+ sc + "\n");
				System.out.println(jobs.infoManageTask());
				addLogAndDisplayIfVerbose(log, "SilentlyClose ... " + sc);
				writeStandardLog(log);
				sc.close();
			} catch (IOException e) { writeLogErrorAndDisplayIfVerbose(e.toString());}
		}
	}

	private static void addLogAndDisplayIfVerbose(StringBuilder log, String message){ 	

		if(verbose)
			System.out.println('\n' + message);
		log.append(message).append('\n');
	}

	private static void writeStandardLog(StringBuilder log){

		if(log.toString().equals(""))
			return;
		try {
			Logs.writeLog(logPath, LocalDate.now() + " " +  logFileName, log.toString());
			log = new StringBuilder();
		} catch (IOException e) { e.printStackTrace(); } 
	}

	private void writeLogErrorAndDisplayIfVerbose(String message){

		if(verbose)
			System.err.println('\n' + message);
		try {
			Logs.writeLog(logPath, LocalDate.now() + " " + logErrorFileName, message);
		} catch (IOException e) { e.printStackTrace(); }
	}

	/******************************************************* MAIN *******************************************************/

	public static void main(String[] args){

		if(args.length != 1 && args.length != 2){
			usage();
			return;
		}
		int indexStartAt = args.length - 1;
		String pathJarRetConfig = args[indexStartAt + 0];

		try{
			String jsonConfig = Parse.parseFileToString(Paths.get(pathJarRetConfig));

			if(JsonUtils.isJSONValid(jsonConfig)){
				try{
					ServerParameters serverParam = ServerParameters.createFromJsonFile(jsonConfig);
					try(ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()){
						ServerJarRet serverJarRet = new ServerJarRet(serverSocketChannel,serverParam);
						if(args.length == 2)
							switch(args[0]){
							case "-v":
								serverJarRet.setIsVerbose();
								break;
							case "--verbose":
								serverJarRet.setIsVerbose();
								break;
							default:
								if(args.length != 3){
									usage();
									return;
								}
								break;
							}
						try{
							System.out.println("-- Main --- Load " + serverJarRet.jobs.addFilesFromPath(Paths.get(serverParam.getDefaultInputPath())) + " files");
							System.out.println("-- Main --- Add " + serverJarRet.jobs.loadJobsFromAllPaths() + " jobs");
							serverJarRet.serve();
						}catch(IOException ioe){
							serverJarRet.writeLogErrorAndDisplayIfVerbose(ioe.toString());
							serverSocketChannel.close();
							return;
						}
					}
				}catch(JSONException e){
					System.err.println(e);
					return;
				}
			} else { System.out.println("File isn't in a valid JSON format for configuration server "); }

		}catch(IOException ioe){
			System.err.println(ioe);
			return;
		}
	}
}
