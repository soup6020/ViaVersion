package us.myles.ViaVersion.bukkit.platform;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import us.myles.ViaVersion.ViaVersionPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.platform.ViaPlatformLoader;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.bukkit.classgenerator.ClassGenerator;
import us.myles.ViaVersion.bukkit.listeners.UpdateListener;
import us.myles.ViaVersion.bukkit.listeners.multiversion.PlayerSneakListener;
import us.myles.ViaVersion.bukkit.listeners.protocol1_15to1_14_4.EntityToggleGlideListener;
import us.myles.ViaVersion.bukkit.listeners.protocol1_9to1_8.ArmorListener;
import us.myles.ViaVersion.bukkit.listeners.protocol1_9to1_8.BlockListener;
import us.myles.ViaVersion.bukkit.listeners.protocol1_9to1_8.DeathListener;
import us.myles.ViaVersion.bukkit.listeners.protocol1_9to1_8.HandItemCache;
import us.myles.ViaVersion.bukkit.listeners.protocol1_9to1_8.PaperPatch;
import us.myles.ViaVersion.bukkit.providers.BukkitBlockConnectionProvider;
import us.myles.ViaVersion.bukkit.providers.BukkitInventoryQuickMoveProvider;
import us.myles.ViaVersion.bukkit.providers.BukkitViaBulkChunkTranslator;
import us.myles.ViaVersion.bukkit.providers.BukkitViaMovementTransmitter;
import us.myles.ViaVersion.protocols.protocol1_12to1_11_1.providers.InventoryQuickMoveProvider;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.blockconnections.ConnectionData;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.blockconnections.providers.BlockConnectionProvider;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.providers.BulkChunkTranslatorProvider;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.providers.HandItemProvider;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.providers.MovementTransmitterProvider;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BukkitViaLoader implements ViaPlatformLoader {
    private final ViaVersionPlugin plugin;

    private final Set<Listener> listeners = new HashSet<>();
    private final Set<BukkitTask> tasks = new HashSet<>();

    private HandItemCache handItemCache;

    public BukkitViaLoader(ViaVersionPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(storeListener(listener), plugin);
    }

    public <T extends Listener> T storeListener(T listener) {
        listeners.add(listener);
        return listener;
    }

    @Override
    public void load() {
        // Update Listener
        registerListener(new UpdateListener());

        /* Base Protocol */
        final ViaVersionPlugin plugin = (ViaVersionPlugin) Bukkit.getPluginManager().getPlugin("ViaVersion");

        // Add ProtocolSupport ConnectListener if necessary.
        ClassGenerator.registerPSConnectListener(plugin);

        /* 1.9 client to 1.8 server */
        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_9.getOriginalVersion()) {
            storeListener(new ArmorListener(plugin)).register();
            storeListener(new DeathListener(plugin)).register();
            storeListener(new BlockListener(plugin)).register();

            if (plugin.getConf().isItemCache()) {
                handItemCache = new HandItemCache();
                tasks.add(handItemCache.runTaskTimerAsynchronously(plugin, 2L, 2L)); // Updates player's items :)
            }
        }

        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_14.getOriginalVersion()) {
            boolean use1_9Fix = plugin.getConf().is1_9HitboxFix() && ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_9.getOriginalVersion();
            if (use1_9Fix || plugin.getConf().is1_14HitboxFix()) {
                try {
                    storeListener(new PlayerSneakListener(plugin, use1_9Fix, plugin.getConf().is1_14HitboxFix())).register();
                } catch (ReflectiveOperationException e) {
                    Via.getPlatform().getLogger().warning("Could not load hitbox fix - please report this on our GitHub");
                    e.printStackTrace();
                }
            }
        }

        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_15.getOriginalVersion()) {
            try {
                Class.forName("org.bukkit.event.entity.EntityToggleGlideEvent");
                storeListener(new EntityToggleGlideListener(plugin)).register();
            } catch (ClassNotFoundException ignored) {
            }
        }

        if ((Bukkit.getVersion().toLowerCase(Locale.ROOT).contains("paper")
                || Bukkit.getVersion().toLowerCase(Locale.ROOT).contains("taco")
                || Bukkit.getVersion().toLowerCase(Locale.ROOT).contains("torch"))
                && ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_12.getOriginalVersion()) {
            plugin.getLogger().info("Enabling Paper/TacoSpigot/Torch patch: Fixes block placement.");
            storeListener(new PaperPatch(plugin)).register();
        }

        /* Providers */
        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_9.getOriginalVersion()) {
            Via.getManager().getProviders().use(BulkChunkTranslatorProvider.class, new BukkitViaBulkChunkTranslator());
            Via.getManager().getProviders().use(MovementTransmitterProvider.class, new BukkitViaMovementTransmitter());

            Via.getManager().getProviders().use(HandItemProvider.class, new HandItemProvider() {
                @Override
                public Item getHandItem(final UserConnection info) {
                    if (handItemCache != null) {
                        return handItemCache.getHandItem(info.getProtocolInfo().getUuid());
                    }
                    try {
                        return Bukkit.getScheduler().callSyncMethod(Bukkit.getPluginManager().getPlugin("ViaVersion"), () -> {
                            UUID playerUUID = info.getProtocolInfo().getUuid();
                            Player player = Bukkit.getPlayer(playerUUID);
                            if (player != null) {
                                return HandItemCache.convert(player.getItemInHand());
                            }
                            return null;
                        }).get(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        Via.getPlatform().getLogger().severe("Error fetching hand item: " + e.getClass().getName());
                        if (Via.getManager().isDebug())
                            e.printStackTrace();
                        return null;
                    }
                }
            });
        }

        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_12.getOriginalVersion()) {
            if (plugin.getConf().is1_12QuickMoveActionFix()) {
                Via.getManager().getProviders().use(InventoryQuickMoveProvider.class, new BukkitInventoryQuickMoveProvider());
            }
        }
        if (ProtocolRegistry.SERVER_PROTOCOL < ProtocolVersion.v1_13.getOriginalVersion()) {
            if (Via.getConfig().getBlockConnectionMethod().equalsIgnoreCase("world")) {
                BukkitBlockConnectionProvider blockConnectionProvider = new BukkitBlockConnectionProvider();
                Via.getManager().getProviders().use(BlockConnectionProvider.class, blockConnectionProvider);
                ConnectionData.blockConnectionProvider = blockConnectionProvider;
            }
        }
    }

    @Override
    public void unload() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }
}
