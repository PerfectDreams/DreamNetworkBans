package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.libs.com.mongodb.client.model.Filters

class UnbanCommand(val m: DreamNetworkBans) : AbstractCommand("unban", permission = "dreamnetworkbans.unban") {
	
	@Subcommand
    fun unban(sender: CommandSender, playerName: String) {
		val foundBan = m.bansColl.find(
				Filters.eq("playerName", playerName)
		).firstOrNull() ?: return sender.sendMessage("§cEste jogador não está banido!".toTextComponent())
		
		m.bansColl.deleteOne(Filters.eq("_id", foundBan.uuid))
		sender.sendMessage("§a${foundBan.playerName} (${foundBan.uuid}) desbanido com sucesso!".toTextComponent())
	}
}