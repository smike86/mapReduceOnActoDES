package it.unipr.aotlab.mapreduce.context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Scarpenti 
 * @author Viti 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MapContext implements Context {

	private final int bufferedContextSize;
	private List<Entry> bufferedList;
	private long actualSize;
	private int fileIdx = 0;
	private final String tmpPath;

	/**
	 * Buffered File Context that implements {@link Context}, save data in files using 
	 * a memory buffer for initial partial ordering.
	 * This class write a file every time buffer is full and when close Context. 
	 * @param tmpPath directory for temporary files
	 * @param bufferedContextSize buffer dimensions
	 */
	public MapContext(String tmpPath, int bufferedContextSize) {
		this.bufferedContextSize = bufferedContextSize;
		this.tmpPath = tmpPath;
		bufferedList = new ArrayList<>();
		fileIdx = 0;
	}

	/**
	 * {@inheritDoc}
	 * Build a structure key - list of values creating a group of key-value pair
	 * and create by this group a key - list of values association. 
	 */
	@Override
	public synchronized void put(Object key, Object value) {
		Entry newEntry = new MyEntry(key, value);
		actualSize += key.toString().length() + value.toString().length() + 1;
		bufferedList.add(newEntry);
		if (actualSize >= bufferedContextSize) {
			writeInFile();
			actualSize = 0;
			bufferedList.clear();
		}
	}

	/**
	 * Write in file actual buffer
	 */
	private synchronized void writeInFile() {
		try {
			// create new Context from mapContext where values are grouped by
			// key
			TreeMap<String, List> mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			for (Entry entry : this.bufferedList) {
				if (mappa.containsKey(entry.getKey())) {
					mappa.get(entry.getKey()).add(entry.getValue());
				} else {
					List tmpList = new ArrayList<>();
					tmpList.add(entry.getValue());
					mappa.put((String) entry.getKey(), tmpList);
				}
			}
			File file = new File(tmpPath + "tmp" + fileIdx++ + ".txt");
			file.getParentFile().mkdirs();
			if (!file.exists())
				file.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			for (Entry<String, List> entry : mappa.entrySet()) {
				bw.write(entry.getKey() + " ");
				List values = entry.getValue();
				for (Object object : values) {
					bw.write(object + " ");
				}
				bw.write("\n");
			}
			bw.close();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void putMap(Map map) {
		Set set = map.entrySet();
		for (Object object : set) {
			Entry entry = ((Entry) object);
			put(entry.getKey(), entry.getValue());
		}

	}

	@Override
	public String toString() {
		return String.format("{ dim:%d, %s}", actualSize, bufferedList.toString());
	}

	@Override
	public void close() throws Exception {

	}

	/**
	 * Make the sort operation with all temp files generated by this Context and
	 * create a final sorted file with the "sort operation" after the "map
	 * operation"
	 */
	public void sortAll() {
		writeInFile();
		actualSize = 0;
		bufferedList.clear();

		BufferedWriter bw = null;
		try {
			File dir = new File(tmpPath);
			File[] files = dir.listFiles();
			ArrayList<HeadFile> fileHeads = initAllReaders(files);
			File sortedFile = new File(dir, "sorted/tmp.txt");
			sortedFile.getParentFile().mkdir();
			bw = new BufferedWriter(new FileWriter(sortedFile));
			while (otherLines(fileHeads)) {
				String lower = null;
				HeadFile pointer = null;
				// foreach headline select minor and memorize associated
				// HeadFile
				int index = 0;
				for (HeadFile headFile : fileHeads) {

					if (headFile.fileNotEnded()) {
						String line = headFile.getLastReadedLine().split(" ")[0];
						if (lower == null || String.CASE_INSENSITIVE_ORDER.compare(line, lower) < 0) {
							lower = line;
							pointer = headFile;
						}
						index++;
					}
				}
				writeSorted(bw, pointer.getLastReadedLine());
				pointer.nextLine();

			}
			writeLastValue(bw);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Write last Value of final sorted file (before close Stream). 
	 * This is necessary because the last key in memory is not writted yet
	 * @param bw
	 */
	private void writeLastValue(BufferedWriter bw) {
		try {
			bw.write(lastReadedKey + " " + lastReadedKeyValues);
			bw.write("\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String lastReadedKey = null;
	private String lastReadedKeyValues = "";
	
	/**
	 * write a line in final sorted file if the key (first word) is different from the latest.
	 * @param bw
	 * @param lastReadedLine
	 */

	private void writeSorted(BufferedWriter bw, String lastReadedLine) {
		try {
			String[] values = lastReadedLine.split(" ", 2);
			if (lastReadedKey == null)
				lastReadedKey = values[0];

			if (String.CASE_INSENSITIVE_ORDER.compare(values[0], lastReadedKey) != 0) {
				bw.write(lastReadedKey + " " + lastReadedKeyValues);
				bw.write("\n");
				lastReadedKey = values[0];
				lastReadedKeyValues = values[1];
			} else {
				lastReadedKeyValues += values[1];
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Check presence of other Lines in all files
	 * 
	 * @param fileHeads
	 * @return
	 */
	private boolean otherLines(ArrayList<HeadFile> fileHeads) {
		for (HeadFile headFile : fileHeads) {
			if (!headFile.endOfFile)
				return true;
		}
		return false;
	}
	
	/**
	 * Initialize Reader on each temp file for final sorting inside {@link MapContext#sortAll()}
	 * @param files
	 * @return
	 */

	private ArrayList<HeadFile> initAllReaders(File[] files) {
		ArrayList<HeadFile> fileHeads;
		// init Reader on all files
		fileHeads = new ArrayList<>();
		for (int i = 0; i < files.length; i++) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(files[i]));
				HeadFile headFile = new HeadFile(reader);
				fileHeads.add(headFile);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return fileHeads;
	}
	
	

	private class HeadFile {
		private BufferedReader reader;
		private String lastReadedLine;
		public boolean endOfFile = false;

		/**
		 * Represents reader on temp file, wich mantains information about last readed line and reaching the end of file.
		 * @param reader
		 */
		public HeadFile(BufferedReader reader) {
			this.reader = reader;
			nextLine();
		}

		/**
		 * return {@code true} when there is no more line to read
		 * @return
		 */
		public boolean fileNotEnded() {
			return !endOfFile;
		}

		/**
		 * @return the current head line of file
		 */
		public String getLastReadedLine() {
			return lastReadedLine;
		}

		/**
		 * move Head pointer to next Line
		 */
		public void nextLine() {
			try {
				lastReadedLine = reader.readLine();
				endOfFile = (lastReadedLine == null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private class MyEntry implements Entry {
		Object key;
		Object value;

		/**
		 * Generic Entry for ordering
		 * @param key
		 * @param value
		 */
		public MyEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public Object getKey() {
			return this.key;
		}

		@Override
		public Object getValue() {
			return this.value;
		}

		@Override
		public Object setValue(Object value) {
			return this.value = value;
		}

		@Override
		public String toString() {
			return key + "-" + value;
		}

	}

}
