package upem.net.tcp.nonblocking;


public class testenum {

	private enum OpCode {	
		ASK_CONNEXION((byte)0),
		ASK_ALIAS((byte)1),
		DECLINE_CONNEXION((byte)3);

		private byte code;

		OpCode(byte code){
			this.code = code;
		}
		
		public byte getCode(){
			return code;
		}
		
		public static OpCode newOpCode(byte val){
			
			switch(val){
				case 0:
					return OpCode.ASK_ALIAS;
					
				case 1:
					return OpCode.ASK_CONNEXION;
					
				case 2:
					return OpCode.DECLINE_CONNEXION;
					
				default:
					throw new IllegalArgumentException();
			}
			
		}
	};



	public static void main(String[] args){

		byte opCode = 1;
		
		System.out.println(OpCode.values().toString());
		switch(OpCode.newOpCode(opCode)){

		case ASK_CONNEXION:
			System.out.println("connection");
			break;
		case ASK_ALIAS:
			System.out.println("alias");
			break;
		case DECLINE_CONNEXION:
			System.out.println("decline");
			break;
	


		}
		
	}
}
