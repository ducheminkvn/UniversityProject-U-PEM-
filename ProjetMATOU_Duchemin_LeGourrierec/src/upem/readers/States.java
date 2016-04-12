package upem.readers;

public interface States {

	public enum ProcessState {
		REFILL,
		DONE,
		ERROR;
	}
	
	public enum private enum FieldsRequest{
		SIZENAME,
		NAME;
	}
}
