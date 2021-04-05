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

    public int getUserId() {
        return getInteger("user_id");
    }

    public void setUserId(int userId) {
        set("user_id", userId);
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

    public @NonNull Instant getDateEntry() {
        return Instant.ofEpochMilli(getLong("punishment_date"));
    }

    public void setDateEntry(@NonNull Instant dateEntry) {
        set("punishment_date", dateEntry);
    }

    public @Nullable Instant getEndDate() {
        return Instant.ofEpochMilli(getLong("punishment_end_date"));
    }

    public void setEndDate(@Nullable Instant endDateEntry) {
        set("punishment_end_date", endDateEntry);
    }

    public @Nullable String getMessage() {
        return getString("punishment_message");
    }

    public void setPunishmentMessage(@Nullable String message) {
        set("punishment_message", message);
    }

    public void createPunishment(
            int userId,
            int serverId,
            @NonNull String punishmentType,
            @NonNull Instant punishmentDate,
            @Nullable Instant punishmentEndDate,
            @Nullable String punishmentMessage
    ) {
        Punishment p = Punishment.createIt();
        p.setUserId(userId);
        p.setServerId(serverId);
        p.setPunishmentType(punishmentType);
        p.setDateEntry(punishmentDate);
        p.setEndDate(punishmentEndDate);
        p.setPunishmentMessage(punishmentMessage);
        p.saveIt();
    }

    public void createPunishment(
            Long userIdSnowflake,
            Long serverIdSnowflake,
            @NonNull String punishmentType,
            @NonNull Instant punishmentDate,
            @Nullable Instant punishmentEndDate,
            @Nullable String punishmentMessage
    ) {
        SnowflakeIdFinder snowflakeIdFinder = new SnowflakeIdFinder();
        Punishment p = Punishment.createIt();
        p.setUserId(snowflakeIdFinder.getUserIdFromSnowflake(userIdSnowflake));
        p.setServerId(snowflakeIdFinder.getServerIdFromSnowflake(serverIdSnowflake));
        p.setPunishmentType(punishmentType);
        p.setDateEntry(punishmentDate);
        p.setEndDate(punishmentEndDate);
        p.setPunishmentMessage(punishmentMessage);
        p.saveIt();
    }

}
