package me.albert.timeditem.storage;

/**
 * One audit trail entry: who did what to which timed item. Written by
 * the /ti commands (set/add/pause/resume/remove/give/restore). Pure
 * data so every backend can store it and tests can build it freely.
 */
public final class AuditRecord {

    /** Action names, also the values stored in SQL/Mongo. */
    public static final String SET = "set";
    public static final String ADD = "add";
    public static final String PAUSE = "pause";
    public static final String RESUME = "resume";
    public static final String REMOVE = "remove";
    public static final String GIVE = "give";
    public static final String RESTORE = "restore";

    private final long timeMillis;
    private final String actorUuid; // null = console
    private final String actorName;
    private final String action; // one of the constants above
    private final String target; // affected player, or null
    private final String itemUid; // TimedItem uid of the touched item, or null
    private final String itemId; // plugin:id for custom items, else the material name
    private final String details; // free-form, e.g. the duration

    public AuditRecord(long timeMillis, String actorUuid, String actorName, String action,
                       String target, String itemUid, String itemId, String details) {
        this.timeMillis = timeMillis;
        this.actorUuid = actorUuid;
        this.actorName = actorName;
        this.action = action;
        this.target = target;
        this.itemUid = itemUid;
        this.itemId = itemId;
        this.details = details;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public String getActorUuid() {
        return actorUuid;
    }

    public String getActorName() {
        return actorName;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getItemUid() {
        return itemUid;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDetails() {
        return details;
    }
}
