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
package us.myles.ViaVersion.commands.defaultsubs;

import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.command.ViaCommandSender;
import us.myles.ViaVersion.api.command.ViaSubCommand;

public class ReloadSubCmd extends ViaSubCommand {
    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String description() {
        return "Reload the config from the disk";
    }

    @Override
    public boolean execute(ViaCommandSender sender, String[] args) {
        Via.getPlatform().getConfigurationProvider().reloadConfig();
        sendMessage(sender, "&6Configuration successfully reloaded! Some features may need a restart.");
        return true;
    }
}
