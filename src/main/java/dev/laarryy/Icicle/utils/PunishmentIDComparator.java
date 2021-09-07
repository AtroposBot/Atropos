package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;

import java.util.Comparator;

public class PunishmentIDComparator implements Comparator<Punishment> {

    @Override
    public int compare(Punishment firstPunishment, Punishment secondPunishment) {
        DatabaseLoader.openConnectionIfClosed();
        return Integer.compare(firstPunishment.getPunishmentId(), secondPunishment.getPunishmentId());
    }
}
