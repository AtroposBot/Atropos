package dev.laarryy.Eris.managers;

import dev.laarryy.Eris.commands.punishments.PunishmentManager;

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
