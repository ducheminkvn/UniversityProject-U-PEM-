package upem.readers;

public interface Reader<T> {
	
	public ProcessState process();
	
	public T get();
	
	public void reset();

}