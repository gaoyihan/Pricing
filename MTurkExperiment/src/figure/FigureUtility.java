package figure;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import dynamicImageComparison.*;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;

class LabelUtility {
	private Map<String, List<List<Integer>>> LabelMapper;
	private void ReadLabelMapper() throws IOException {
		Scanner scanner = new Scanner(new FileReader("outputYN.txt"));
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] split = line.split(",");
		}
		scanner.close();
	}
	public List<List<Integer>> getPairs() {
		if (LabelMapper == null) {
			try {
				ReadLabelMapper();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}

public class FigureUtility {
	static final boolean Generate = true;
	static final boolean Prepare = false;
	static final boolean GenerateDynamic = false;
	static final int hitIdLength = "3P4C70TRMRSLOG7X0H1H1R1C9CBLGA".length();

	private static void calendarUtility() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("PST"));
		calendar.set(Calendar.DAY_OF_MONTH, 12);
		calendar.set(Calendar.MONTH, Calendar.JUNE);
		calendar.set(Calendar.HOUR_OF_DAY, 8);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		System.out.println("Date " + (calendar.get(Calendar.MONTH) + 1) + "."
				+ calendar.get(Calendar.DAY_OF_MONTH)
				+ " , Time in Millisecond: " + calendar.getTimeInMillis());
	}

	private static void prepareURL(String rawLogFile) throws IOException {
		Scanner scanner = new Scanner(new FileReader(rawLogFile));
		Logger.log("Reading" + rawLogFile);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.length() == 14) {
				String acceptTime = scanner.nextLine();
				String approveTime = scanner.nextLine();
				Calendar approveCalendar = Calendar.getInstance();
				approveCalendar.setTimeInMillis(Long.parseLong(approveTime));
				URL url = new URL(
						"https://requester.mturk.com/mturk/workerpaymenttransactions?date="
								+ (approveCalendar.get(Calendar.MONTH) + 1)
								+ "%2F"
								+ String.format("%02d", approveCalendar
										.get(Calendar.DAY_OF_MONTH)) + "%2F"
								+ "2014&workerId=" + line);
				Logger.log("Getting URL Content:\n" + url.toString());
			}
		}
	}

	private static void prepareLog(String rawLogFile, String outputFile)
			throws IOException {
		Scanner scanner = new Scanner(new FileReader(rawLogFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		RequesterService service = new RequesterService(
				new PropertiesClientConfig("./mturk.properties"));
		Map<Integer, Integer> entityMapping = DynamicImageComparison
				.getEntityMapping();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			ArrayList<String> hitIdList = new ArrayList<String>();
			if (GenerateDynamic) {
				if (line.contains("Created HIT:")) {
					hitIdList.add(line.substring(line.length() - hitIdLength,
							line.length()));
				}
			} else {
				if (line.contains("Created HIT:")) {
					hitIdList.add(line.substring(line.length() - hitIdLength,
							line.length()));
				}
			}
			if (hitIdList.size() > 0) {
				for (String hitId : hitIdList) {
					HIT hit = service.getHIT(hitId);
					Assignment[] assignment = service
							.getAllAssignmentsForHIT(hitId);
					if (assignment.length > 0) {
						List<List<Integer>> pairs = Utility.getPairs(hit
								.getRequesterAnnotation());
						int rightAnswers = Utility
								.getNumOfRightAnswers(entityMapping,
										assignment[0].getAnswer(), pairs);
						writer.write(assignment[0].getWorkerId() + "\n");
						writer.write(assignment[0].getAcceptTime()
								.getTimeInMillis() + "\n");
						writer.write(pairs.size() + "\n");
						writer.write(rightAnswers + "\n");
						writer.write(assignment[0].getSubmitTime()
								.getTimeInMillis() + "\n");
					}
				}
			}
		}
		scanner.close();
		writer.close();
	}

	public static void main(String[] args) {
		if (Generate) {
			try {
				if (GenerateDynamic) {
					prepareLog("dyn_price_log_1.txt",
							"log_group_dyn_002_01.txt");
					prepareLog("dyn_price_log_2.txt",
							"log_group_dyn_002_02.txt");
					prepareLog("dyn_price_log_3.txt",
							"log_group_dyn_002_03.txt");
					prepareLog("dyn_price_log_4.txt",
							"log_group_dyn_002_04.txt");
					prepareLog("dyn_price_log_5.txt",
							"log_group_dyn_002_05.txt");
				} else {
					prepareLog("new_fix_price_log_1.txt",
							"log_group_fix_002_1.txt");
					prepareLog("new_fix_price_log_2.txt",
							"log_group_fix_002_2.txt");
					prepareLog("new_fix_price_log_3.txt",
							"log_group_fix_002_3.txt");
					prepareLog("new_fix_price_log_4.txt",
							"log_group_fix_002_4.txt");
					prepareLog("new_fix_price_log_5.txt",
							"log_group_fix_002_5.txt");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (Prepare) {
			try {
				prepareURL("log_group_10_002_full.txt");
				prepareURL("log_group_20_002_full.txt");
				prepareURL("log_group_30_002.txt");
				prepareURL("log_group_40_002.txt");
				prepareURL("log_group_50_002.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			calendarUtility();
		}

	}
}
