package ru.fizteh.fivt.students.anastasia_ermolaeva.junit;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import ru.fizteh.fivt.storage.strings.Table;
import ru.fizteh.fivt.students.anastasia_ermolaeva.
        junit.util.ExitException;

public class DBTable implements Table/*, AutoCloseable*/ {
    private static final int DIR_AMOUNT = 16;
    private static final int FILES_AMOUNT = 16;
    private Map<String, String> allRecords; //Records readed from real file
    private Map<String, String> sessionChanges;
    private Path dbPath; // The table's directory.
    private String name;

    public DBTable(final Path rootPath, final String Name) {
        //String path = rootPath.toAbsolutePath().toString()
          //      + File.separator + name;
        dbPath = rootPath.resolve(Name);
        //dbPath = Paths.get(path);
        name = Name;
        allRecords = new HashMap<>();
        sessionChanges = new HashMap<>();
        create();
    }

    public DBTable(final Path rootPath, final String Name, Map<String, String> records) {
        //String path = rootPath.toAbsolutePath().toString()
            //    + File.separator + name;
        //dbPath = Paths.get(path);
        dbPath = rootPath.resolve(Name);
        name = Name;
        allRecords = Collections.synchronizedMap(records);
        sessionChanges = new HashMap<>();
    }

    public static void main() {
        //
    }
    private void create() {
        try {
            read();
        } catch (ExitException e) {
            System.exit(e.getStatus());
        }
    }
    public Path getDBTablePath(){
        return dbPath;
    }
    //public Map<String, String> getAllRecords() {
      //  return allRecords;
    //}

    private String readUtil(final RandomAccessFile dbFile) throws ExitException {
        try {
            int wordLength = dbFile.readInt();
            byte[] word = new byte[wordLength];
            dbFile.read(word, 0, wordLength);
            return new String(word, "UTF-8");
        } catch (IOException | SecurityException e) {
            System.err.println("Error reading the table");
            throw new ExitException(1);
        }
    }

    private void read() throws ExitException {
        //System.out.println(dbPath.toString());
        File pathDirectory = dbPath.toFile();
        if (pathDirectory.list()== null ||pathDirectory.list().length == 0)
            return;
        File[] tableDirectories = pathDirectory.listFiles();
        for (File t : tableDirectories) {
            // Checking subdirectories.
            if (!t.isDirectory()) {
                throw new IllegalStateException("Table subdirectories "
                           + "are not actually directories");
                //System.err.println("Table subdirectories "
                     //   + "are not actually directories");
                //throw new ExitException(1);
            }
        }
        for (File directory : tableDirectories) {
            File[] directoryFiles = directory.listFiles();
            int k = directory.getName().indexOf('.');
            if ((k < 0) || !(directory.getName().substring(k).equals(".dir"))) {
                throw new IllegalStateException("Table subdirectories don't "
                               + "have appropriate name");
                //System.err.println("Table subdirectories don't "
                 //       + "have appropriate name");
                //throw new ExitException(1);
            }
            try {
                /*
                Delete .dir and check(automatically )
                if the subdirectory has the suitable name.
                If not, then parseInt throws NumberFormatException,
                error message is shown.
                Then program would finish with exit code != 0.
                 */
                if (directory.list().length == 0) {
                    throw new IllegalStateException("Table has the wrong format");
                    //System.err.println("Table has the wrong format");
                    //throw new ExitException(1);
                }
                int nDirectory = Integer.parseInt(
                        directory.getName().substring(0, k));
                for (File file : directoryFiles) {
                    try {
                        k = file.getName().indexOf('.');
                        /*
                        Checking files' names the same way
                        we did with directories earlier.
                        */
                        if ((k < 0) || !(file.getName().substring(k).equals(".dat"))) {
                            throw new IllegalStateException("Table subdirectory's files doesn't "
                                         + "have appropriate name");
                            //System.err.println("Table subdirectory's files doesn't "
                              //      + "have appropriate name");
                            //throw new ExitException(1);
                        }
                        int nFile = Integer.parseInt(
                                file.getName().substring(0, k));
                        try (RandomAccessFile dbFile = new RandomAccessFile(file.getAbsolutePath(), "r")) {
                            if (dbFile.length() > 0) {
                                while (dbFile.getFilePointer() < dbFile.length()) {
                                    String key = readUtil(dbFile);
                                    String value = readUtil(dbFile);
                                    allRecords.put(key, value);
                                }
                            }
                            dbFile.close();
                        } catch (IOException e) {
                            System.err.println("Error reading to table");
                            throw new ExitException(1);
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException("Subdirectories' files "
                                    + "have wrong names, "
                                     + "expected(0.dat-15.dat)");
                        //System.err.println("Subdirectories' files "
                           //     + "have wrong names, "
                           //     + "expected(0.dat-15.dat)");
                        //throw new ExitException(1);
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Subdirectories' names are wrong, "
                              + "expected(0.dir - 15.dir)");
                //System.err.println("Subdirectories' names are wrong, "
                  //      + "expected(0.dir - 15.dir)");
                //throw new ExitException(1);
            }
        }
    }

    private void write(Map<String, String> records) throws ExitException {
        Map<String, String>[][] db = new Map[DIR_AMOUNT][FILES_AMOUNT];
        for (int i = 0; i < DIR_AMOUNT; i++) {
            for (int j = 0; j < FILES_AMOUNT; j++) {
                db[i][j] = new HashMap<>();
            }
        }
        for (Map.Entry<String, String> entry : records.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            try {
                int nDirectory = Math.abs(key.getBytes("UTF-8")[0] % DIR_AMOUNT);
                int nFile = Math.abs((key.getBytes("UTF-8")[0] / DIR_AMOUNT) % FILES_AMOUNT);
                db[nDirectory][nFile].put(key, value);
            } catch (UnsupportedEncodingException e) {
                System.err.println("Can't encode the record");
                throw new ExitException(1);
            }
        }
        for (int i = 0; i < DIR_AMOUNT; i++) {
            for (int j = 0; j < FILES_AMOUNT; j++) {
                if (!db[i][j].isEmpty()) {
                    Integer nDirectory = i;
                    Integer nFile = j;
                    Path newPath = dbPath.resolve(nDirectory.toString()+".dir");
                    //String newPath = dbPath.toAbsolutePath().toString()
                      //      + File.separator
                        //    + nDirectory.toString()
                          //  + ".dir";
                    File directory = newPath.toFile();
                    if (!directory.exists()) {
                        if (!directory.mkdir()) {
                            System.err.println("Cannot create directory");
                            throw new ExitException(1);
                        }
                    }
                    //String newFilePath = directory.getAbsolutePath()
                         //   + File.separator
                           // + nFile.toString()
                          //  + ".dat";
                    Path newFilePath = directory.toPath().resolve(nFile.toString() + ".dat");
                    File file = newFilePath.toFile();
                    try {
                        file.createNewFile();
                    } catch (IOException | SecurityException e) {
                        System.err.println(e);
                        throw new ExitException(1);
                    }
                    try (RandomAccessFile dbFile = new
                            RandomAccessFile(file, "rw")) {
                        dbFile.setLength(0);
                        for (Map.Entry<String, String> entry
                                : db[i][j].entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            writeUtil(key, dbFile);
                            writeUtil(value, dbFile);
                        }
                        dbFile.close();
                    } catch (IOException e) {
                        System.err.println(e);
                        throw new ExitException(1);
                    }
                } else {
                    //Deleting empty files and directories.
                    Integer nDirectory = i;
                    Integer nFile = j;
                    //String newPath = dbPath.toAbsolutePath().toString()
                      //      + File.separator
                        //    + nDirectory.toString()
                          //  + ".dir";
                    Path newPath = dbPath.resolve(nDirectory.toString()+".dir");
                    File directory = newPath.toFile();
                    if (directory.exists()) {
                        //String newFilePath = directory.getAbsolutePath()
                          //      + File.separator
                            //    + nFile.toString()
                              //  + ".dat";
                        Path newFilePath = directory.toPath().resolve(nFile.toString() + ".dat");
                        File file = newFilePath.toFile();
                        //File file = new File(newFilePath);
                        try {
                            Files.deleteIfExists(file.toPath());
                        } catch (IOException | SecurityException e) {
                            System.err.println(e);
                            throw new ExitException(1);
                        }
                        if (directory.list().length == 0) {
                            try {
                                Files.delete(directory.toPath());
                            } catch (IOException e) {
                                System.err.println(e);
                                throw new ExitException(1);
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < DIR_AMOUNT; i++) {
            for (int j = 0; j < FILES_AMOUNT; j++) {
                db[i][j].clear();
            }
        }
    }
// need check
    public final void close() /*throws ExitException*/ {
        commit();
        //write(allRecords);
    }

    private void writeUtil(final String word,
                           final RandomAccessFile dbFile) throws ExitException {
        try {
            dbFile.writeInt(word.getBytes("UTF-8").length);
            dbFile.write(word.getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println(e);
            throw new ExitException(1);
        }
    }

    public int getNumberOfChanges() {
        return sessionChanges.size();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key is a null-string");
        }
        if (sessionChanges.containsKey(key)) {
            return sessionChanges.get(key);
        } else {
            return allRecords.get(key);
        }
    }

    @Override
    public String put(String key, String value) {
        if (key == null || value == null ) {
            throw new IllegalArgumentException("Key and/or value is a null-string");
        }
        if (sessionChanges.containsKey(key)) { // The record with key has already been changed or added during the session.
            return sessionChanges.put(key, value);
            /*if (sessionChanges.get(key) == null) { // The record with key has been deleted during the session.
                sessionChanges.put(key, value);
                return null;
            }
            else { //Value of the record associated with key has been changed during the session.
                return sessionChanges.put(key, value);
            }*/
        }
        if (allRecords.containsKey(key)) { // The record with key hasn't been changed yet.
           sessionChanges.put(key, value);
           return allRecords.get(key);
        }
        return sessionChanges.put(key, value); // Absolutely new record.
    }

    @Override
    public String remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key is a null-string");
        }
        if (sessionChanges.containsKey(key)) {
            if (sessionChanges.get(key) == null) { // The record with key has been deleted during the session.
                return null;
            } else {
                return sessionChanges.put(key, null);// The record with key has already been changed or added during the session.
            }
        } else {
            if (allRecords.containsKey(key)) {
                sessionChanges.put(key, null);
                return allRecords.get(key);
            }
            return null;
        }
    }

    @Override
    public int size() {
        Set<String> keyList = new HashSet<>();
        keyList.addAll(allRecords.keySet());
        for (Map.Entry<String, String> entry: sessionChanges.entrySet()) {
            if (keyList.contains(entry.getKey())) {
                if (entry.getValue() == null) { // If the record was deleted during the session.
                    keyList.remove(entry.getKey());
                }
            } else {
                if (entry.getValue() != null) {
                    keyList.add(entry.getKey());
                }
            }
        }
        return keyList.size();
    }

    @Override
    public int commit() {
        int numberOfChanges = sessionChanges.size();
        // Execute changes to disk
        Map<String, String> tempStorage = new HashMap<>();
        tempStorage.putAll(allRecords);
        for (Map.Entry<String, String> entry: sessionChanges.entrySet()) {
            if (tempStorage.containsKey(entry.getKey())) {
                if (entry.getValue() == null) { // If the record was deleted during the session.
                    tempStorage.remove(entry.getKey());
                }
                else {
                    tempStorage.put(entry.getKey(),entry.getValue()); //If the value was changed during the session.
                }
            } else {
                if(entry.getValue() != null) {
                    tempStorage.put(entry.getKey(), entry.getValue());
                }
            }
        }
        try {
            write(tempStorage);
            allRecords = Collections.synchronizedMap(tempStorage);
            sessionChanges.clear();
        } catch (ExitException e) {
            System.err.println("Error while commiting");
            System.exit(e.getStatus());
        }
        return numberOfChanges;
    }

    @Override
    public int rollback() {
        int numberOfChanges = sessionChanges.size();
        sessionChanges.clear();
        return numberOfChanges;
    }

    @Override
    public List<String> list() {
        Set<String> keyList = new HashSet<>();
        keyList.addAll(allRecords.keySet());
        for (Map.Entry<String, String> entry: sessionChanges.entrySet()) {
            if (keyList.contains(entry.getKey())) {
                if (entry.getValue() == null) { // If the record was deleted during the session.
                    keyList.remove(entry.getKey());
                }
            } else {
                keyList.add(entry.getKey());
            }
        }
        List<String> list = new ArrayList<>();
        list.addAll(keyList);
        return list;
    }
}