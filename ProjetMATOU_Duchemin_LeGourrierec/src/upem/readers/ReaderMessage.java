package upem.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ReaderMessage extends BufferReader<String>{

	
	private static final Charset utf8 = Charset.forName("UTF-8");
	private int size=0;
	
	public ReaderMessage(ByteBuffer bb) {
		super(bb);
	}
	
	public void setSize(int size){
		this.size = size;
	}

	@Override
	int nbTypeBytes() {
		return size;
	}

	@Override
	String getData(ByteBuffer bb) {
		return utf8.decode(bb).toString();
	}

	@Override
	boolean testValue(String value) {
		return value.length()>0 || value.length()<size;
	}

	
}
