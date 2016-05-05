package dynamicImageComparison;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class Logger {
	private static Logger instance = null;
	private BufferedWriter writer;
	
	private Logger() {
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("PST"), Locale.US);		
		String filename = String.format("log %02d-%02d-%02d_%02d_%02d.txt", time.get(Calendar.MONTH) + 1, time.get(Calendar.DATE), time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.SECOND));
		try {
			writer = new BufferedWriter(new FileWriter(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String getPrefix() {
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("PST"), Locale.US);
		return String.format("%d:%02d:%02d", time.get(Calendar.HOUR_OF_DAY),
				time.get(Calendar.MINUTE), time.get(Calendar.SECOND));
	}
	
	public static void close() {
		if (instance != null) {
			try {
				instance.writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			instance = null;
		}
	}
	
	public static void open() {
		Logger.close();
		instance = new Logger();
	}
	
	public static void log(String log) {
		if (instance == null)
			Logger.open();
		
		try {
			instance.writer.write(instance.getPrefix() + " " + log);
			instance.writer.newLine();
			System.out.println(log);
			instance.writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		};
	}
}
