package it.unipr.aotlab.mapreduce.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * The class {@code BlockReader} has the purpose to split the inputfile in some
 * blocks and calculate how many blocks are needed to read the inputfile, this
 * is achieved with the definition of fixed length blocks based on total length
 * of file. By the way one block cannot end in the middle of one line, in fact
 * we need to using block that end at the end of a line of file.
 *
 */
public class BlockReader {
	private String inputPath; // folder or file path
	private int totalBlockNumber;
	private int blockSize;
	private List<Integer> startPosition_list;
	protected RandomAccessFile reader;

	/**
	 *
	 * Class Constructor
	 * 
	 * In the constructor there are and Algorithm that calculate the starting
	 * point of each block with this correction: if block with fixed length end
	 * at the middle of one line, the dimension of block was modified adding
	 * some length at the actual dimension of the block that we need for read
	 * completely the line. We did this correction for every block. After this
	 * algorithm may happen this situation: {@code totalBlockNumber} can be less
	 * then at the start of algorithm, so the number of blocks that we used to
	 * perform the lecture of the file in blocks was reduced.
	 * 
	 * @param inputPath
	 *            The inputpath of file that we want to split in blocks
	 * @param blockSize
	 *            The size of fixed length block that we want to split the file
	 * 
	 * @throws IOException
	 * 
	 * 
	 */
	public BlockReader(String inputPath, int blockSize) throws IOException {
		this.inputPath = inputPath;
		this.blockSize = blockSize;
		this.startPosition_list = new ArrayList<Integer>();

		// conteggio blocchi
		int dim_file = (int) new File(inputPath).length();
		int num_blocchi = (dim_file / this.blockSize) + 1;
		this.totalBlockNumber = num_blocchi;
		File fileToRead = new File(this.inputPath);

		try {
			this.reader = new RandomAccessFile(fileToRead, "r");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		int startPosition = 0;
		int blocchi_di_scarto = 0;

		for (int i = 0; i < totalBlockNumber; i++) {
			boolean scarto = false;
			reader.seek(startPosition);
			if (i == 0) {
				startPosition = 0;
			} else {
				reader.seek(startPosition + blockSize);
				// salvo il puntatore corrente di lettura file
				long pointer = reader.getFilePointer();
				if (pointer >= fileToRead.length()) {
					pointer = fileToRead.length();
					blocchi_di_scarto++;
					scarto = true;
					reader.seek(pointer);
				}
				if (pointer != fileToRead.length()) {
					// tiene conto che non sono a fine riga
					if (reader.readLine().length() != 0) {
						reader.seek(pointer - 1);
						byte bit = reader.readByte();
						char c = (char) bit;

						if (c != '\n') // controllo di non essere appena a capo
						{
							// ho dei byte da leggere per arrivare a fine riga
							reader.seek(pointer);
							reader.readLine();

							// MEMO POINTER - in questo modo dovrebbe essere preciso
							pointer = reader.getFilePointer();
							startPosition = (int) pointer;
						} else {
							System.out.println("Sono a fine riga \n");
							startPosition = startPosition + blockSize;
						}
					}
				}
			}
			if (scarto == false)
				startPosition_list.add(startPosition);
		}

		if (blocchi_di_scarto > 0)
			this.totalBlockNumber -= blocchi_di_scarto;

		reader.close();

	}

	/**
	 * @return {@code totalBlockNumber} is equal to the number of total blocks
	 *         needed to read the file in blocks
	 */
	public int getTotalBlockNumber() {
		return totalBlockNumber;
	}

	/**
	 * @param blockNumber
	 *            block with a specific number that we specific the operation of
	 *            the {@code NewLinesReader}
	 * 
	 * @return create a new istance of NewLinesReader with start position of the
	 *         reading point and the blocksize of fixed length block, that is
	 *         necessary for a method in this class to know when the dimension
	 *         of fixed block was achieved in the reading operation
	 * @throws IOException
	 */
	public InputLinesReader getInputLinesReader(int blockNumber) throws IOException {
		// todo in base al blockNumber restituisce un oggetto che legge dal byte
		// X al byte Y oppure al blockSize

		File f = null;
		f = new File(this.inputPath);
		return new InputLinesReader(startPosition_list.get(blockNumber), blockSize, f);
	}

	/**
	 * @param blockNumber
	 *            block with a specific number that we specific the operation of
	 *            the {@code NewLinesReader}
	 * 
	 * @return create a new istance of NewLinesReader with start position of the
	 *         reading point and the blocksize of fixed length block, that is
	 *         necessary for a method in this class to know when the dimension
	 *         of fixed block was achieved in the reading operation
	 * @throws IOException
	 */
	public SortedLinesReader getSortedLinesReader(int blockNumber) throws IOException {
		// todo in base al blockNumber restituisce un oggetto che legge dal byte
		// X al byte Y oppure al blockSize
		File f = null;
		f = new File(this.inputPath);
		return new SortedLinesReader(startPosition_list.get(blockNumber), blockSize, f);
	}

	/**
	 * 
	 * Read files in a specific folder and read them after they are splitted in
	 * blocks
	 * 
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String cartella = "output/prova_file/";

		File cartellaCorrente = new File(cartella);
		File files[] = cartellaCorrente.listFiles();
		for (File f : files) {
			String nomeFile = f.getName();

			System.out.println("Elaborazione a blocchi del percorso: " + nomeFile + "\n");

			BlockReader blockreader = new BlockReader(cartella + nomeFile, (25 * 1024 * 1024));

			for (int i = 0; i < blockreader.totalBlockNumber; i++) {

				InputLinesReader line_reader = blockreader.getInputLinesReader(i);

				String line;

				while ((line = line_reader.readLine()) != null) {
					// esegui elaborazione sulla riga
					System.out.println("readline: [" + line + "]");

				}
			}

			System.out.println("Esecuzione terminata \n");

			blockreader.reader.close();

		} // fine for files

	}
}
