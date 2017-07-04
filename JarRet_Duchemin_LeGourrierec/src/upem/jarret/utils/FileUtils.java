package upem.jarret.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */

public class FileUtils {

	/**
	 * List the path contains in the path passed in parameter
	 * @param path
	 * @return List<Path> 
	 * @throws IOException
	 */
	public static List<Path> filesFromDirectory(Path path) throws IOException{
		List<Path> list = new ArrayList<>();
		try(Stream<Path> paths = Files.walk(path)) {
			paths.forEach(filePath -> {
				if (Files.isRegularFile(filePath)) {
					if(!list.contains(filePath))
						list.add(filePath);
				}
			});
		}
		return list;
	}

	/**
	 * Create a Directory from String's path passed in parameter if he not already exist
	 * @param path
	 * @return false the directory does'nt exist and can't be created, true otherwise
	 */
	public static boolean createDirectoryIfNotExist(String path){
		File dir;
		StringBuilder sb = new StringBuilder().append(path.split("/",2)[0]);
		for(String dirName : path.split("/",2)[1].split("/")){
			sb.append("/"+dirName);
			dir = new File(sb.toString());
			if(!dir.exists()){
				if(!dir.mkdir()){
					System.err.println("Directory : " + dir +" - can't be created");
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Open the file and write the text before close this file
	 * @param pathDir
	 * @param fileName
	 * @param text
	 * @throws IOException
	 */
	public static void openAndWriteFile(String pathDir, String fileName, String text) throws IOException{

		pathDir = validDirPathName(pathDir);
		if(! createDirectoryIfNotExist(pathDir))
			return;
		String path = pathDir + fileName;

		try {
			FileWriter fw = new FileWriter(path,true);
			fw.write(text);
			fw.close();
		}
		catch(IOException ioe) { System.err.println("IOException: " + ioe.getMessage()); }
	}

	/**
	 *  Open the file and write the text and truncate the file if he is more long than the parameter maxSize before close this file
	 * @param pathDir
	 * @param fileName
	 * @param text
	 * @param maxSize
	 * @throws IOException
	 */
	public static void openAndWriteFile(String pathDir, String fileName, String text, long maxSize) throws IOException{

		pathDir = validDirPathName(pathDir);
		if(! createDirectoryIfNotExist(pathDir))
			return;
		String path = pathDir + fileName;

		File file = null;
		try { 
			file = new File(path);
			file.createNewFile();
		} catch (IOException e) {  throw new IOException("File " + path + " can't be created"); } 

		FileOutputStream streamFile = null; 
		try { 
			streamFile	= new FileOutputStream(file);
			long oldSize = streamFile.getChannel().size();
			byte[] bytes = text.getBytes(); 

			if(oldSize + bytes.length > maxSize)
				for(int i = 0; i < maxSize - oldSize; i++ )
					streamFile.write(bytes[i]);
			else
				streamFile.write(text.getBytes());
			streamFile.close(); 	
			return;
		} catch (FileNotFoundException e1) { throw new FileNotFoundException("File " + path + " not found"); } 
	}

	/**
	 * format correctly the format for a directory path
	 * @param path
	 * @return String with a correct format for a directory path
	 */
	public static String validDirPathName(String path){

		String finalPath = Objects.requireNonNull(path);
		if(!Paths.get(path).isAbsolute() && !path.startsWith("./") && !path.startsWith("../") && !path.startsWith("~"))
			finalPath = "../" + path;
		if(!path.endsWith("/"))
			finalPath = finalPath + "/" ;
		if(finalPath.endsWith("//"))
			throw new IllegalArgumentException("Directory Path : " + path + " - No Valid!");
		return finalPath;
	}
}
