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
package us.myles.ViaVersion.sponge.listeners.protocol1_9to1_8.sponge4;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaListener;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.ArmorType;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.Protocol1_9To1_8;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

public class Sponge4ArmorListener extends ViaListener {
    private static Field entityIdField;

    private static final UUID ARMOR_ATTRIBUTE = UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150");

    public Sponge4ArmorListener() {
        super(Protocol1_9To1_8.class);
    }

    //
    public void sendArmorUpdate(Player player) {
        // Ensure that the player is on our pipe
        if (!isOnPipe(player.getUniqueId())) return;


        int armor = 0;
        armor += calculate(player.getHelmet());
        armor += calculate(player.getChestplate());
        armor += calculate(player.getLeggings());
        armor += calculate(player.getBoots());

        PacketWrapper wrapper = new PacketWrapper(0x4B, null, getUserConnection(player.getUniqueId()));
        try {
            wrapper.write(Type.VAR_INT, getEntityId(player)); // Player ID
            wrapper.write(Type.INT, 1); // only 1 property
            wrapper.write(Type.STRING, "generic.armor");
            wrapper.write(Type.DOUBLE, 0D); //default 0 armor
            wrapper.write(Type.VAR_INT, 1); // 1 modifier
            wrapper.write(Type.UUID, ARMOR_ATTRIBUTE); // armor modifier uuid
            wrapper.write(Type.DOUBLE, (double) armor); // the modifier value
            wrapper.write(Type.BYTE, (byte) 0);// the modifier operation, 0 is add number

            wrapper.send(Protocol1_9To1_8.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int calculate(Optional<ItemStack> itemStack) {
        if (itemStack.isPresent())
            return ArmorType.findByType(itemStack.get().getItem().getType().getId()).getArmorPoints();

        return 0;
    }

    @Listener
    public void onInventoryClick(ClickInventoryEvent e, @Root Player player) {
        for (SlotTransaction transaction : e.getTransactions()) {
            if (ArmorType.isArmor(transaction.getFinal().getType().getId()) ||
                    ArmorType.isArmor(e.getCursorTransaction().getFinal().getType().getId())) {
                sendDelayedArmorUpdate(player);
                break;
            }
        }
    }

    @Listener
    public void onInteract(InteractEvent event, @Root Player player) {
        if (player.getItemInHand().isPresent()) {
            if (ArmorType.isArmor(player.getItemInHand().get().getItem().getId()))
                sendDelayedArmorUpdate(player);
        }
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join e) {
        sendArmorUpdate(e.getTargetEntity());
    }

    @Listener
    public void onRespawn(RespawnPlayerEvent e) {
        sendDelayedArmorUpdate(e.getTargetEntity());
    }

    @Listener
    public void onWorldChange(DisplaceEntityEvent.Teleport e) {
        if (!(e.getTargetEntity() instanceof Player)) return;
        if (!e.getFromTransform().getExtent().getUniqueId().equals(e.getToTransform().getExtent().getUniqueId())) {
            sendArmorUpdate((Player) e.getTargetEntity());
        }
    }

    public void sendDelayedArmorUpdate(final Player player) {
        if (!isOnPipe(player.getUniqueId())) return; // Don't start a task if the player is not on the pipe
        Via.getPlatform().runSync(new Runnable() {
            @Override
            public void run() {
                sendArmorUpdate(player);
            }
        });
    }

    @Override
    public void register() {
        if (isRegistered()) return;

        Sponge.getEventManager().registerListeners(Via.getPlatform(), this);
        setRegistered(true);
    }

    protected int getEntityId(Player p) {
        try {
            if (entityIdField == null) {
                entityIdField = p.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("field_145783_c");
                entityIdField.setAccessible(true);
            }

            return entityIdField.getInt(p);
        } catch (Exception e) {
            Via.getPlatform().getLogger().severe("Could not get the entity id, please report this on our Github");
            e.printStackTrace();
        }

        Via.getPlatform().getLogger().severe("Could not get the entity id, please report this on our Github");
        return -1;
    }
}
