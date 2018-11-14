package net.perfectdreams.dreamnetworkbans.listeners

import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ChatEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.dao.Mute
import net.perfectdreams.dreamnetworkbans.tables.Mutes
import org.jetbrains.exposed.sql.transactions.transaction

class ChatListener : Listener {
	
	@EventHandler
	fun onChat(event: ChatEvent) {
		val player = event.sender as? ProxiedPlayer
		
		if (player != null) {
			val mute = transaction {
				Mute.find { Mutes.player eq player.uniqueId }.firstOrNull()
			}
			
			if (mute != null) {
				event.isCancelled = true
				
				player.sendMessage("§aVocê está §csilenciado permanentemente§a!".toTextComponent())
				player.sendMessage("§aMotivo: §e${mute.reason}".toTextComponent())
			}
		}
	}
}