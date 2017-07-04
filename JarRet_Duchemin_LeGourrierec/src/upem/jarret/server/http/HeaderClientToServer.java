package upem.jarret.server.http;

import java.util.HashMap;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class HeaderClientToServer{

	private String typeProtocol;//GET ou POST
	private String dataType;//Task ou Answer
	private String protocolVersion;//HTTP/1.1

	private HashMap<String, String> fields = new HashMap<>();
	private boolean isComplete = false;

	/**
	 * Create a HeaderClientToServer
	 * @param firstLine
	 */
	public HeaderClientToServer(String firstLine){
		String[] tokens = firstLine.split(" ");
		typeProtocol = tokens[0];
		dataType = tokens[1];
		protocolVersion = tokens[2];
	}

	/**
	 * Add to HeaderClientToServer the value associated to the field
	 * @param field
	 * @param value
	 * @return true if value has been well added, false otherwise
	 */
	public boolean addValue(String field, String value){
		if(! fields.containsKey(field))
			return value.equals(fields.put(field, value));
		String regitryValue = fields.get(field);
		return (value + ";" + fields.get(field)).equals(fields.put(field ,value + ";" + regitryValue));
	}
	
	/**
	 * Set the flag Complete has true
	 * @return true if flag Complete is true, false otherwise
	 */
	public boolean setIsComplete(){ return isComplete = true; }
	
	/**
	 * Get the flag Complete
	 * @return true if flag Complete is true, false otherwise
	 */
	public boolean isComplete(){ return isComplete; }

	/**
	 * Get the value associated field Content-Length
	 * @return length of content
	 */
	public int getContentLength(){ return Integer.parseInt(fields.getOrDefault("Content-Length", "-1").replaceFirst(" ", "")); }

	/**
	 * Get the host value associated field Host
	 * @return host in String format
	 */
	public String getHost(){ return fields.getOrDefault("Host", "NOT FOUND"); }

	/**
	 * Get the type associated field Content-Type
	 * @return type of content in String format
	 */
	public String getContentType(){ return fields.getOrDefault("Content-Type", "NOT FOUND"); }
	
	/**
	 * Get the type protocol
	 * @return type of protocol in String format
	 */
	public String getTypeProtocol() { return typeProtocol; }

	/**
	 * Get the type of data
	 * @return data type in String Format
	 */
	public String getDataType() { return dataType; }

	/**
	 * Get the protocol version
	 * @return protocol version in String format
	 */
	public String getProtocolVersion() { return protocolVersion; }
	
	@Override
	public String toString(){
		
		StringBuilder sb = new StringBuilder();
		sb.append(typeProtocol + " " + dataType + " " + protocolVersion + "\r\n");
		for(String key : fields.keySet()){
			sb.append(key).append(" : ").append(fields.get(key)).append("\r\n");
		}
		return sb.toString();
	}
	
}




