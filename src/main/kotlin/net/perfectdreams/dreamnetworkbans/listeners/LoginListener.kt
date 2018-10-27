package net.perfectdreams.dreamnetworkbans.listeners

import com.mongodb.client.model.Filters
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PreLoginEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import java.util.regex.Pattern

class LoginListener(val m: DreamNetworkBans) : Listener {
	
	@EventHandler
	fun onLogin(event: PreLoginEvent) {
		val pattern = Pattern.compile("[a-zA-Z0-9_]{3,16}")
		val matcher = pattern.matcher(event.connection.name)
		
		if (!matcher.find()) {
			event.connection.disconnect("""
				§cSeu nickname não atende aos critérios necessários!
				
				§cProvavelmente:
				§c - Seu nickname contém caracteres especiais (como $, #, @, %, etc)
				§c - Seu nickname contém espaços
				§c - Seu nickname contém mais de 16 caracteres
				§c - Seu nickname contém menos de 3 caracteres
			""".trimIndent().toTextComponent())
			
			return
		}
		
		val foundAccountBan = m.bansColl.find(
				Filters.eq("_id", event.connection.uniqueId.toString())
		).firstOrNull()
		
		if (foundAccountBan != null) {
			event.connection.disconnect("""
				§cVocê foi banido!
				§cMotivo:
				
				§a${foundAccountBan.reason}
				§cPor: ${foundAccountBan.authorName}
			""".trimIndent().toTextComponent())
			
			return
		}
		
		val foundIpBan = m.bansColl.find(
				Filters.eq("ip", event.connection.address.hostString)
		).firstOrNull()
		
		if (foundIpBan != null) {
			if (foundIpBan.isIpBan) {
				event.connection.disconnect("""
					§cVocê foi banido!
					§cMotivo:
					
					§a${foundIpBan.reason}
					§cPor: ${foundIpBan.authorName}
				""".trimIndent().toTextComponent())
				
				return
			}
			
			// O IP ban de uma semana por padrão ainda não expirou!
			if (!foundIpBan.isIpBan && foundIpBan.timestamp + 604_800_000 > System.currentTimeMillis()) {
				event.connection.disconnect("""
					§cVocê foi banido!
					§cMotivo:
					
					§a${foundIpBan.reason}
					§cPor: ${foundIpBan.authorName}
				""".trimIndent().toTextComponent())
				
				return
			}
		}
	}
	
	@EventHandler
	fun onDisconnect(event: ServerKickEvent) {
		// TODO: Remover "event.kickReason", já que está deprecated :whatdog:
		if (event.kickReason.contains("Server closed", true) && event.kickedFrom.name != "sparklypower_lobby") {
			event.player.connect(m.proxy.getServerInfo("sparklypower_lobby"))
		}
	}
}
