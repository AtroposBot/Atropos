package dev.laarryy.atropos.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class ConfigSettings {

    protected @Nullable String address = "nothing";
    protected @Nullable String username = "nope";
    protected @Nullable String password = "nada";
    protected @Nullable String token = "secret";
    protected @Nullable String controlGuildId = "unknown";
    protected @Nullable String controllerId = "unknown";
    protected @Nullable String encryptionKey = "mysterious";

    public @Nullable String getAddress() {
        return address;
    }

    public void setAddress(@Nullable String address) {
        this.address = address;
    }

    public @Nullable String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    public @Nullable String getToken() {
        return token;
    }

    public void setToken(@Nullable String token) { this.token = token; }

    public @Nullable String getControlGuildId() {
        return controlGuildId;
    }

    public void setControlGuildId(@Nullable String controlGuildId) {
        this.controlGuildId = controlGuildId;
    }

    public @Nullable String getControllerId() {
        return controllerId;
    }

    public void setControllerId(@Nullable String controllerId) {
        this.controllerId = controllerId;
    }

    public @Nullable String getEncryptionKey() { return encryptionKey; }

    public void setEncryptionKey(@Nullable String encryptionKey) { this.encryptionKey = encryptionKey; }

}
