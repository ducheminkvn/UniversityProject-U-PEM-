package upem.readers;

import java.nio.ByteBuffer;

public class ReaderByte extends BufferReader<Byte>{

	private byte[] valuesAllowed = {0,1,2,3,4,5,6,7};
	
	ReaderByte(ByteBuffer bb) {
		super(bb);
	}

	@Override
	int nbTypeBytes() {
		return Byte.BYTES;
	}

	@Override
	Byte getData(ByteBuffer bb) {
		return bb.get();
	}

	@Override
	boolean testValue(Byte value) {
		
		for(byte b:valuesAllowed){
			if(value == b){
				return true;
			}
		}
		
		return false;
	}

}
