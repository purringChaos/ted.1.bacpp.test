package tv.blackarrow.cpp.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides a hash map that is backed with a persistent file data store. The
 * contents of the hash map are retained the next time the map is instantiated
 * with the same persistence directory. This map only supports persistence
 * through the put, putAll, remove and clear methods. Changing an object that is
 * currently in the map will change the object for the life of the map instance
 * but will not persist those changes. The map returned is thread safe. The key
 * and value data types must implement {@link java.io.Serializable}. If an
 * exception maintaining the persistent data store occurs at runtime, it will be
 * logged but not thrown during any of the standard map interface methods to
 * conform to that interface specification. The constructor and compact methods
 * will throw exceptions if they encounter errors however.
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * @param <V>
 *            the type of mapped values
 */
public class PersistentHashMap<K extends Serializable, V extends Serializable> implements Map<K, V> {

    private static final Logger LOGGER = LogManager.getLogger(PersistentHashMap.class.getName());

    private static final String ACTION_PUT = "P";
    private static final String ACTION_REMOVE = "R";
    private static final String ACTION_CLEAR = "C";

    private Map<K, V> dataMap;
    private String baseJournalFullFileName;
    private String temporaryBaseJournalFullFileName;
    private String activeJournalFullFileName;
    private String compactingJournalFullFileName;
    private ObjectOutputStream activeJournalObjectOutputStream;
    private Object compactMonitor = new Object();

    /**
     * Creates an instance of a hash map with persistent backing using the
     * directory provided for all persistence files. To ensure consistency, the
     * user should not create multiple instances of a map that use the same
     * persistent backing directory.
     * 
     * @param persistenceDirectory
     *            Directory where all files related to persisting the data in
     *            this hash map will be stored.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public PersistentHashMap(File persistenceDirectory) throws IOException, ClassNotFoundException {
        if (!persistenceDirectory.exists()) {
            persistenceDirectory.mkdirs();
        }
        String fileNamePrefix = persistenceDirectory.getPath() + File.separatorChar + "PersistentHashMapJournal";
        baseJournalFullFileName = fileNamePrefix + "Base.dat";
        temporaryBaseJournalFullFileName = baseJournalFullFileName + ".tmp";
        activeJournalFullFileName = fileNamePrefix + "Active.dat";
        compactingJournalFullFileName = fileNamePrefix + "Compacting.dat";
        init();
    }

    @Override
    public int size() {
        return dataMap.size();
    }

    @Override
    public boolean isEmpty() {
        return dataMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return dataMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return dataMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return dataMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        V oldValue = dataMap.put(key, value);
        try {
            writeActiveJournalEntry(ACTION_PUT, key, value);
        } catch (IOException e) {
            LOGGER.error(()->"Error handling put operation. Supressing error at this point.", e);
        }
        return oldValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        V oldValue = dataMap.remove(key);
        if (oldValue != null) {
            try {
                writeActiveJournalEntry(ACTION_REMOVE, (K) key, null);
            } catch (IOException e) {
                LOGGER.error(()->"Error handling remove operation. Supressing error at this point.", e);
            }
        }
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

        // for simplicity we will just iterate and use the put version to
        // ensure that persistence is maintained properly
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        dataMap.clear();
        try {
            writeActiveJournalEntry(ACTION_CLEAR, null, null);
        } catch (IOException e) {
            LOGGER.error(()->"Error handling clear operation. Supressing error at this point.", e);
        }
    }

    @Override
    public Set<K> keySet() {
        return dataMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return dataMap.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return dataMap.entrySet();
    }

    /**
     * Compacts the persistence files to reduce wasted space that accumulates
     * when changes are made to the map data.
     * 
     * @throws IOException
     *             If an error occurs while compacting the files.
     */
    public void compact() throws IOException {
        try {
            // make sure two compact operations do not run at the same time
            synchronized (compactMonitor) {
                rolloverToCompactingFile();
                createNewBaseFile();
                // now that we have captured the state of the map into the new base file,
                // we can discard all of the journal data in the compacting file.
                File compactingFile = new File(compactingJournalFullFileName);
                if (compactingFile.exists()) {
                    compactingFile.delete();
                }
            }
        } catch (IOException e) {
            LOGGER.error(()->"Error compacting journal files", e);
            throw e;
        }
    }

    /**
     * The built in implementation of ObjectOutputStream does not support
     * appending to an existing file. This implementation simply suppresses the
     * creation of the file header to make that possible.
     */
    private class AppendingObjectOutputStream extends ObjectOutputStream {

        public AppendingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            // do not write a header but we need to reset to ensure the base class works
            reset();
        }

    }

    /**
     * Writes a journal entry to the active journal file.
     * 
     * @param action
     *            Action to write.
     * @param key
     *            Map key associated with this entry (if any).
     * @param value
     *            Map value associated with this entry (if any).
     * @throws IOException
     *             If an error occurs writing this entry to the persistent data
     *             store.
     */
    private void writeActiveJournalEntry(String action, K key, V value) throws IOException {
        writeJournalEntry(activeJournalFullFileName, activeJournalObjectOutputStream, action, key, value);
    }

    /**
     * Writes a journal entry to the stream specified.
     * 
     * @param fullFileName
     *            Full file name of the journal file being written to. This is
     *            only used for reporting errors.
     * @param oos
     *            ObjectOutputStream for the journal file to write to.
     * @param action
     *            Action to write.
     * @param key
     *            Map key associated with this entry (if any).
     * @param value
     *            Map value associated with this entry (if any).
     * @throws IOException
     *             If an error occurs writing this entry to the persistent data
     *             store.
     */
    private synchronized void writeJournalEntry(String fullFileName, ObjectOutputStream oos, String action, K key, V value)
            throws IOException {
        try {
            oos.writeObject(action);
            oos.writeObject(key);
            oos.writeObject(value);
            oos.flush();
        } catch (IOException e) {
            LOGGER.error(()->"Error writing to journal file \"" + fullFileName + "\"", e);
            throw e;
        }
    }

    /**
     * Loads the contents of a journal file into the current hash map.
     * 
     * @param fileName
     *            Full name of the journal file to load.
     * @throws IOException
     *             If an error occurs reading from the journal file.
     * @throws ClassNotFoundException
     *             If an object in the journal file cannot be found for
     *             deserialization.
     */
    private void reloadJournalFile(String fullFileName) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;
        try {
            File file = new File(fullFileName);
            if (!file.exists()) {
                return;
            }
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ois = new ObjectInputStream(bis);
            boolean eof = false;
            while (!eof) {
                try {
                    String action = (String) ois.readObject();
                    @SuppressWarnings("unchecked")
                    K key = (K) ois.readObject();
                    @SuppressWarnings("unchecked")
                    V value = (V) ois.readObject();
                    processJournalEntry(action, key, value);
                } catch (EOFException e) {
                    eof = true;
                }
            }
        } catch (IOException e) {
            LOGGER.error(()->"Error reading from journal file \"" + fullFileName + "\"", e);
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.error(()->"Error processing object stream from journal file \"" + fullFileName + "\"", e);
            throw e;
        } finally {
            closeJournalFile(fullFileName, ois);
        }
    }

    /**
     * Modifies the current hash map contents by applying the given journal
     * entry.
     * 
     * @param action
     *            Action of journal entry.
     * @param key
     *            Map key associated with this entry (if any).
     * @param value
     *            Map value associated with this entry (if any).
     */
    private void processJournalEntry(String action, K key, V value) {
        if (ACTION_PUT.equals(action)) {
            dataMap.put(key, value);
        } else if (ACTION_REMOVE.equals(action)) {
            dataMap.remove(key);
        } else if (ACTION_CLEAR.equals(action)) {
            dataMap.clear();
        } else {
            LOGGER.error(()->"Unexpected action \"" + action + "\" found in journal entry");
        }
    }

    /**
     * Closes the journal file specified if it is not null.
     * 
     * @param fullFileName
     *            Full file name of the journal file to close. This is only used
     *            for error logging.
     * @param stream
     *            Stream of the journal file to close.
     * @throws IOException
     *             If an error occurs closing the stream.
     */
    private void closeJournalFile(String fullFileName, Closeable stream) throws IOException {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                LOGGER.error(()->"Error closing journal file \"" + fullFileName + "\"", e);
                throw e;
            }
        }
    }

    /**
     * Opens the active journal file for appending.
     * 
     * @throws IOException
     *             If an error occurs opening the journal file.
     */
    private synchronized void openActiveJournalFile() throws IOException {
        activeJournalObjectOutputStream = openJournalFile(activeJournalFullFileName);
    }

    /**
     * Opens the journal file specified for appending. If the file does not
     * exist, it is automatically created.
     * 
     * @param fullFileName
     *            Full file name of the journal file to open for appending.
     * @return The ObjectOutputStream for the journal file.
     * @throws IOException
     *             If an error occurs opening or creating the journal file.
     */
    private ObjectOutputStream openJournalFile(String fullFileName) throws IOException {
        File newFile = new File(fullFileName);
        try {
            // we only want the java object output stream header written one time so
            // if this is a new journal file just open it to write the header and close
            if (!newFile.exists()) {
                FileOutputStream fos = new FileOutputStream(newFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.close();
            }
            FileOutputStream fos = new FileOutputStream(newFile, true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new AppendingObjectOutputStream(bos);
            return oos;
        } catch (IOException e) {
            LOGGER.error(()->"Error opening journal file \"" + fullFileName + "\"", e);
            throw e;
        }
    }

    /**
     * Creates a new base file with the current contents of the hash map and
     * replaces the existing base file if successful.
     * 
     * @throws IOException
     *             If an error occurs creating the new base file.
     */
    private void createNewBaseFile() throws IOException {
        ObjectOutputStream oos = null;
        try {
            // remove any existing temporary base file (this might happen in a crash)
            File newFile = new File(temporaryBaseJournalFullFileName);
            if (newFile.exists()) {
                newFile.delete();
            }

            // write the current contents of the map to the new base file.  note that
            // it makes no difference if the map is changed while we are processing it
            // because the active journal file is capturing those changes and will be
            // processed as well whenever the map needs to be reloaded.
            oos = openJournalFile(temporaryBaseJournalFullFileName);
            for (Map.Entry<K, V> entry : entrySet()) {
                writeJournalEntry(temporaryBaseJournalFullFileName, oos, ACTION_PUT, entry.getKey(), entry.getValue());
            }

            // replace the existing base file with the new one
            File oldFile = new File(baseJournalFullFileName);
            if (oldFile.exists()) {
                oldFile.delete();
            }
            newFile.renameTo(oldFile);
        } finally {
            closeJournalFile(temporaryBaseJournalFullFileName, oos);
        }
    }

    /**
     * Moves the current active journal file to the compacting file name and
     * opens a new active journal file. The file is not rolled over if a
     * compacting file already exists.
     * 
     * @throws IOException
     *             If an error occurs moving the active journal file or creating
     *             the new one.
     */
    private synchronized void rolloverToCompactingFile() throws IOException {
        // if there is already a compacting file present then we can't roll over the current
        // file until we have created a new base file that captures that data.
        File compactingFile = new File(compactingJournalFullFileName);
        if (compactingFile.exists()) {
            return;
        }
        closeJournalFile(activeJournalFullFileName, activeJournalObjectOutputStream);

        // move the active journal file to the compacting file.  this represents journal
        // entries that can all be discarded if the current state of the map is written to
        // a new base file.
        File activeFile = new File(activeJournalFullFileName);
        activeFile.renameTo(compactingFile);
        openActiveJournalFile();
    }

    /**
     * Reloads data from the persistent backing files to the memory hash map.
     * 
     * @throws IOException
     *             If an error occurs reading from the journal files.
     * @throws ClassNotFoundException
     *             If an object in a journal file cannot be found for
     *             deserialization.
     */
    private void reloadFromPersistentBacking() throws IOException, ClassNotFoundException {
        dataMap.clear();

        // the base file has the last successfully compacted version of the map data
        // so we need to start with that file to restore the persisted state.
        reloadJournalFile(baseJournalFullFileName);

        // if there is a compacting file present then the system may have been shutdown
        // while compacting was in progress.  to restore the map properly we need to
        // process the compacting file before the active file.
        reloadJournalFile(compactingJournalFullFileName);
        reloadJournalFile(activeJournalFullFileName);
    }

    /**
     * Initializes the hash map and restores any data that is in the persistent
     * data store for it.
     * 
     * @throws IOException
     *             If an error occurs reading from the journal files.
     * @throws ClassNotFoundException
     *             If an object in a journal file cannot be found for
     *             deserialization.
     */
    private void init() throws IOException, ClassNotFoundException {
        dataMap = new ConcurrentHashMap<K, V>();
        reloadFromPersistentBacking();
        openActiveJournalFile();
    }

}
