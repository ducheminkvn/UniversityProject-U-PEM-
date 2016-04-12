package upem.readerspackage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

import upem.readers.ProcessState;
import upem.readers.Reader;
import upem.readers.ReaderInt;
import upem.readers.ReaderLong;
import upem.readers.ReaderMessage;


public class ReaderBroadcastClient implements Reader<HashMap<InetSocketAddress,String>> {

	private ByteBuffer bb;

	ReaderInt ri;
	String str;
	InetSocketAddress addr;
	HashMap<InetSocketAddress,String> res;
	int rawPosition = bb.position();
	int rawLimit = bb.limit();


	ReaderBroadcastClient(ByteBuffer bb){
		this.bb = bb;
		ri = new ReaderInt(bb);
	}

	@Override
	public ProcessState process() {
		ReaderMessage rm;
		int sizeAddress;
		int sizeMsg;
		rawPosition = bb.position();
		rawLimit = bb.limit();

		bb.flip();
		if(bb.remaining() < Integer.BYTES)
			return ProcessState.REFILL;

		sizeAddress = ri.get();
		if(sizeAddress != 4 && sizeAddress != 6){
			return ProcessState.ERROR;
		}

		if(bb.remaining() < sizeAddress){
			bb.position(rawPosition);
			bb.limit(rawPosition + Integer.BYTES);

			bb.putInt(sizeAddress);

			reset();
			return ProcessState.REFILL;
		}
		if(sizeAddress == 4){
			int values[];
			for(int i=0;i<4;i++)
				values[i] = ri.get();
			addr = new InetSocketAddress(arg0);


			if(bb.remaining() < Integer.BYTES){
				bb.position(rawPosition);
				bb.limit(rawPosition + Integer.BYTES*5);

				bb.putInt(sizeAddress);
				for(int i=0;i<4;i++)
					bb.putInt(values[i]);

				reset();
				return ProcessState.REFILL;
			}
			sizeMsg = ri.get();

			if(bb.remaining() < sizeMsg){
				bb.position(rawPosition);
				bb.limit(rawPosition + Integer.BYTES*6);

				bb.putInt(sizeAddress);
				for(int i=0;i<4;i++)
					bb.putInt(values[i]);

				bb.putInt(sizeMsg);

				reset();
				return ProcessState.REFILL;
			}

		}else{
			ReaderLong rl = new ReaderLong(bb);
			long values[];
			for(int i=0;i<4;i++)
				values[i] = rl.get();
			addr = new InetSocketAddress(arg0);


			if(bb.remaining() < Integer.BYTES){
				bb.position(rawPosition);
				bb.limit(rawPosition + Integer.BYTES + Long.BYTES*4);

				bb.putInt(sizeAddress);
				for(int i=0;i<4;i++)
					bb.putLong(values[i]);

				reset();
				return ProcessState.REFILL;
			}

			sizeMsg = ri.get();

			if(bb.remaining() < sizeMsg){
				bb.position(rawPosition);
				bb.limit(rawPosition + Integer.BYTES*2 + Long.BYTES*4);

				bb.putInt(sizeAddress);
				for(int i=0;i<4;i++)
					bb.putLong(values[i]);

				bb.putInt(sizeMsg);

				reset();
				return ProcessState.REFILL;
			}

		}

		rm.setSize(sizeMsg);
		str = rm.get();
		bb.compact();
		if(str==null)
			return ProcessState.ERROR;	
		return ProcessState.DONE;
	}

	@Override
	public HashMap<InetSocketAddress,String> get() {
		res = new HashMap<InetSocketAddress,String>();
		res.put(addr, str);
		return res;
	}

	@Override
	public void reset() {
		bb.position(rawPosition);
		bb.limit(rawLimit);
	}

}
