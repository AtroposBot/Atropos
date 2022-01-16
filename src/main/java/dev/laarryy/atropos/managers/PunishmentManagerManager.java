package dev.laarryy.atropos.managers;

import dev.laarryy.atropos.commands.punishments.PunishmentManager;

public class PunishmentManagerManager {
    private static PunishmentManagerManager instance;
    private final PunishmentManager punishmentManager;

    public PunishmentManagerManager(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    public static PunishmentManagerManager getManager() {
        if (instance == null) {
            instance = new PunishmentManagerManager(new PunishmentManager());
        }
        return instance;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
}
