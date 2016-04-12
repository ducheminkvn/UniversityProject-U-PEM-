package upem.net.tcp.nonblocking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

class Attachment {

	ByteBuffer in;
	ByteBuffer out;

	boolean isClosed;
	Queue<String> message;
	
	private final SocketChannel sc;
	
	public Attachment(SocketChannel sc){
		this.sc = sc;
	}
	
	public int updateInterestOps(){
		int interest = 0;
		
		if(in.hasRemaining() && !isClosed){
			interest = interest | SelectionKey.OP_READ;
		}
		if(in.position() != 0){
			interest = interest | SelectionKey.OP_WRITE;
		}
		
		return interest;
	}
	
	public int doRead() throws IOException{
		
		in.flip();
		if(sc.read(in)==-1){
			if(in.position() == 0){
				silentlyClose();
			}
		}
		
		//reader.read(in);
		
		return updateInterestOps();
	}
	
	public int doWrite() throws IOException{
		
		message.poll();
		
		sc.write(out);
		
		return updateInterestOps();
	}
	
	public String getMessage(){
		return message.poll();
	}
	private void silentlyClose() {
		if (sc != null) {
			try {
				sc.close();
			} catch (IOException e) {
				
			}
		}
	}
}
