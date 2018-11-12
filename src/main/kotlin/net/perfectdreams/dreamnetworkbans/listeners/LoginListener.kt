package net.perfectdreams.dreamnetworkbans.listeners

import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PreLoginEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.event.SettingsChangedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.Fingerprint
import net.perfectdreams.dreamnetworkbans.tables.Bans
import net.perfectdreams.libs.com.mongodb.client.model.Filters
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.regex.Pattern

class LoginListener(val m: DreamNetworkBans) : Listener {
	
	@EventHandler
	fun onLogin(event: PreLoginEvent) {
		event.registerIntent(m);
		ProxyServer.getInstance().getScheduler().runAsync(m) {
			val pattern = Pattern.compile("[a-zA-Z0-9_]{3,16}")
			val matcher = pattern.matcher(event.connection.name)

			if (!matcher.find()) {
				event.setCancelReason("""
				§cSeu nickname não atende aos critérios necessários!

				§cProvavelmente:
				§c - Seu nickname contém caracteres especiais (como $, #, @, %, etc)
				§c - Seu nickname contém espaços
				§c - Seu nickname contém mais de 16 caracteres
				§c - Seu nickname contém menos de 3 caracteres
			""".trimIndent().toTextComponent())
				event.completeIntent(m)
				return@runAsync
			}

			val ban = transaction(Databases.databaseNetwork) {
				Ban.find { Bans.player eq event.connection.uniqueId }.firstOrNull()
			}

			if (ban != null) {
				event.setCancelReason("""
				§cVocê foi banido!
				§cMotivo:

				§a${ban.reason}
				§cPor: ${ban.punishedBy}
			""".trimIndent().toTextComponent())
				event.completeIntent(m)
				return@runAsync
			}

			event.completeIntent(m)
		}
	}
	
	@EventHandler
	fun onDisconnect(event: ServerKickEvent) {
		// TODO: Remover "event.kickReason", já que está deprecated :whatdog:
		if (event.kickReason.contains("Server closed", true) && event.kickedFrom.name != "sparklypower_lobby") {
			event.isCancelled = true
			event.cancelServer = m.proxy.getServerInfo("sparklypower_lobby")
		}
	}

	@EventHandler
	fun onSettingsChange(event: SettingsChangedEvent) {
		transaction(Databases.databaseNetwork) {
			Fingerprint.new {
				this.player = event.player.uniqueId
				this.isForgeUser = event.player.isForgeUser
				this.chatMode = event.player.chatMode
				this.mainHand = event.player.mainHand
				this.language = event.player.locale.language
				this.viewDistance = event.player.viewDistance.toInt()
				this.hasCape = event.player.skinParts.hasCape()
				this.hasHat = event.player.skinParts.hasHat()
				this.hasJacket = event.player.skinParts.hasJacket()
				this.hasLeftPants = event.player.skinParts.hasLeftPants()
				this.hasLeftSleeve = event.player.skinParts.hasLeftSleeve()
				this.hasRightPants = event.player.skinParts.hasRightPants()
				this.hasRightSleeve = event.player.skinParts.hasRightSleeve()
			}
		}
	}
}
