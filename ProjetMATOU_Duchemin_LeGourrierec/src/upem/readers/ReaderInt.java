package upem.readers;

import java.nio.ByteBuffer;

public class ReaderInt extends BufferReader<Integer> {

	public ReaderInt(ByteBuffer bb) {
		super(bb);
		
	}

	@Override
	int nbTypeBytes() {
		return Integer.BYTES;
	}

	@Override
	Integer getData(ByteBuffer bb) {
		return bb.getInt();
	}

	@Override
	public boolean testValue(Integer value) {
		return value>0;
	}

}
