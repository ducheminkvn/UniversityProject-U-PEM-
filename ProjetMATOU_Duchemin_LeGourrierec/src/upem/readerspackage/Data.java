package upem.readerspackage;

public class Data {

	private String message;
	private int size;

	public Data(int size,String message){
		this.size = size;
		this.message = message;
	}
	
	public String getMessage(){
		return message;
	}
	
	public int getSize(){
		return size;
	}
}
