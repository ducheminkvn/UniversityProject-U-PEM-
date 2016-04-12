package upem.readerspackage;

import java.nio.ByteBuffer;

import upem.readers.ProcessState;
import upem.readers.Reader;
import upem.readers.ReaderInt;
import upem.readers.ReaderMessage;


public class ReaderPrivateMessage implements Reader<String> {

	private ByteBuffer bb;

	private ReaderInt ri;
	private String res;

	private ReaderMessage rm;
	
	private int rawPosition = bb.position();
	private int rawLimit = bb.limit();

	ReaderPrivateMessage(ByteBuffer bb){
		this.bb = bb;
		ri = new ReaderInt(bb);
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
