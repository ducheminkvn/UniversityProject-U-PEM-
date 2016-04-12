package upem.net.tcp.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Client {

	public static Charset utf8 = Charset.forName("UTF-8");
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		ByteBuffer bb = ByteBuffer.allocate(128);
		String msg = "Hello world";
		
		bb.put((byte)0);
		
		ByteBuffer bbmsg = utf8.encode(msg);
		
		bb.putInt(bbmsg.remaining());
		
		bb.put(bbmsg);
		
		SocketChannel sc = SocketChannel.open();
		
		sc.connect(new InetSocketAddress(args[1],Integer.parseInt(args[2])));
		
		for(;;){
		   
			Thread.sleep(300);
			sc.write(bb);
		}
				
	}
}
