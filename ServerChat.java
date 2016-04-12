package upem.net.tcp.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;


public class ServerChat {

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	
	private HashMap<SocketChannel,String> listclient = new HashMap<>();

	public ServerChat(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
	}

	private void doAccept(SelectionKey key) throws IOException {

		SocketChannel sc = serverSocketChannel.accept();
		if(sc == null)
			return;

		sc.configureBlocking(false);
		sc.register(selector,SelectionKey.OP_READ,new Attachment(sc));
		listclient.put(sc,null);
	}
	
	private void processSelectedKeys() throws IOException {

		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			try{
				if (key.isValid() && key.isWritable()) {
					key.interestOps(((Attachment) key.attachment()).doWrite());
				}
				if (key.isValid() && key.isReadable()) {
					key.interestOps(((Attachment) key.attachment()).doRead());
				}
			}catch(IOException io){
				silentlyClose((SocketChannel)key.channel());
			}

		}
	}
	
	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		
		while (!Thread.interrupted()) {
			processSelectedKeys();
			selectedKeys.clear();
		}
	}
	
	private void silentlyClose(SocketChannel sc) {
		if (sc != null) {
			try {
				sc.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}
}
