package dev.laarryy.Icicle.models.users;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

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

    public String getPunishmentType() {
        return getString("punishment_type");
    }

    public void setPunishmentType(String punishmentType) {
        set("punishment_type", punishmentType);
    }

    public Long getDateEntry() {
        return getLong("punishment_date");
    }

    public void setDateEntry(Long dateEntry) {
        set("punishment_date", dateEntry);
    }

    public Long getEndDate() {
        return getLong("punishment_end_date");
    }

    public void setEndDate(Long endDateEntry) {
        set("punishment_end_date", endDateEntry);
    }

    public String getPunishmentMessage() {
        return getString("punishment_message");
    }

    public void setPunishmentMessage(String message) {
        set("punishment_message", message);
    }

    public boolean getIfDMed() {
        return getBoolean("did_dm");
    }

    public void setDMed(boolean DMed) {
        setBoolean("did_dm", DMed);
    }

    public boolean getIfEnded() {
        return getBoolean("end_date_passed");
    }

    public void setEnded(boolean ended) {
        setBoolean("end_date_passed", ended);
    }

    public String getEndReason() {
        return getString("punishment_end_reason");
    }

    public void setEndReason(String endReason) {
        setString("punishment_end_reason", endReason);
    }
}
