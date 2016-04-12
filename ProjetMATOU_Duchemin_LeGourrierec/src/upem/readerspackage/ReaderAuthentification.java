package upem.readerspackage;

import java.nio.ByteBuffer;

import upem.readers.ProcessState;
import upem.readers.Reader;
import upem.readers.ReaderInt;
import upem.readers.ReaderMessage;

public class ReaderAuthentification implements Reader<Data> {

	private enum FieldsRequest{
		SIZENAME,
		NAME;
	}

	private ByteBuffer bb;
	
	ReaderInt ri;
	ReaderMessage rm;
	
	private int sizenick;
	private String nick; 
	private int originPosition;
	private int originLimit;
	
	ReaderAuthentification(ByteBuffer bb){
		this.bb = bb;
		ri = new ReaderInt(bb);
		rm = new ReaderMessage(bb,1);
	}
	
	@Override
	public ProcessState process() {
		originLimit = bb.limit();
		originPosition = bb.position();
		
		ri.process();
		sizenick = ri.get();
		ri.reset();
		rm.setSize(sizenick);
		rm.process();
		nick = rm.get();
		rm.reset();
		
		return ;
	}

	@Override
	public Data get() {

		return new Data(sizenick,nick);
	}

	@Override
	public void reset() {
		bb.limit(originLimit);
		bb.position(originPosition);
	}

}
