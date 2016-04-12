package upem.readerspackage;

import java.nio.ByteBuffer;

import upem.readers.ProcessState;
import upem.readers.Reader;
import upem.readers.ReaderInt;
import upem.readers.ReaderMessage;


public class ReaderBroadcastServer implements Reader<String> {
	
	private ByteBuffer bb;
	
	ReaderInt ri;
	String res;
	ReaderMessage rm;
	int rawPosition = bb.position();
	int rawLimit = bb.limit();

	ReaderBroadcastServer(ByteBuffer bb){
		this.bb = bb;
		ri = new ReaderInt(bb);
		rm = new ReaderMessage(bb);
	}

	@Override
	public ProcessState process() {
		
		int size;
		rawPosition = bb.position();
		rawLimit = bb.limit();
		
		
		bb.flip();
		if(bb.remaining() < Integer.BYTES)
			return ProcessState.REFILL;
		size = ri.get();

		if(bb.remaining() < size){
			bb.position(rawPosition);
			bb.limit(rawPosition + Integer.BYTES);
			return ProcessState.REFILL;
		}
		rm.setSize(size);
		res = rm.get();
		bb.compact();
		if(res == null)
			return ProcessState.ERROR;	
		return ProcessState.DONE;
	}
	
	@Override
	public String get() {
		return res;
	}

	@Override
	public void reset() {
		bb.position(rawPosition);
		bb.limit(rawLimit);
	}

}
