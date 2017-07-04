package upem.jarret.server.http;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */


public class HTTPReaderServer {
	
	private final ByteBuffer buff;
	private HeaderClientToServer header;
	
	/**
	 * Create a HTTPReaderServer who read in the ByteBuffer passed in parameter
	 * @param ByteBuffer
	 */
	public HTTPReaderServer(ByteBuffer buff) { this.buff = buff; }

	/**
	 * @return The ASCII Optional<string> terminated by CRLF or Optional.empty if data in ByteBuffer isn't sufficient
	 * <p>
	 * The method assume that buff is in write mode and leave it in write-mode
	 * The method never reads from the socket
	 * @throws  
	 * @throws IOException HTTPException if the connection is closed before a line could be read
	 */
	public Optional<String> readLineCRLFFromBuffer(){
		StringBuilder sb = new StringBuilder();
		boolean cr = false;
		boolean lf = false;

		buff.flip();
		while(buff.hasRemaining() && !lf){
			byte character = buff.get();
			if(cr)
				if(character == '\n')
					lf = true;
				else 
					cr = false;
			else
				if(character == '\r')
					cr = true;
			sb.append((char)character);
		}	
		if(!lf){
			buff.position(0);
			buff.compact();
			return Optional.empty();
		}
		buff.compact();
		return Optional.ofNullable(sb.substring(0,sb.length()-2).toString());
	}

	/**
	 * Read the HeaderClientToServer from ByteBuffer registry in HTTPReaderServer
	 * @return Optional<HeaderClientToServer> or Optional.empty if data in ByteBuffer isn't sufficient
	 */
	public Optional<HeaderClientToServer> readHeader(){
		
		if(header == null || header.isComplete()){
			Optional<String> firstLine = readLineCRLFFromBuffer();
			if(!firstLine.isPresent()){
				return Optional.empty();
			}
			header = new HeaderClientToServer(firstLine.get());
		}
		Optional<String> line = readLineCRLFFromBuffer();
		while(line.isPresent() && !line.get().equals("")){
			String[] tokens = line.get().split(":",2);
			header.addValue(tokens[0], tokens[1]);	
			line = readLineCRLFFromBuffer();
		}
		if(line.isPresent() && line.get().equals("")){
			header.setIsComplete();
			return Optional.ofNullable(header);
		}
		return Optional.empty();
	}
	
	/**
	 * Read from ByteBuffer registry in HTTPReaderServer size byte to a new Bytebuffer
	 * @param size
	 * @return Optional<ByteBuffer> or Optional.empty if the data from ByteBuffer registry is inferior to size
	 */
	public Optional<ByteBuffer> readBytes(int size){
		
		buff.flip();
		ByteBuffer bb;
		if(buff.remaining() >= size){
			bb = ByteBuffer.allocate(size);
			int oldLimit = buff.limit();
			buff.limit(buff.position() + size);
			bb.put(buff);
			buff.limit(oldLimit);
			buff.compact();
			return Optional.ofNullable(bb);
		} 
		buff.compact();
		return Optional.empty();
	}
}
