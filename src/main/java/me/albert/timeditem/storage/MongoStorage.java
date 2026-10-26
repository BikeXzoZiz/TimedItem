package me.albert.timeditem.storage;

import com.mongodb.ConnectionString;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MongoDB backend (Settings.storage.type: mongodb). Collections:
 * timeditem_recovery, timeditem_audit, timeditem_stats - mirroring the
 * SQL schema one to one. The connection URI carries host, credentials
 * and options, so the config stays a single string.
 */
public final class MongoStorage implements Storage {

    private final Logger logger;
    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> recovery;
    private final MongoCollection<Document> audit;
    private final MongoCollection<Document> stats;
    private final long cutoffMillis;
    private final int maxItems;
    private final int auditHistory;
    /** Same amortization as SqlStorage: count+trim round trips every 64th insert. */
    private static final int AUDIT_TRIM_EVERY = 64;
    /** Audit inserts since the last trim; only touched from the writer thread. */
    private int auditSinceTrim;

    public MongoStorage(String uri, String databaseName, Logger logger, long cutoffMillis,
                        int maxItems, int auditHistory) {
        this.logger = logger;
        this.cutoffMillis = cutoffMillis;
        this.maxItems = maxItems;
        this.auditHistory = Math.max(100, auditHistory);
        ConnectionHolder holder = connect(uri);
        this.client = holder.client;
        this.database = holder.client.getDatabase(databaseName == null || databaseName.isEmpty()
                ? holder.defaultDatabase : databaseName);
        this.recovery = database.getCollection("timeditem_recovery");
        this.audit = database.getCollection("timeditem_audit");
        this.stats = database.getCollection("timeditem_stats");
    }

    /** Database name from the URI path when present (the config value overrides it). */
    private static final class ConnectionHolder {
        private final MongoClient client;
        private final String defaultDatabase;

        private ConnectionHolder(MongoClient client, String defaultDatabase) {
            this.client = client;
            this.defaultDatabase = defaultDatabase;
        }
    }

    private static ConnectionHolder connect(String uri) {
        ConnectionString connectionString = new ConnectionString(uri);
        String defaultDatabase = connectionString.getDatabase() == null
                ? "timeditem" : connectionString.getDatabase();
        MongoClient client = MongoClients.create(connectionString);
        // force the first round trip here so a dead server surfaces as a
        // clean fallback in StorageFactory, not as a scattered warning later
        client.getDatabase(defaultDatabase).runCommand(new Document("ping", 1));
        return new ConnectionHolder(client, defaultDatabase);
    }

    @Override
    public String id() {
        return "mongodb";
    }

    @Override
    public List<RecoveryRecord> loadRecovery() {
        recovery.deleteMany(Filters.lt("time_millis", System.currentTimeMillis() - cutoffMillis));
        List<RecoveryRecord> out = new ArrayList<RecoveryRecord>();
        for (Document doc : recovery.find()
                .sort(Sorts.descending("time_millis"))
                .limit(maxItems)) {
            out.add(new RecoveryRecord(doc.getString("_id"), doc.getLong("time_millis"),
                    doc.getString("player_uuid"), doc.getString("player_name"),
                    doc.getString("world"), doc.getString("format"), doc.getString("data")));
        }
        return out;
    }

    @Override
    public void insertRecovery(RecoveryRecord record) {
        Document doc = new Document("_id", record.getId())
                .append("time_millis", record.getTimeMillis())
                .append("player_uuid", record.getPlayerUuid())
                .append("player_name", record.getPlayerName())
                .append("world", record.getWorld())
                .append("format", record.getFormat())
                .append("data", record.getBase64());
        recovery.replaceOne(Filters.eq("_id", record.getId()), doc,
                new ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteRecovery(String id) {
        recovery.deleteOne(Filters.eq("_id", id));
    }

    @Override
    public List<RecoveryRecord> loadRecoveryFor(String playerUuid, int limit) {
        List<RecoveryRecord> out = new ArrayList<RecoveryRecord>();
        for (Document doc : recovery.find(Filters.eq("player_uuid", playerUuid))
                .sort(Sorts.descending("time_millis"))
                .limit(Math.max(1, limit))) {
            out.add(new RecoveryRecord(doc.getString("_id"), doc.getLong("time_millis"),
                    doc.getString("player_uuid"), doc.getString("player_name"),
                    doc.getString("world"), doc.getString("format"), doc.getString("data")));
        }
        return out;
    }

    @Override
    public int countRecoveryFor(String playerUuid) {
        return (int) recovery.countDocuments(Filters.eq("player_uuid", playerUuid));
    }

    @Override
    public void appendAudit(AuditRecord record) {
        audit.insertOne(new Document("time_millis", record.getTimeMillis())
                .append("actor_uuid", record.getActorUuid())
                .append("actor_name", record.getActorName())
                .append("action", record.getAction())
                .append("target", record.getTarget())
                .append("item_uid", record.getItemUid())
                .append("item_id", record.getItemId())
                .append("details", record.getDetails()));
        // countDocuments + border query + deleteMany are up to three round
        // trips - amortize them to every AUDIT_TRIM_EVERYth insert; the
        // collection may exceed the cap by up to that many documents,
        // which auditPage never shows (it sorts newest first with a limit)
        if (++auditSinceTrim < AUDIT_TRIM_EVERY) {
            return;
        }
        auditSinceTrim = 0;
        long count = audit.countDocuments();
        if (count > auditHistory) {
            Document border = audit.find()
                    .sort(Sorts.descending("time_millis"))
                    .skip(auditHistory - 1)
                    .first();
            if (border != null) {
                Long borderTime = border.getLong("time_millis");
                if (borderTime != null) {
                    audit.deleteMany(Filters.lt("time_millis", borderTime));
                }
            }
        }
    }

    @Override
    public List<AuditRecord> auditPage(int pageSize, int pageIndex) {
        List<AuditRecord> out = new ArrayList<AuditRecord>();
        FindIterable<Document> query = audit.find()
                .sort(Sorts.orderBy(Sorts.descending("time_millis"), Sorts.descending("_id")))
                .skip(Math.max(0, pageIndex) * pageSize)
                .limit(pageSize);
        for (Document doc : query) {
            Long time = doc.getLong("time_millis");
            out.add(new AuditRecord(time == null ? 0L : time, doc.getString("actor_uuid"),
                    doc.getString("actor_name"), doc.getString("action"), doc.getString("target"),
                    doc.getString("item_uid"), doc.getString("item_id"), doc.getString("details")));
        }
        return out;
    }

    @Override
    public void incrementExpired(String day, String material) {
        stats.updateOne(Filters.and(Filters.eq("day", day), Filters.eq("material", material)),
                Updates.combine(Updates.setOnInsert("day", day),
                        Updates.setOnInsert("material", material), Updates.inc("count", 1L)),
                new UpdateOptions().upsert(true));
    }

    @Override
    public Map<String, Map<String, Long>> expiredStats(String sinceDay) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<String, Map<String, Long>>();
        for (Document doc : stats.find(Filters.gte("day", sinceDay)).sort(Sorts.ascending("day"))) {
            String day = doc.getString("day");
            String material = doc.getString("material");
            Long count = doc.getLong("count");
            if (day == null || material == null || count == null) {
                continue;
            }
            Map<String, Long> perDay = out.get(day);
            if (perDay == null) {
                perDay = new LinkedHashMap<String, Long>();
                out.put(day, perDay);
            }
            perDay.put(material, count);
        }
        return out;
    }

    @Override
    public void close() {
        client.close();
    }
}
