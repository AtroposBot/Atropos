package dev.laarryy.Icicle.models.users;

import dev.laarryy.Icicle.utils.SnowflakeIdFinder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.time.Instant;

@Table("punishments")
public class Punishment extends Model {

    public int getPunishmentId() {
        return getInteger("id");
    }

    public int getPunishedUserId() {
        return getInteger("user_id_punished");
    }

    public void setPunishedUserId(int punishedUserId) {
        set("user_id_punished", punishedUserId);
    }

    public int getPunishingUserId() {
        return getInteger("user_id_punisher");
    }

    public void setPunishingUserId(int punishingUserId) {
        set("user_id_punisher", punishingUserId);
    }

    public int getServerId() {
        return getInteger("server_id");
    }

    public void setServerId(int serverId) {
        set("server_id", serverId);
    }

    public @NonNull String getPunishmentType() {
        return getString("punishment_type");
    }

    public void setPunishmentType(@NonNull String punishmentType) {
        set("punishment_type", punishmentType);
    }

    public @NonNull Long getDateEntry() {
        return getLong("punishment_date");
    }

    public void setDateEntry(@NonNull Long dateEntry) {
        set("punishment_date", dateEntry);
    }

    public @Nullable Long getEndDate() {
        return getLong("punishment_end_date");
    }

    public void setEndDate(@Nullable Long endDateEntry) {
        set("punishment_end_date", endDateEntry);
    }

    public @Nullable String getPunishmentMessage() {
        return getString("punishment_message");
    }

    public void setPunishmentMessage(@Nullable String message) {
        set("punishment_message", message);
    }

    public boolean getIfDMed() {
        return getBoolean("did_dm");
    }

    public void setDMed(boolean DMed) {
        setBoolean("did_dm", DMed);
    }

}
