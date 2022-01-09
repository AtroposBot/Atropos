package dev.laarryy.eris.models.users;

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

    public String getPunishedUserName() {
        return getString("name_punished");
    }

    public void setPunishedUserName(String punishedUserName) {
        setString("name_punished", punishedUserName);
    }

    public Integer getPunishedUserDiscrim() {
        return getInteger("discrim_punished");
    }

    public void setPunishedUserDiscrim(Integer discrim) {
        setInteger("discrim_punished", discrim);
    }

    public Integer getPunishingUserId() {
        return getInteger("user_id_punisher");
    }

    public void setPunishingUserId(Integer punishingUserId) {
        set("user_id_punisher", punishingUserId);
    }

    public String getPunishingUserName() {
        return getString("name_punisher");
    }

    public void setPunishingUserName(String punishingUserName) {
        setString("name_punisher", punishingUserName);
    }

    public Integer getPunishingUserDiscrim() {
        return getInteger("discrim_punisher");
    }

    public void setPunishingUserDiscrim(Integer punishingUserDiscrim) {
        setInteger("discrim_punisher", punishingUserDiscrim);
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

    public boolean getPermanent() {
        return getBoolean("permanent");
    }

    public void setPermanent(boolean permanent) {
        setBoolean("permanent", permanent);
    }

    public boolean getAutomatic() {
        return getBoolean("automatic");
    }

    public void setAutomatic(boolean automatic) {
        setBoolean("automatic", automatic);
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

    public Integer getPunishmentEnder() {
        return getInteger("punishment_ender");
    }

    public void setPunishmentEnder(Integer enderId) {
        setInteger("punishment_ender", enderId);
    }

    public String getPunishmentEnderName() {
        return getString("name_punishment_ender");
    }

    public void setPunishmentEnderName(String punishmentEnderName) {
        setString("name_punishment_ender", punishmentEnderName);
    }

    public Integer getPunishmentEnderDiscrim() {
        return getInteger("discrim_punishment_ender");
    }

    public void setPunishmentEnderDiscrim(Integer punishmentEnderDiscrim) {
        setInteger("discrim_punishment_ender", punishmentEnderDiscrim);
    }

    public boolean getAutomaticEnd() {
        return getBoolean("automatic_end");
    }

    public void setAutomaticEnd(boolean automaticEnd) {
        setBoolean("automatic_end", automaticEnd);
    }

    public Integer getBatchId() {
        return getInteger("batch_id");
    }

    public void setBatchId(Integer batchId) {
        setInteger("batch_id", batchId);
    }
}
