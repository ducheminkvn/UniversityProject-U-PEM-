package upem.readers;

import java.nio.ByteBuffer;

public class ReaderLong extends BufferReader<Long>{

	ReaderLong(ByteBuffer bb) {
		super(bb);
	}

	@Override
	int nbTypeBytes() {
		return Long.BYTES;
	}

	@Override
	Long getData(ByteBuffer bb) {
		return bb.getLong();
	}

	@Override
	boolean testValue(Long value) {
		return value>0;
	}

}
