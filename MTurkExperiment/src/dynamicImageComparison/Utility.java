package dynamicImageComparison;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Utility {
	public static int getNumOfRightAnswers(Map<Integer, Integer> entityMapping, String answer, List<List<Integer>> pairs) {
		String[] split = answer.split("</?SelectionIdentifier>");
		String[] ret = new String[split.length / 2];
		for (int i = 1; i < split.length; i += 2)
			ret[i / 2] = split[i];
		int rightCount = 0;
		for (int i = 0; i < ret.length; i++) {
			boolean answerYes = (ret[i].charAt(0) == '1');
			boolean actualYes = (entityMapping.get(pairs.get(i).get(0)) == entityMapping.get(pairs.get(i).get(1)));
			if (answerYes == actualYes) rightCount ++;
		}
		return rightCount;
	}
	
	public static void expandQuestion(String templateFile, String outputFile, int repeatNum) {
		try {
			Scanner scanner = new Scanner(new FileReader(templateFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			StringBuffer mainQuestion = new StringBuffer();
			boolean write = false;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.equals("===PREFIX==="))
					write = true;
				else if (line.equals("===QUESTION==="))
					write = false;
				else if (line.equals("===SUFFIX===")) {
					for (int i = 0; i < repeatNum; i++)
						writer.write(mainQuestion.toString().replaceAll("\\$id", String.valueOf(i)));
					write = true;
				} else if (write)
					writer.write(line.replaceAll("\\$num", String.valueOf(repeatNum)) + "\n");
				else
					mainQuestion.append(line + "\n");
			}
			writer.close();
			scanner.close();
		} catch (IOException e) {
			Logger.log(e.getMessage());
			e.printStackTrace();
		}		
	}
	
	public static String getAnnotation(List<List<Integer>> pairs) {
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < pairs.size(); i++) {
			ret.append((char)(pairs.get(i).get(0) + 'A'));
			ret.append((char)(pairs.get(i).get(1) + 'A'));
		}
		return ret.toString();
	}
	
	public static List<List<Integer>> getPairs(String annotation) {
		List<List<Integer>> ret = new ArrayList<List<Integer>>();
		for (int i = 0; i < annotation.length(); i += 2) {
			List<Integer> pair = new ArrayList<Integer>();
			pair.add(annotation.charAt(i) - 'A');
			pair.add(annotation.charAt(i + 1) - 'A');
			ret.add(pair);
		}
		return ret;
	}
	
	public static List<List<Integer>> setExclude(List<List<Integer>> source, List<List<Integer>> exclude, int maxElement) {
		List<List<Integer>> ret = new ArrayList<List<Integer>>();
		HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
		for (List<Integer> item: source) {
			int key = item.get(0) * maxElement + item.get(1);
			if (!result.containsKey(key))
				result.put(key, 0);
			result.put(key, result.get(key) + 1);
		}
		for (List<Integer> item: exclude) {
			int key = item.get(0) * maxElement + item.get(1);
			if (!result.containsKey(key))
				result.put(key, 0);
			result.put(key, result.get(key) - 1);
		}
		for (int i: result.keySet()) {
			for (int j = 0; j < result.get(i); j++) {
				List<Integer> pair = new ArrayList<Integer>();
				pair.add(i / maxElement); pair.add(i % maxElement);
				ret.add(pair);
			}
		}
		return ret;
	}
}
