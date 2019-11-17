package t2.sisop;

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
            for (int i = 0; i < blocks; i++) {
                record[i] = fileStore.readShort();
            }
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
            for (int i = 0; i < blocks; i++) {
                fileStore.writeShort(fat[i]);
            }
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

            for (int i = 0; i < 25; i++) {
                dir_entry.filename[i] = in.readByte();
            }
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
            for (int i = 0; i < entry * dir_entry_size; i++) {
                out.writeByte(in.readByte());
            }

            for (int i = 0; i < dir_entry_size; i++) {
                in.readByte();
            }

            for (int i = 0; i < 25; i++) {
                out.writeByte(dir_entry.filename[i]);
            }
            out.writeByte(dir_entry.attributes);
            out.writeShort(dir_entry.first_block);
            out.writeInt(dir_entry.size);

            for (int i = entry + 1; i < entry * dir_entry_size; i++) {
                out.writeByte(in.readByte());
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        final byte[] bytes2 = bos.toByteArray();
        for (int i = 0; i < bytes2.length; i++) {
            data_block[i] = bytes2[i];
        }
        writeBlock("filesystem.dat", block, data_block);
    }

    public static void init() {
        /* initialize the FAT */
        for (int i = 0; i < fat_blocks; i++) {
            fat[i] = 0x7ffe;
        }
        fat[root_block] = 0x7fff;
        for (int i = root_block + 1; i < blocks; i++) {
            fat[i] = 0;
        }
        /* write it to disk */
        writeFat("filesystem.dat", fat);

        /* initialize an empty data block */
        for (int i = 0; i < block_size; i++) {
            data_block[i] = 0;
        }

        /* write an empty ROOT directory block */
        writeBlock("filesystem.dat", root_block, data_block);

        /* write the remaining data blocks to disk */
        for (int i = root_block + 1; i < blocks; i++) {
            writeBlock("filesystem.dat", i, data_block);
        }
    }

    public static DirEntry instanciaDir(String nome, byte atributos, short first_block, int size) {
        DirEntry dir_entry = new DirEntry();
        String name = nome;
        byte[] namebytes = name.getBytes();
        for (int j = 0; j < namebytes.length; j++) {
            dir_entry.filename[j] = namebytes[j];
        }
        dir_entry.attributes = atributos;
        dir_entry.first_block = first_block;
        dir_entry.size = size;
        return dir_entry;
    }

    public static short verificaVazio(int block) {

        for (int i = 0; i < dir_entry_size; i++) {
            DirEntry entry = readDirEntry(block, i);

            if (entry.attributes == 0) {
                return (short) i;
            }
        }
        return -1;
    }

    private static boolean existeNoBloco(int blocoAtual, String path) {

        boolean n = false;
        byte[] t = path.getBytes();
        for (int i = 0; i < 32; i++) {
            DirEntry p = readDirEntry(blocoAtual, i);

            for (int j = 0; j < path.length(); j++) {
                if (t[j] == p.filename[j]) {
                    n = true;
                } else {
                    n = false;
                    break;
                }
            }
            if (n) {
                break;
            }
        }

        return n;
    }

    public static short primeiroBlocoVazioDaFat() {
        for (int i = 5; i < fat_size; i++) {
            if (fat[i] == 0) {
                return (short) i;
            }
        }
        System.out.println("FAT TA LOTADA");
        return -1;
    }

    //0x01 - arquivo
    //0x02 - diretorio
    public static void procuraDiretorioeCria(String[] caminho, short blocoAtual, int count) {

        DirEntry dir_entry = new DirEntry();
        if (caminho.length - 1 == count) {
            if (existeNoBloco(blocoAtual, caminho[count])) {
                System.out.println("O diretório chamado " + caminho[count] + " ja existe");
                return;
            }

            short firstBlock = primeiroBlocoVazioDaFat();
            fat[firstBlock] = 0x7fff;
            writeFat("filesystem.dat", fat);

            DirEntry entry = instanciaDir(caminho[caminho.length - 1], (byte) 0x02, (short) blocoAtual, 0);
            writeDirEntry(blocoAtual, verificaVazio(blocoAtual), entry);

        } else {
            boolean tem = true;
            byte[] diretorioAtual = caminho[count].getBytes();
            for (int i = 0; i < dir_entries; i++) {
                dir_entry = readDirEntry(root_block, i);

                //entra no diretorio
                if (dir_entry.filename == diretorioAtual) {
                    procuraDiretorioeCria(caminho, dir_entry.first_block, count + 1);
                    break;
                }
            }
        }
    }

    public static void mkdir(String path) {
        String[] caminho = path.split("/");
        procuraDiretorioeCria(caminho, (short) root_block, 0);

    }

    public static void ls() {
        DirEntry dir_entry = new DirEntry();
        for (int i = 0; i < dir_entries; i++) {
            dir_entry = readDirEntry(root_block, i);
            System.out.println("Entry " + i + ", file: " + new String(dir_entry.filename) + " attr: "
                    + dir_entry.attributes + " first: " + dir_entry.first_block + " size: " + dir_entry.size);
        }
    }

    public static void main(final String args[]) {

        laco:
        while (true) {
            System.out.print("testShell@Shell:~" + "$ ");
            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine();
            String[] inputArr = input.split(" ");
            System.out.println();
            switch (inputArr[0]) {
                case "exit":
                    break laco;
                case "init":
                    init();
                    break;
                case "ls":
                    ls();
                    break;
                case "load":

                    break;
                case "mkdir":
                    mkdir(inputArr[1]);
                    break;
                case "create":

                    break;
                case "unlink":
                    break;

            }
        }

    }
}
