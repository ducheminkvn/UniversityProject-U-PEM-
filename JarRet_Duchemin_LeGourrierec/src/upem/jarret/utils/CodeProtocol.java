package upem.jarret.utils;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */


public class CodeProtocol {

	public	static enum HTTPProtocolCode{
		BAD_REQUEST_CODE(400),
		OK_CODE(200);

		private final int code;
		HTTPProtocolCode(int code){ this.code = code; }
		public  int getCode(){ return code; }
		@Override
		public String toString(){
			return ""+code;
		}
	}

	public	static enum AnswerKind{
		ANSWER("Answer"),
		ERROR("Error");

		private final String state;
		AnswerKind(String value){ this.state = value; }
		@Override
		public String toString(){ return state; }
	}

	public	static enum Error{
		ERROR_COMPUTATION("Computation error"),
		ERROR_ANSWER_NOT_VALID_JSON("Answer is not valid JSON"),
		ERROR_ANSWER_NESTED("Answer is nested"),
		ERROR_ANSWER_TOO_LONG("Too Long");

		private final String type;
		Error(String value){ this.type = value; }
		@Override
		public String toString(){ return type; }
	}

}