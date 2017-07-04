package upem.jarret.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class Parse{

	/**
	 * Extract the data in the file path in param to a String with this data
	 * @param pathFile
	 * @return String with this data contend in the file target by pathFile
	 * @throws IOException
	 */
	public static String parseFileToString(Path pathFile) throws IOException{
		StringBuilder sb = new StringBuilder();
		try(Stream<String> lines = Files.lines(pathFile)){
			lines.forEach(sb::append);
		}
		return sb.toString();
	}
	
	/**
	 * Parse a String in JSON format to a List of String contend one block JSON by String
	 * @param line
	 * @return List of String contend one block JSON by String
	 * @throws IOException
	 */
	public static List<String> parseStringByBlockJSON(String line) throws IOException{
		ArrayList<String> list = new ArrayList<>();
		String[] tokens = line.split("}");
		for(String str : tokens)
			list.add(str+"}");
		return list;
	}
	
}
