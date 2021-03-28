/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package us.myles.ViaVersion;

import it.unimi.dsi.fastutil.ints.IntSortedSet;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.platform.TaskId;
import us.myles.ViaVersion.api.platform.ViaConnectionManager;
import us.myles.ViaVersion.api.platform.ViaInjector;
import us.myles.ViaVersion.api.platform.ViaPlatform;
import us.myles.ViaVersion.api.platform.ViaPlatformLoader;
import us.myles.ViaVersion.api.platform.providers.ViaProviders;
import us.myles.ViaVersion.api.protocol.ProtocolManager;
import us.myles.ViaVersion.api.protocol.ProtocolManagerImpl;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.api.protocol.ServerProtocolVersion;
import us.myles.ViaVersion.api.protocol.ServerProtocolVersionRange;
import us.myles.ViaVersion.api.protocol.ServerProtocolVersionSingleton;
import us.myles.ViaVersion.commands.ViaCommandHandler;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.TabCompleteThread;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.ViaIdleThread;
import us.myles.ViaVersion.update.UpdateUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViaManagerImpl implements ViaManager {
    private final ProtocolManagerImpl protocolManager = new ProtocolManagerImpl();
    private final ViaConnectionManager connectionManager = new ViaConnectionManager();
    private final ViaProviders providers = new ViaProviders();
    private final ViaPlatform<?> platform;
    private final ViaInjector injector;
    private final ViaCommandHandler commandHandler;
    private final ViaPlatformLoader loader;
    private final Set<String> subPlatforms = new HashSet<>();
    private List<Runnable> enableListeners = new ArrayList<>();
    private TaskId mappingLoadingTask;
    private boolean debug;

    public ViaManagerImpl(ViaPlatform<?> platform, ViaInjector injector, ViaCommandHandler commandHandler, ViaPlatformLoader loader) {
        this.platform = platform;
        this.injector = injector;
        this.commandHandler = commandHandler;
        this.loader = loader;
    }

    public static ViaManagerBuilder builder() {
        return new ViaManagerBuilder();
    }

    public void init() {
        if (System.getProperty("ViaVersion") != null) {
            // Reload?
            platform.onReload();
        }
        // Check for updates
        if (platform.getConf().isCheckForUpdates()) {
            UpdateUtil.sendUpdateMessage();
        }

        // Load supported protocol versions if we can
        if (!injector.lateProtocolVersionSetting()) {
            loadServerProtocol();
        }

        // Register protocols
        protocolManager.registerProtocols();

        // Inject
        try {
            injector.inject();
        } catch (Exception e) {
            platform.getLogger().severe("ViaVersion failed to inject:");
            e.printStackTrace();
            return;
        }

        // Mark as injected
        System.setProperty("ViaVersion", platform.getPluginVersion());

        for (Runnable listener : enableListeners) {
            listener.run();
        }
        enableListeners = null;

        // If successful
        platform.runSync(this::onServerLoaded);
    }

    public void onServerLoaded() {
        // Load Server Protocol
        try {
            ProtocolRegistry.SERVER_PROTOCOL = ProtocolVersion.getProtocol(injector.getServerProtocolVersion()).getOriginalVersion();
        } catch (Exception e) {
            platform.getLogger().severe("ViaVersion failed to get the server protocol!");
            e.printStackTrace();}
        if (!protocolManager.getServerProtocolVersion().isKnown()) {
            // Try again
            loadServerProtocol();
        }

        // Check if there are any pipes to this version
        ServerProtocolVersion protocolVersion = protocolManager.getServerProtocolVersion();
        if (protocolVersion.isKnown()) {
            if (platform.isProxy()) {
                platform.getLogger().info("ViaVersion detected lowest supported versions by the proxy: "
                        + ProtocolVersion.getProtocol(protocolVersion.lowestSupportedVersion())
                        + "\nHighest supported version by the proxy: "
                        + ProtocolVersion.getProtocol(protocolVersion.highestSupportedVersion()));
                if (debug) {
                    platform.getLogger().info("Supported version range: " + Arrays.toString(protocolVersion.supportedVersions().toArray(new int[0])));
                }
            } else {
                platform.getLogger().info("ViaVersion detected server version: " + ProtocolVersion.getProtocol(protocolVersion.highestSupportedVersion()));
            }

            if (!protocolManager.isWorkingPipe()) {
                platform.getLogger().warning("ViaVersion does not have any compatible versions for this server version!");
                platform.getLogger().warning("Please remember that ViaVersion only adds support for versions newer than the server version.");
                platform.getLogger().warning("If you need support for older versions you may need to use one or more ViaVersion addons too.");
                platform.getLogger().warning("In that case please read the ViaVersion resource page carefully or use https://jo0001.github.io/ViaSetup");
                platform.getLogger().warning("and if you're still unsure, feel free to join our Discord-Server for further assistance.");
            } else if (ProtocolRegistry.SERVER_PROTOCOL <= ProtocolVersion.v1_12_2.getOriginalVersion() && !platform.isProxy()) {
                platform.getLogger().warning("This version of Minecraft is extremely outdated and support for it has reached its end of life. "
                        + "You will still be able to run Via on this version, but we are unlikely to provide any further fixes or help with problems specific to legacy versions. "
                        + "Please consider updating to give your players a better experience and to avoid issues that have long been fixed.");
            }
        }

        // Load Listeners / Tasks
        protocolManager.onServerLoaded();

        // Load Platform
        loader.load();
        // Common tasks
        mappingLoadingTask = Via.getPlatform().runRepeatingSync(() -> {
            if (protocolManager.checkForMappingCompletion()) {
                platform.cancelTask(mappingLoadingTask);
                mappingLoadingTask = null;
            }
        }, 10L);
        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_9.getOriginalVersion()) {

        int serverProtocolVersion = protocolManager.getServerProtocolVersion().lowestSupportedVersion();
            if (Via.getConfig().isSimulatePlayerTick()) {
                Via.getPlatform().runRepeatingSync(new ViaIdleThread(), 1L);
            }
        }
        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_13.getOriginalVersion()) {
            if (Via.getConfig().get1_13TabCompleteDelay() > 0) {
                Via.getPlatform().runRepeatingSync(new TabCompleteThread(), 1L);
            }
        }

        // Refresh Versions
        protocolManager.refreshVersions();
    }

    private void loadServerProtocol() {
        try {
            ProtocolVersion serverProtocolVersion = ProtocolVersion.getProtocol(injector.getServerProtocolVersion());
            ServerProtocolVersion versionInfo;
            if (platform.isProxy()) {
                IntSortedSet supportedVersions = injector.getServerProtocolVersions();
                versionInfo = new ServerProtocolVersionRange(supportedVersions.firstInt(), supportedVersions.lastInt(), supportedVersions);
            } else {
                versionInfo = new ServerProtocolVersionSingleton(serverProtocolVersion.getVersion());
            }

            protocolManager.setServerProtocol(versionInfo);
        } catch (Exception e) {
            platform.getLogger().severe("ViaVersion failed to get the server protocol!");
            e.printStackTrace();
        }
    }

    public void destroy() {
        // Uninject
        platform.getLogger().info("ViaVersion is disabling, if this is a reload and you experience issues consider rebooting.");
        try {
            injector.uninject();
        } catch (Exception e) {
            platform.getLogger().severe("ViaVersion failed to uninject:");
            e.printStackTrace();
        }

        // Unload
        loader.unload();
    }

    @Override
    public ViaPlatform<?> getPlatform() {
        return platform;
    }

    @Override
    public ViaConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @Override
    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    @Override
    public ViaProviders getProviders() {
        return providers;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public ViaInjector getInjector() {
        return injector;
    }

    @Override
    public ViaCommandHandler getCommandHandler() {
        return commandHandler;
    }

    @Override
    public ViaPlatformLoader getLoader() {
        return loader;
    }

    /**
     * Returns a mutable set of self-added subplatform version strings.
     * This set is expanded by the subplatform itself (e.g. ViaBackwards), and may not contain all running ones.
     *
     * @return mutable set of subplatform versions
     */
    public Set<String> getSubPlatforms() {
        return subPlatforms;
    }

    /**
     * Adds a runnable to be executed when ViaVersion has finished its init before the full server load.
     *
     * @param runnable runnable to be executed
     */
    public void addEnableListener(Runnable runnable) {
        enableListeners.add(runnable);
    }

    public static final class ViaManagerBuilder {
        private ViaPlatform<?> platform;
        private ViaInjector injector;
        private ViaCommandHandler commandHandler;
        private ViaPlatformLoader loader;

        public ViaManagerBuilder platform(ViaPlatform<?> platform) {
            this.platform = platform;
            return this;
        }

        public ViaManagerBuilder injector(ViaInjector injector) {
            this.injector = injector;
            return this;
        }

        public ViaManagerBuilder loader(ViaPlatformLoader loader) {
            this.loader = loader;
            return this;
        }

        public ViaManagerBuilder commandHandler(ViaCommandHandler commandHandler) {
            this.commandHandler = commandHandler;
            return this;
        }

        public ViaManagerImpl build() {
            return new ViaManagerImpl(platform, injector, commandHandler, loader);
        }
    }
}
