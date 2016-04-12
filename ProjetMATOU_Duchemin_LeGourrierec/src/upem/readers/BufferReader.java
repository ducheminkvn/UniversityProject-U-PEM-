package upem.readers;

import java.nio.ByteBuffer;

public abstract class BufferReader<T> implements Reader<T> {
	
	private ByteBuffer bb;
	private T data;
	private ProcessState state = ProcessState.REFILL;
	
	BufferReader(ByteBuffer bb){
		this.bb = bb;
	}
	
	@Override
	public ProcessState process(){
		if(state != ProcessState.REFILL){
			throw new IllegalAccessError("you must reset first");
		}
		if(bb.position()>nbTypeBytes()){
			bb.flip();
			int oldlimit = bb.limit();
			bb.limit(bb.position()+nbTypeBytes());
			data = getData(bb);
			bb.limit(oldlimit);
			bb.compact();
			if(testValue(data)){
				state = ProcessState.DONE;
			}
			else{
				state = ProcessState.ERROR;
			}
		}
		
		return state;
	}
	@Override
	public T get() {
		return data;
	}
	
	@Override
	public void reset() {
		data = null;
		state = ProcessState.REFILL;
	}
	
	abstract int nbTypeBytes();
	
	abstract T getData(ByteBuffer bb);
	
	abstract boolean testValue(T value);
}
