package net.perfectdreams.dreamnetworkbans.listeners

import com.github.salomonbrys.kotson.*
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.event.SettingsChangedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.perfectdreams.dreamcorebungee.network.socket.SocketReceivedEvent
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.DreamUtils
import net.perfectdreams.dreamcorebungee.utils.extensions.toBaseComponent
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.Fingerprint
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.dao.IpBan
import net.perfectdreams.dreamnetworkbans.tables.Bans
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import net.perfectdreams.dreamnetworkbans.tables.IpBans
import net.perfectdreams.dreamnetworkbans.utils.DateUtils
import net.perfectdreams.dreamnetworkbans.utils.GeoUtils
import org.jetbrains.exposed.sql.transactions.transaction
import protocolsupport.api.ProtocolSupportAPI
import java.util.*
import java.util.regex.Pattern

class SocketListener(val m: DreamNetworkBans) : Listener {
	@EventHandler
	fun onLogin(e: SocketReceivedEvent) {
		val type = e.json["type"].nullString ?: return

		// Exemplo de JSON...
		// { "type": "ping" }

		// Caso aconteça algum erro, { "error": { ... } }
		when (type) {
			"sendAdminChat" -> {
				val player = e.json["player"].string
				val message = e.json["message"].string

				val staff = m.proxy.players.filter { it.hasPermission("dreamnetworkbans.adminchat") }

				val senderName = player

				val color = "§d"

				val tc = "§3[$color(Discord) §l${senderName}§3] §b$message".toTextComponent()

				staff.forEach { it.sendMessage(tc) }
			}
		}
	}
}
