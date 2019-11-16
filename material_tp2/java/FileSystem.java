import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;

public class FileSystem {
	static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
	static int dir_entries = block_size / dir_entry_size;
	static String currentPath = "root";

	/* FAT data structure */
	final static short[] fat = new short[blocks];
	/* data block */
	final static byte[] data_block = new byte[block_size];

	/* reads a data block from disk */
	public static byte[] readBlock(final String file, final int block) {
		final byte[] record = new byte[block_size];
		try {
			final RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.read(record, 0, block_size);
			fileStore.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return record;
	}

	/* writes a data block to disk */
	public static void writeBlock(final String file, final int block, final byte[] record) {
		try {
			final RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.write(record, 0, block_size);
			fileStore.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/* reads the FAT from disk */
	public static short[] readFat(final String file) {
		final short[] record = new short[blocks];
		try {
			final RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				record[i] = fileStore.readShort();
			fileStore.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return record;
	}

	/* writes the FAT to disk */
	public static void writeFat(final String file, final short[] fat) {
		try {
			final RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				fileStore.writeShort(fat[i]);
			fileStore.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/* reads a directory entry from a directory */
	public static DirEntry readDirEntry(final int block, final int entry) {
		final byte[] bytes = readBlock("filesystem.dat", block);
		final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		final DataInputStream in = new DataInputStream(bis);
		final DirEntry dir_entry = new DirEntry();

		try {
			in.skipBytes(entry * dir_entry_size);

			for (int i = 0; i < 25; i++)
				dir_entry.filename[i] = in.readByte();
			dir_entry.attributes = in.readByte();
			dir_entry.first_block = in.readShort();
			dir_entry.size = in.readInt();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return dir_entry;
	}

	/* writes a directory entry in a directory */
	public static void writeDirEntry(final int block, final int entry, final DirEntry dir_entry) {
		final byte[] bytes = readBlock("filesystem.dat", block);
		final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		final DataInputStream in = new DataInputStream(bis);
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(bos);

		try {
			for (int i = 0; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());

			for (int i = 0; i < dir_entry_size; i++)
				in.readByte();

			for (int i = 0; i < 25; i++)
				out.writeByte(dir_entry.filename[i]);
			out.writeByte(dir_entry.attributes);
			out.writeShort(dir_entry.first_block);
			out.writeInt(dir_entry.size);

			for (int i = entry + 1; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final byte[] bytes2 = bos.toByteArray();
		for (int i = 0; i < bytes2.length; i++)
			data_block[i] = bytes2[i];
		writeBlock("filesystem.dat", block, data_block);
	}

	public static void init() {
		/* initialize the FAT */
		for (int i = 0; i < fat_blocks; i++)
			fat[i] = 0x7ffe;
		fat[root_block] = 0x7fff;
		for (int i = root_block + 1; i < blocks; i++)
			fat[i] = 0;
		/* write it to disk */
		writeFat("filesystem.dat", fat);

		/* initialize an empty data block */
		for (int i = 0; i < block_size; i++)
			data_block[i] = 0;

		/* write an empty ROOT directory block */
		writeBlock("filesystem.dat", root_block, data_block);

		/* write the remaining data blocks to disk */
		for (int i = root_block + 1; i < blocks; i++)
			writeBlock("filesystem.dat", i, data_block);
	}

	public static DirEntry findDir(String[] caminho, int index) {
		if (currentPath.equals("root")) {
			for (int i : dir_entry) {

			}
		}
		if (currentPath.equals(caminho[index])) {
			// return null;
		}

	}

	public static void mkdir(String path){
		String[] caminho = path.split("/");
		
		DirEntry dir_entry = new DirEntry();
		String name = "file1";
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 1111;
		dir_entry.size = 222;
		int blockFinal;
		//blockFinal= findDir(caminho);
		actualDir = readDirEntry(block, entry);
		writeDirEntry(root_block, 0, dir_entry);
	}

	public static void listRoot() {
		DirEntry dir_entry = new DirEntry();
		for (int i = 0; i < dir_entries; i++) {
			dir_entry = readDirEntry(root_block, i);
			System.out.println("Entry " + i + ", file: " + new String(dir_entry.filename) + " attr: "
					+ dir_entry.attributes + " first: " + dir_entry.first_block + " size: " + dir_entry.size);
		}
	}

	public static void main(final String args[]) {

		/* fill three root directory entries and list them */
		// DirEntry dir_entry = new DirEntry();
		// String name = "file1";
		// byte[] namebytes = name.getBytes();
		// for (int i = 0; i < namebytes.length; i++)
		// dir_entry.filename[i] = namebytes[i];
		// dir_entry.attributes = 0x01;
		// dir_entry.first_block = 1111;
		// dir_entry.size = 222;
		writeDirEntry(root_block, 0, dir_entry);

		laco: while (true) {
			System.out.print("testShell@Shell:~" + currentPath + "$ ");
			Scanner sc = new Scanner(System.in);
			String input = sc.nextLine();
			System.out.println();
			switch (input) {
			case "exit":
				break laco;
			case "init":
				init();
				break;
			case "ls":
				break;
			case "load":
				break;
			case "mkdir":
				break;
			case "create":
				break;
			case "unlink":
				break;

			}
		}

		name = "file2";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 2222;
		dir_entry.size = 333;
		writeDirEntry(root_block, 1, dir_entry);

		name = "file3";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 4444;
		dir_entry.size = 444;
		writeDirEntry(root_block, 2, dir_entry); // bloco pra escrever / posicao do bloco / direntry

		/* list entries from the root directory */
		for (int i = 0; i < dir_entries; i++) {
			dir_entry = readDirEntry(root_block, i);
			System.out.println("Entry " + i + ", file: " + new String(dir_entry.filename) + " attr: "
					+ dir_entry.attributes + " first: " + dir_entry.first_block + " size: " + dir_entry.size);
		}
	}
}
