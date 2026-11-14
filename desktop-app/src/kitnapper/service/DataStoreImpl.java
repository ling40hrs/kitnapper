package kitnapper.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

import kitnapper.model.Equipment;
import kitnapper.model.LoanRecord;
import kitnapper.model.Penalty;
import kitnapper.model.User;
import kitnapper.service.persistence.JsonParser;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Loads {@code kitnapper-data.json} from the application folder on startup
 * and saves the aggregate after every mutation (REQ-012). A missing,
 * unparseable, or semantically corrupt file never crashes the app: it
 * seeds fresh demo data and saves it immediately (NFR-07, D18). Writes go
 * to a tmp file that is atomically moved over the target, with a
 * replace-existing fallback where Windows forbids the atomic move.
 */
public final class DataStoreImpl implements DataStore {

    public static final String SAVE_FILE = "kitnapper-data.json";
    private static final String TMP_FILE = "kitnapper-data.json.tmp";

    private final Path savePath;
    private final Path tmpPath;
    private final RootMapper rootMapper = new RootMapper();
    private AppData data = new AppData();

    /** Uses the current working directory as the application folder. */
    public DataStoreImpl() {
        this(Paths.get("").toAbsolutePath());
    }

    /**
     * @param appFolder directory holding the save file (injectable so the
     *                  M1 round-trip check can use a scratch directory)
     */
    public DataStoreImpl(Path appFolder) {
        savePath = appFolder.resolve(SAVE_FILE);
        tmpPath = appFolder.resolve(TMP_FILE);
    }

    @Override
    public AppData load() {
        if (!Files.exists(savePath)) {
            return seedAndSave();
        }
        try {
            Object document = JsonParser.parse(Files.readString(savePath, StandardCharsets.UTF_8));
            data = rootMapper.fromDocument(document);
            return data;
        } catch (RuntimeException | IOException corrupted) {
            System.err.println("KitNapper: unusable save file (" + corrupted.getMessage()
                    + "); seeding fresh demo data.");
            return seedAndSave();
        }
    }

    @Override
    public void save() {
        try {
            Files.writeString(tmpPath, JsonWriter.write(rootMapper.toDocument(data)),
                    StandardCharsets.UTF_8);
            moveTmpOverTarget();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + savePath, e);
        }
    }

    private void moveTmpOverTarget() throws IOException {
        try {
            Files.move(tmpPath, savePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmpPath, savePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void resetToSeed() {
        seedAndSave();
    }

    private AppData seedAndSave() {
        data = SeedData.create(LocalDate.now());
        save();
        return data;
    }

    @Override
    public List<User> users() {
        return data.users();
    }

    @Override
    public List<Equipment> equipment() {
        return data.equipment();
    }

    @Override
    public List<LoanRecord> loans() {
        return data.loans();
    }

    @Override
    public List<Penalty> penalties() {
        return data.penalties();
    }
}
