import java.io.*;
import java.util.*;

public class TextCategorizationPreprocess {
	private String outputfile = "output.txt";
	private String directory = "./data/text_categorization/reuters21578";
	private int numOfDocuments = 21578;
	private int numOfTopics = 120;
	private String[] document = new String[numOfDocuments];
	private String[][] topic = new String[numOfDocuments][]; //There are empty topics in this array, they shouldn't be counted in the program
	private Map<String, Integer> topicIndex = new HashMap<String, Integer>();
	private int topicNum = 0;
	private int[] topicCnt = new int[numOfTopics];
	private String[] topicSet = new String[numOfTopics];
	private String[] selectedTopic = {"grain", "wheat", "corn", "veg-oil", "soybean", "oilseed", "coffee", "sugar", "livestock"};
	
	public void readData() throws IOException {
		int docCnt = 0;
		System.out.println("Reading Data from Files");
		for (int i = 0; i < 22; i++) {
			String filename = directory + "/reut2-0" + (i < 10 ? "0" : "") + Integer.toString(i) + ".sgm";
			Scanner reader = new Scanner(new FileReader(filename));
			StringBuffer buffer = new StringBuffer();
			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				if (line.equals("</REUTERS>")) {
					document[docCnt ++] = buffer.toString();
					buffer = new StringBuffer();
				} else {
					buffer.append(line + "\n");
				}
			}
			reader.close();
		}
		
		for (int i = 0; i < numOfDocuments; i++) {
			int topicStart = document[i].indexOf("<TOPICS>");
			int topicEnd = document[i].indexOf("</TOPICS>");
			String topicString = document[i].substring(topicStart + "<TOPICS>".length(), topicEnd);
			topic[i] = topicString.split("</?D>");
		}
	}
	
	public void outputTopicCount() {
		for (int i = 0; i < numOfDocuments; i++) 
			for (int j = 0; j < topic[i].length; j++)
				if (topic[i][j].length() > 1) {
					if (!topicIndex.containsKey(topic[i][j])) {
						topicIndex.put(topic[i][j], topicNum);
						topicSet[topicNum ++] = topic[i][j];
					}
					topicCnt[topicIndex.get(topic[i][j])] ++;
				}
		System.out.println(topicNum);
		for (int i = 0; i < topicNum; i++)
		if (topicCnt[i] > 100)
			System.out.println(topicSet[i] + "," + topicCnt[i]);
	}
	
	public void generateData() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputfile));
		for (int i = 0; i < numOfDocuments; i++) {
			boolean relevant = false;
			for (int j = 0; j < topic[i].length; j++)
				for (int k = 0; k < selectedTopic.length; k++)
					if (topic[i][j].equals(selectedTopic[k]))
						relevant = true;
			if (relevant) {
				int bodyStart = document[i].indexOf("<BODY>");
				int bodyEnd = document[i].indexOf("</BODY>");
				
				if (bodyStart == -1) continue;
	
				String fetchedBody = document[i].substring(bodyStart + "<BODY>".length(), bodyEnd);	
				writer.write("<NewDocument>\n");
				for (int j = 0; j < topic[i].length; j++)
					if (topic[i][j].length() > 0)
						writer.write(topic[i][j] + ";");
				writer.write("\n");
				int truncate = fetchedBody.toLowerCase().indexOf("reuter", fetchedBody.length() - 15);
				if (truncate != -1)
					writer.write(fetchedBody.substring(0, truncate));
				else
					writer.write(fetchedBody);
				writer.write("\n</NewDocument>\n");
			}
		}
		writer.close();
	}
	
	public static void main(String args[]) throws IOException {
		TextCategorizationPreprocess main = new TextCategorizationPreprocess();
		main.readData();
		main.outputTopicCount();
		main.generateData();
	}
}
