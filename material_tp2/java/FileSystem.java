public static void append(String str, String path) {
        String[] caminho = path.split("/");
        int b = getBlocoTam(caminho, (short) root_block, 0);
        int tam = b + str.getBytes().length;
        if (tam >= 1024) {
            int var = (int) Math.ceil(tam / 1024.0);
            short[] lista = getListFat(var, caminho);
            String[] text = new String[var];
            String cortada = str.substring(0, 1024 - b);
            text[0] = cortada;
            appendAux(caminho, (short) root_block, 0, text[0], tam);
            
            str = str.substring(cortada.length(), str.length());
            for (int i = 0; i < var - 2; i++) {
                text[i+1] = str.substring(i * (1024), (i * 1024) + 1024);
            }
            text[var - 1] = str.substring((var - 2) * 1024, str.length());

            for (int i = 1; i < var - 1; i++) {
                System.out.println(i);
                metododoshell(lista[i], lista[i + 1], text[i]);
            }
            metododoshell(lista[var - 1], (short) 0x7fff, text[var - 1]);
            fat[lista[0]] = lista[1];
            writeFat("filesystem.dat", fat);
        } else {
            appendAux(caminho, (short) root_block, 0, str, tam);
        }
    }

    private static void appendAux(String[] caminho, short blocoAtual, int count, String str, int size) {

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

            entry.size = size;
            System.out.println(entry);
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
                    appendAux(caminho, entry.first_block, count + 1, str, size);
                }
            }

            if (found == false) {
                System.out.println("Não há nenhum diretório chamado /" + caminho[count]);
            }
        }
    }
