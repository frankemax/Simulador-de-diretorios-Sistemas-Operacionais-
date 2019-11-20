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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
    public static void writeDirEntry(final int block, final int entry, final DirEntry dir_entry, byte[] data_block) {
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

    private static boolean equal(byte[] t, byte[] p) {
        boolean n = false;
        int i = t.length;
        if (p.length < i) {
            i = p.length;
        }
        for (int j = 0; j < i; j++) {
            if (t[j] == p[j]) {
                n = true;
            } else {
                n = false;
                break;
            }
        }
        return n;
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
            System.out.printf("fat[%d] = %d \n", i, fat[i]);
            if (fat[i] == 0) {
                // System.out.println("entrou");

                return (short) i;

            }
        }
        System.out.println("FAT TA LOTADA");
        return -1;
    }

    // 0x01 - arquivo
    // 0x02 - diretorio
    public static void procuraDiretorioeCria(String[] caminho, short blocoAtual, int count) {
        byte[] db;
        short firstBlock = primeiroBlocoVazioDaFat();
        if (caminho.length - 1 == count) {
            if (existeNoBloco(blocoAtual, caminho[count])) {
                System.out.println("O arquivo/entrada de diretório chamado " + caminho[count] + " já existe");
                return;
            }
            fat[firstBlock] = 0x7fff;
            writeFat("filesystem.dat", fat);

            DirEntry entry = instanciaDir(caminho[caminho.length - 1], (byte) 0x02, firstBlock, 0);
            db = readBlock("filesystem.dat", blocoAtual);
            writeDirEntry(blocoAtual, verificaVazio(blocoAtual), entry, db);

        } else {
            boolean found = false;

            for (int i = 0; i < dir_entry_size && found == false; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);

                if (equal(entry.filename, caminho[count].getBytes())) {
                    found = true;
                    if (entry.attributes == (byte) 0x01) {
                        System.out.println("O caminho especificado não é um diretório, e sim um arquivo");
                        break;
                    }
                    procuraDiretorioeCria(caminho, entry.first_block, count + 1);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }

    public static void mkdir(String path) {
        String[] caminho = path.split("/");
        procuraDiretorioeCria(caminho, (short) root_block, 0);
    }

    public static void lsAuxiliar(String[] caminho, short blocoAtual, int count) {
        DirEntry dir_entry = new DirEntry();

        if (caminho.length == count) {
            for (int i = 0; i < dir_entries; i++) {
                dir_entry = readDirEntry(blocoAtual, i);
                System.out.println("Entry " + i + ", file: " + new String(dir_entry.filename) + " attr: "
                        + dir_entry.attributes + " first: " + dir_entry.first_block + " size: " + dir_entry.size);
            }

        } else {
            boolean found = false;

            byte[] file = caminho[count].getBytes();

            for (int i = 0; i < dir_entry_size && found == false; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);

                if (equal(entry.filename, caminho[count].getBytes())) {

                    found = true;
                    if (entry.attributes == (byte) 0x01) {
                        System.out.println(
                                "O caminho especificado não é um diretório, e sim um arquivo, portanto nao pode ser listado. para ler, use o comando: read");
                        break;
                    }
                    lsAuxiliar(caminho, entry.first_block, count + 1);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }

    public static void ls(String path) {
        DirEntry dir_entry = new DirEntry();

        if (path == null) {
            for (int i = 0; i < dir_entries; i++) {
                dir_entry = readDirEntry(root_block, i);
                System.out.println("Entry " + i + ", file: " + new String(dir_entry.filename) + " attr: "
                        + dir_entry.attributes + " first: " + dir_entry.first_block + " size: " + dir_entry.size);
            }
            return;
        }
        String[] caminho = path.split("/");

        lsAuxiliar(caminho, (short) root_block, 0);
    }

    // 0x01 - arquivo
    // 0x02 - diretorio
    public static void procuraDiretorioeCriaArquivo(String[] caminho, short blocoAtual, int count) {
        byte[] db;

        short firstBlock = primeiroBlocoVazioDaFat();
        if (caminho.length - 1 == count) {
            if (existeNoBloco(blocoAtual, caminho[count])) {
                System.out.println("O arquivo chamado " + caminho[count] + " ja existe");
                return;
            }
            fat[firstBlock] = 0x7fff;
            writeFat("filesystem.dat", fat);

            DirEntry entry = instanciaDir(caminho[caminho.length - 1], (byte) 0x01, firstBlock, 0);
            db = readBlock("filesystem.dat", blocoAtual);
            writeDirEntry(blocoAtual, verificaVazio(blocoAtual), entry, db);

            //writeBlock("filesystem.dat", firstBlock, data_block);
        } else {
            boolean found = false;

            byte[] file = caminho[count].getBytes();

            for (int i = 0; i < dir_entry_size && found == false; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);

                if (equal(entry.filename, caminho[count].getBytes())) {
                    found = true;
                    if (entry.attributes == (byte) 0x01) {
                        System.out.println("O caminho especificado não é um diretório, e sim um arquivo");
                        break;
                    }
                    procuraDiretorioeCriaArquivo(caminho, entry.first_block, count + 1);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }

    public static void create(String path) {
        String[] caminho = path.split("/");
        procuraDiretorioeCriaArquivo(caminho, (short) root_block, 0);
    }

    private static boolean isTheFirstOne(int blocoAtual, String path) {

        boolean n = false;
        byte[] t = path.getBytes();
        DirEntry p = readDirEntry(blocoAtual, 0);

        for (int j = 0; j < path.length(); j++) {

            if (t[j] == p.filename[j]) {
                n = true;
            } else {
                n = false;
                break;
            }
        }
        return n;
    }

    private static int existeNoBlocoInt(int blocoAtual, String path) {
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
                return i;
            }
        }

        return -1;
    }

    public static void percorreFateLimpaBloco(short bloco) {
        byte[] db;
        System.out.println("bloco fat= " + bloco);

        db = readBlock("filesystem.dat", bloco);
        for (int i = 0; i < block_size; i++) {
            db[i] = 0;
        }
        writeBlock("filesystem.dat", bloco, db);
    }

    public static void procuraDiretorioeUnlinka(String[] caminho, short blocoAtual, int count) {
        //to do -> verificar se a pasta ta vazia
        byte[] db;
        // short firstBlock = primeiroBlocoVazioDaFat();
        int aux = 0;
        if (caminho.length - 1 == count) {
            if (existeNoBloco(blocoAtual, caminho[count])) {

                for (int i = 0; i < dir_entry_size; i++) {
                    DirEntry entry = readDirEntry(blocoAtual, i);
                    if (equal(entry.filename, caminho[count].getBytes())) {
                        aux = i;
                    }
                }

                byte[] file = new byte[25];
                //byte atributos, short first_block, int size)
                DirEntry entry = readDirEntry(blocoAtual, aux);
                percorreFateLimpaBloco(entry.first_block);
                fat[entry.first_block] = 0;
                writeFat("filesystem.dat", fat);
                entry = instanciaDir("", (byte) 0, (short) 0, 0);

                db = readBlock("filesystem.dat", blocoAtual);

                writeDirEntry(blocoAtual, aux, entry, db);

                //writeBlock("filesystem.dat", blocoAtual + existeNoBlocoInt(blocoAtual, caminho[count]) + 1, data_block);
                return;

            }

        } else {
            boolean found = false;

            byte[] file = caminho[count].getBytes();

            for (int i = 0; i < dir_entry_size && found == false; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);

                if (equal(entry.filename, caminho[count].getBytes())) {
                    found = true;
                    if (entry.attributes == (byte) 0x01) {
                        System.out.println("O caminho especificado não é um diretório, e sim um arquivo");
                        break;
                    }
                    procuraDiretorioeUnlinka(caminho, entry.first_block, count + 1);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }

    public static short[] getListFat(int n) {
        int count = 0;
        short[] s = new short[n];
        for (int i = 5; i < fat_size; i++) {
            if (fat[i] == 0) {
                s[count] = (short) i;
                count++;
                if (count == n) {
                    break;
                }
            }
        }
        return s;
    }

    public static void write(String str, String path) {
        String[] caminho = path.split("/");
        if (str.getBytes().length > 1024) {
            writeCerto(caminho, str);
        } else {
            writeAux(caminho, (short) root_block, 0, str);
        }
    }

    public static void writeCerto(String[] caminho, String str) {
        int var = (int) Math.ceil(str.getBytes().length / 1024.0);
        short[] lista = getListFat(var);
        String[] text = new String[var];
        for (int i = 0; i < var - 1; i++) {
            text[i] = str.substring(i * 1024, (i * 1024) + 1024);
        }
        text[var - 1] = str.substring((var - 1) * 1024, str.length());
        writeAux(caminho, (short) root_block, 0, text[0]);
        for (int i = 1; i < var - 1; i++) {
            metododoshell(lista[i], lista[i + 1], text[i]);
        }
        metododoshell(lista[var - 1], (short) 0x7fff, text[var - 1]);
        System.out.println("Lista 0" + lista[0]);
        fat[lista[0]] = lista[1];
        writeFat("filesystem.dat", fat);
    }

    public static void metododoshell(short pos, short proximo, String s) {
        byte[] db;
        fat[pos] = proximo;
        db = readBlock("filesystem.dat", pos);
        byte[] arr = s.getBytes();
        for (int i = 0; i < arr.length; i++) {
            db[i] = arr[i];
        }
        writeFat("filesystem.dat", fat);
        writeBlock("filesystem.dat", pos, db);
    }

    private static void writeAux(String[] caminho, short blocoAtual, int count, String str) {

        // short firstBlock = primeiroBlocoVazioDaFat();
        if (caminho.length - 1 == count) {
            if (!existeNoBloco(blocoAtual, caminho[count])) {
                System.out.println("O arquivo/entrada de diretório chamado " + caminho[count] + " nao existe");
                return;
            }
            int aux = 0;
            for (int i = 0; i < dir_entry_size; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);
                if (equal(entry.filename, caminho[count].getBytes())) {
                    aux = i;
                    if (entry.attributes == (byte) 0x02) {
                        System.out.println("O caminho especificado é um diretório, e nao um arquivo");
                        return;
                    }
                }
            }
            byte[] db;
            db = readBlock("filesystem.dat", blocoAtual);
            byte[] arr = str.getBytes();
            for (byte i : arr) {
                System.out.println(i);
            }

            byte[] bloco = data_block;

            for (int i = 0; i < arr.length; i++) {
                bloco[i] = arr[i];
            }

            for (int i = arr.length + 1; i < bloco.length; i++) {
                bloco[i] = 0;
            }

            DirEntry entry = readDirEntry(blocoAtual, aux);
            entry.size = arr.length;

            writeDirEntry(blocoAtual, aux, entry, db);
            writeBlock("filesystem.dat", entry.first_block, bloco);

        } else {
            boolean found = false;

            for (int i = 0; i < dir_entry_size && found == false; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);

                if (equal(entry.filename, caminho[count].getBytes())) {
                    found = true;
                    if (entry.attributes == (byte) 0x01) {
                        System.out.println("O caminho especificado não é um diretório, e sim um arquivo");
                        break;
                    }
                    writeAux(caminho, entry.first_block, count + 1, str);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }

    public static void read(String path) {
        String[] caminho = path.split("/");
        readAux(caminho, (short) root_block, 0);
    }

    public static void readAux(String[] caminho, short blocoAtual, int count) {
        DirEntry dir_entry = new DirEntry();

        if (caminho.length - 1 == count) {
            if (!existeNoBloco(blocoAtual, caminho[count])) {
                System.out.println("O arquivo/entrada de diretório chamado " + caminho[count] + " nao existe");
                return;
            }
            int aux = 0;
            for (int i = 0; i < dir_entry_size; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);
                if (equal(entry.filename, caminho[count].getBytes())) {
                    aux = i;
                    if (entry.attributes == (byte) 0x02) {
                        System.out.println("O caminho especificado é um diretório, e nao um arquivo");
                        return;
                    }
                }
            }

            DirEntry entry = readDirEntry(blocoAtual, aux);
            byte[] bloco = readBlock("filesystem.dat", entry.first_block);
            System.out.println(new String(bloco));

        } else {
            boolean found = false;

            byte[] file = caminho[count].getBytes();

            for (int i = 0; i < dir_entry_size && found == false; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);

                if (equal(entry.filename, caminho[count].getBytes())) {

                    found = true;
                    if (entry.attributes == (byte) 0x01) {
                        System.out.println(
                                "O caminho especificado não é um diretório, e sim um arquivo, portanto nao pode ser listado. para ler, use o comando: read");
                        break;
                    }
                    readAux(caminho, entry.first_block, count + 1);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }

    public static void append(String str, String path) {
        String[] caminho = path.split("/");
        if (str.getBytes().length > 1024) {
            // to do
        } else {
            appendAux(caminho, (short) root_block, 0, str);
        }
    }

    private static void appendAux(String[] caminho, short blocoAtual, int count, String str) {

        // short firstBlock = primeiroBlocoVazioDaFat();
        if (caminho.length - 1 == count) {
            if (!existeNoBloco(blocoAtual, caminho[count])) {
                System.out.println("O arquivo/entrada de diretório chamado " + caminho[count] + " nao existe");
                return;
            }
            int aux = 0;
            for (int i = 0; i < dir_entry_size; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);
                if (equal(entry.filename, caminho[count].getBytes())) {
                    aux = i;
                    if (entry.attributes == (byte) 0x02) {
                        System.out.println("O caminho especificado é um diretório, e nao um arquivo");
                        return;
                    }
                }
            }
            byte[] arr = str.getBytes();
            for (byte i : arr) {
                System.out.println(i);
            }

            DirEntry entry = readDirEntry(blocoAtual, aux);
            byte[] bloco = readBlock("filesystem.dat", entry.first_block);
            int pedra = 0;
            for (int i = 0; i < bloco.length; i++) {
                if (bloco[i] == 0) {
                    System.out.println();
                    pedra = i;
                    break;
                }
            }

            for (int i = pedra; i < pedra + arr.length; i++) {
                bloco[i] = arr[i - pedra];
            }

            for (int i = pedra + arr.length + 1; i < bloco.length; i++) {
                bloco[i] = 0;
            }

            entry.size = arr.length;
            writeBlock("filesystem.dat", entry.first_block, bloco);

            for (int i = 0; i < block_size; i++) {
                data_block[i] = 0;
            }
            //writeDirEntry(blocoAtual, aux, entry);

        } else {
            boolean found = false;

            for (int i = 0; i < dir_entry_size && found == false; i++) {
                DirEntry entry = readDirEntry(blocoAtual, i);

                if (equal(entry.filename, caminho[count].getBytes())) {
                    found = true;
                    if (entry.attributes == (byte) 0x01) {
                        System.out.println("O caminho especificado não é um diretório, e sim um arquivo");
                        break;
                    }
                    appendAux(caminho, entry.first_block, count + 1, str);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }

    public static void unlink(String path) {
        String[] caminho = path.split("/");
        procuraDiretorioeUnlinka(caminho, (short) root_block, 0);

    }

    public static void main(final String[] args) {
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
                    if (inputArr.length == 1) {
                        ls(null);
                    } else {
                        ls(inputArr[1]);
                    }
                    break;
                case "load":
                    primeiroBlocoVazioDaFat();
                    break;
                case "write":
                    write(inputArr[1], inputArr[2]);
                    break;
                case "mkdir":
                    mkdir(inputArr[1]);
                    break;
                case "create":
                    create(inputArr[1]);
                    break;
                case "unlink":
                    unlink(inputArr[1]);
                    break;
                case "append":
                    append(inputArr[1], inputArr[2]);
                    break;
                case "read":
                    read(inputArr[1]);
                    break;
            }
        }

        /* list entries from the root directory */
    }
}
