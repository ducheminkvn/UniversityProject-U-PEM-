package upem.jarret.utils;

/**
 * 
 * @author Duchemin Kevin
 * @author Le Gourrierec Maugan
 */


public class ConvertTimeUnit{

	/**
	 * Convert a number of seconds to milliseconds
	 * @param seconds
	 * @return seconds in milliseconds
	 */
	public static long secondsToMillis(int seconds){ return seconds * 1000; }
	
	/**
	 * Convert a number of minutes to milliseconds
	 * @param minutes
	 * @return minutes to milliseconds
	 */
	public static long minutesToMillis(int minutes){ return secondsToMillis(minutes) * 60; }
	
	/**
	 * Convert a number of hours to milliseconds
	 * @param hours
	 * @return hours in milliseconds
	 */
	public static long hoursToMillis(int hours){ return minutesToMillis(hours) * 60; }
	
	/**
	 * Convert a number of days to milliseconds
	 * @param nbDays
	 * @return nbDays in milliseconds
	 */
	public static long daysToMillis(int nbDays){ return hoursToMillis(nbDays) * 24; }

}
