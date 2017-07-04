package upem.jarret.client.http;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import upem.jarret.http.HTTPException;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class HTTPReader {

	private final SocketChannel sc;
	private final ByteBuffer buff;

	/**
	 * Create a HTTPReader who read on the SocketChannel in parameter and stock the data in the data reading in the ByteBuffer in parameter
	 * @param sc
	 * @param buff
	 */
	public HTTPReader(SocketChannel sc, ByteBuffer buff){
		this.sc = sc;
		this.buff = buff;
	}

	/**
	 * @return The ASCII string terminated by CRLF
	 * <p>
	 * The method assume that buff is in write mode and leave it in write-mode
	 * The method never reads from the socket as long as the buffer is not empty
	 * @throws  
	 * @throws IOException HTTPException if the connection is closed before a line could be read
	 */
	public String readLineCRLF() throws IOException  {
		StringBuilder sb = new StringBuilder();
		boolean cr = false;
		boolean lf = false;
		
		do{

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

			buff.compact();
			
			if(!lf && buff.hasRemaining()){
				if(-1 == sc.read(buff))
					throw new HTTPException("Input is Closed");
			}

		}while(!lf);


		return sb.substring(0,sb.length()-2).toString();
	}

	/**
	 * @return The HTTPHeader object corresponding to the header read
	 * @throws IOException HTTPException if the connection is closed before a header could be read
	 *                     if the header is ill-formed
	 */
	public HTTPHeader readHeader() throws IOException {
		HashMap<String, String> fields = new HashMap<>();
		String first = readLineCRLF();
		String line = readLineCRLF();

		while(!line.equals("")){
			String[] tokens = line.split(":", 2);
			if(fields.containsKey(tokens[0])){
				String value = fields.get(tokens[0]);
				value = value+";"+tokens[1];
			}
			else
				fields.put(tokens[0], tokens[1]);
			line = readLineCRLF();
		}
		return HTTPHeader.create(first, fields);
	}

	/**
	 * @param size
	 * @return a ByteBuffer in write-mode containing size bytes read on the socket
	 * @throws IOException HTTPException is the connection is closed before all bytes could be read
	 */
	public ByteBuffer readBytes(int size) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(size);
		buff.flip();
		if(buff.remaining() > size){
			int oldLimit = buff.limit();
			buff.limit(buff.position()+size);
			bb.put(buff);
			buff.limit(oldLimit);
		}
		else
			bb.put(buff);
		buff.compact();
		while(bb.hasRemaining()){
			if(-1 == sc.read(bb))
				throw new HTTPException();
		}
		return bb;
	}

	/**
	 * @return a ByteBuffer in write-mode containing a content read in chunks mode
	 * @throws IOException HTTPException if the connection is closed before the end of the chunks
	 *                     if chunks are ill-formed
	 */
	public ByteBuffer readChunks() throws IOException {
		return readBytes(Integer.parseInt(readLineCRLF(), 16)+2);
	}
}
