package dev.laarryy.Eris.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.io.File;

public class EmojiManager {
    private static final Logger logger = LogManager.getLogger(EmojiManager.class);
    String boosted;
    String membersOnline;
    String membersOffline;
    String serverRole;
    String serverCategory;
    String newsChannel;
    String voiceChannel;
    String storeChannel;
    String textChannel;
    String stageChannel;
    String userMute;
    String userCase;
    String userWarn;
    String messageCensor;
    String userBan;
    String messageEdit;
    String messageDelete;
    String userIdentification;
    String userLeave;
    String userKick;
    String userJoin;
    String moderatorBadge;
    String nitroBadge;
    String employeeBadge;
    String serverBoostBadge;
    String developerBadge;
    String bugHunter2Badge;
    String earlySupporterBadge;
    String balanceBadge;
    String bugHunter1Badge;
    String hypeSquadBadge;
    String partnerBadge;
    String hypeSquad2Badge;
    String braveryBadge;
    String brillianceBadge;
    String triangleNitroBadge;

    private static final ConfigLoader configLoader = new ConfigLoader();
    private static EmojiConfig emojiConfig;


    static {
        try {
            emojiConfig = configLoader.loadEmojiConfig("emojiConfig.yml");
        } catch (ConfigurateException e) {
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
        }
    }

    public void loadEmojiConfig() {

        File configFile = new File("/", "emojiConfig.yml");
        configFile.mkdir();

        this.boosted = emojiConfig.getBoosted();
        this.membersOnline = emojiConfig.getMembersOnline();
        this.membersOffline = emojiConfig.getMembersOffline();
        this.serverRole = emojiConfig.getServerRole();
        this.serverCategory = emojiConfig.getServerCategory();
        this.newsChannel = emojiConfig.getNewsChannel();
        this.voiceChannel = emojiConfig.getVoiceChannel();
        this.storeChannel = emojiConfig.getStoreChannel();
        this.textChannel = emojiConfig.getTextChannel();
        this.stageChannel = emojiConfig.getStageChannel();
        this.userMute = emojiConfig.getUserMute();
        this.userCase = emojiConfig.getUserCase();
        this.userWarn = emojiConfig.getUserWarn();
        this.messageCensor = emojiConfig.getMessageCensor();
        this.userBan = emojiConfig.getUserBan();
        this.messageEdit = emojiConfig.getMessageEdit();
        this.messageDelete = emojiConfig.getMessageDelete();
        this.userIdentification = emojiConfig.getUserIdentification();
        this.userLeave = emojiConfig.getUserLeave();
        this.userKick = emojiConfig.getUserKick();
        this.userJoin = emojiConfig.getUserJoin();
        this.moderatorBadge = emojiConfig.getModeratorBadge();
        this.nitroBadge = emojiConfig.getNitroBadge();
        this.employeeBadge = emojiConfig.getEmployeeBadge();
        this.serverBoostBadge = emojiConfig.getServerBoostBadge();
        this.developerBadge = emojiConfig.getDeveloperBadge();
        this.bugHunter2Badge = emojiConfig.getBugHunter2Badge();
        this.earlySupporterBadge = emojiConfig.getEarlySupporterBadge();
        this.balanceBadge = emojiConfig.getBalanceBadge();
        this.bugHunter1Badge = emojiConfig.getBugHunter1Badge();
        this.hypeSquadBadge = emojiConfig.getHypeSquadBadge();
        this.partnerBadge = emojiConfig.getPartnerBadge();
        this.hypeSquad2Badge = emojiConfig.getHypeSquad2Badge();
        this.braveryBadge = emojiConfig.getBraveryBadge();
        this.brillianceBadge = emojiConfig.getBrillianceBadge();
        this.triangleNitroBadge = emojiConfig.getTriangleNitroBadge();
    }

    public static String getServerCategory() {
        return emojiConfig.getServerCategory();
        }

    public static String getServerRole() {
        return emojiConfig.getServerRole();
    }

    public static String getMembersOffline() {
        return emojiConfig.getMembersOffline();
    }

    public static String getMembersOnline() {
        return emojiConfig.getMembersOnline();
    }

    public static String getBoosted() {
        return emojiConfig.getBoosted();
    }

    public static String getNewsChannel() {
        return emojiConfig.getNewsChannel();
    }

    public static String getVoiceChannel() {
        return emojiConfig.getVoiceChannel();
    }

    public static String getStoreChannel() {
        return emojiConfig.getStoreChannel();
    }

    public static String getTextChannel() {
        return emojiConfig.getTextChannel();
    }

    public static String getStageChannel() {
        return emojiConfig.getStageChannel();
    }

    public static String getInvite() {
        return emojiConfig.getInvite();
    }

    public static String getUserMute() {
        return emojiConfig.getUserMute();
    }

    public static String getUserCase() {
        return emojiConfig.getUserCase();
    }

    public static String getUserWarn() {
        return emojiConfig.getUserWarn();
    }

    public static String getMessageCensor() {
        return emojiConfig.getMessageCensor();
    }

    public static String getUserBan() {
        return emojiConfig.getUserBan();
    }

    public static String getMessageEdit() {
        return emojiConfig.getMessageEdit();
    }

    public static String getMessageDelete() {
        return emojiConfig.getMessageDelete();
    }

    public static String getUserIdentification() {
        return emojiConfig.getUserIdentification();
    }

    public static String getUserLeave() {
        return emojiConfig.getUserLeave();
    }

    public static String getUserKick() {
        return emojiConfig.getUserKick();
    }

    public static String getUserJoin() {
        return emojiConfig.getUserJoin();
    }

    public static String getModeratorBadge() {
        return emojiConfig.getModeratorBadge();
    }

    public static String getNitroBadge() {
        return emojiConfig.getNitroBadge();
    }

    public static String getEmployeeBadge() {
        return emojiConfig.getEmployeeBadge();
    }

    public static String getServerBoostBadge() {
        return emojiConfig.getServerBoostBadge();
    }

    public static String getDeveloperBadge() {
        return emojiConfig.getDeveloperBadge();
    }

    public static String getBugHunter2Badge() {
        return emojiConfig.getBugHunter2Badge();
    }

    public static String getEarlySupporterBadge() {
        return emojiConfig.getEarlySupporterBadge();
    }

    public static String getBalanceBadge() {
        return emojiConfig.getBalanceBadge();
    }

    public static String getBugHunter1Badge() {
        return emojiConfig.getBugHunter1Badge();
    }

    public static String getHypeSquadBadge() {
        return emojiConfig.getHypeSquadBadge();
    }

    public static String getPartnerBadge() {
        return emojiConfig.getPartnerBadge();
    }

    public static String getHypeSquad2Badge() {
        return emojiConfig.getHypeSquad2Badge();
    }

    public static String getBraveryBadge() {
        return emojiConfig.getBraveryBadge();
    }

    public static String getBrillianceBadge() {
        return emojiConfig.getBrillianceBadge();
    }

    public static String getTriangleNitroBadge() {
        return emojiConfig.getTriangleNitroBadge();
    }
}
