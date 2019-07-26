package net.perfectdreams.dreamnetworkbans.listeners

import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PreLoginEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.event.SettingsChangedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
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
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import protocolsupport.api.ProtocolSupportAPI
import java.util.regex.Pattern

class LoginListener(val m: DreamNetworkBans) : Listener {
	@EventHandler
	fun onPreLogin(event: PreLoginEvent) {
		val playerNames = m.proxy.players.map { it.name.toLowerCase() }

		if (event.connection.name.toLowerCase() in playerNames) {
			event.isCancelled = true
			event.setCancelReason("§cJá há um player com o nome §e${event.connection.name}§c conectado no servidor!".toTextComponent())
			return
		}

		val staffIps = DreamUtils.jsonParser.parse(m.staffIps.readText(Charsets.UTF_8)).obj
		staffIps.entrySet().forEach {
			if (it.key.toLowerCase() == event.connection.name.toLowerCase() && event.connection.virtualHost.hostString != it.value.string) {
				event.registerIntent(m)

				m.proxy.scheduler.runAsync(m) {
					event.isCancelled = true

					val alreadyBanned = transaction(Databases.databaseNetwork) {
						IpBans.select {
							IpBans.ip eq event.connection.address.hostString
						}.count() != 0
					}

					if (!alreadyBanned)
						m.proxy.pluginManager.dispatchCommand(m.proxy.console, "ipban ${event.connection.address.hostString} Tentar entrar com uma conta de um membro da equipe do SparklyPower. Tenha mais sorte na próxima vez! Porque pelo visto você falhou ^-^")

					// Trollei
					event.setCancelReason("Internal Exception: java.io.IOException: An existing connection was forcibly closed by the remote host".toTextComponent())
					event.completeIntent(m)
					return@runAsync
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	fun onLogin(event: LoginEvent) {
		val pattern = Pattern.compile("[a-zA-Z0-9_]{3,16}")
		val matcher = pattern.matcher(event.connection.name)

		if (!matcher.matches()) {
			event.isCancelled = true
			event.setCancelReason(*"""
				§cSeu nickname não atende aos critérios necessários!

				§cProvavelmente:
				§c - Seu nickname contém caracteres especiais (como $, #, @, %, etc)
				§c - Seu nickname contém espaços
				§c - Seu nickname contém mais de 16 caracteres
				§c - Seu nickname contém menos de 3 caracteres
			""".trimIndent().toBaseComponent())
			return
		}

		if (event.connection.name.toLowerCase() in m.youtuberNames) {
			event.isCancelled = true
			event.setCancelReason(*"""
				§eVocê parece ser alguém famoso...
					|
					|§aCaso você seja §b${event.connection.name}§a, por favor, mande um email confirmando a sua identidade para §3mrpowergamerbr@perfectdreams.net§a, obrigado! :)
					|
					|§aSei que é chato, mas sempre existem aquelas pessoas mal intencionadas que tentam se passar por YouTubers... :(
					""".trimMargin().toBaseComponent())
			return
		}

		event.registerIntent(m)

		m.proxy.scheduler.runAsync(m) {
			val geoLocalization = transaction(Databases.databaseNetwork) {
				GeoLocalization.find { GeoLocalizations.player eq event.connection.uniqueId }.firstOrNull()
			}

			if (geoLocalization == null) {
				// Vamos executar isto em uma thread externa, para evitar problemas
				m.proxy.scheduler.runAsync(m) {
					val loc = GeoUtils.getGeolocalization(event.connection.address.hostString)

					transaction(Databases.databaseNetwork) {
						GeoLocalization.new {
							this.player = event.connection.uniqueId
							this.ip = event.connection.address.hostString

							this.country = loc.country
							this.region = loc.regionName
						}
					}
				}
			}

			val ban = transaction(Databases.databaseNetwork) {
				Ban.find { Bans.player eq event.connection.uniqueId }.firstOrNull()
			}

			if (ban != null) {
				if (ban.temporary && ban.expiresAt!! < System.currentTimeMillis()) {
					event.completeIntent(m)
					return@runAsync
				}

				event.isCancelled = true

				event.setCancelReason(*"""
					§cVocê foi ${if (ban.temporary) "temporariamente " else ""}banido!
					§cMotivo:
					
					§a${ban.reason}
					§cPor: ${PunishmentManager.getPunisherName(ban.punishedBy)}
					${if (ban.temporary) "§c Expira em: §e${DateUtils.formatDateDiff(ban.expiresAt!!)}" else ""}
				""".trimIndent().toBaseComponent())

				event.completeIntent(m)
				return@runAsync
			}

			val ipBan = transaction(Databases.databaseNetwork) {
				IpBan.find { IpBans.ip eq event.connection.address.hostString }.firstOrNull()
			}

			if (ipBan != null) {
				if (ipBan.temporary && ipBan.expiresAt!! < System.currentTimeMillis()) {
					event.completeIntent(m)
					return@runAsync
				}

				event.isCancelled = true
				event.setCancelReason(*"""
					§cVocê foi ${if (ipBan.temporary) "temporariamente " else ""}banido!
					§cMotivo:
					
					§a${ipBan.reason}
					§cPor: ${PunishmentManager.getPunisherName(ipBan.punishedBy)}
					${if (ipBan.temporary) "§c Expira em: §e${DateUtils.formatDateDiff(ipBan.expiresAt!!)}" else ""}
				""".trimIndent().toBaseComponent())

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
				this.createdAt = System.currentTimeMillis()
				this.isForgeUser = event.player.isForgeUser
				this.chatMode = event.player.chatMode
				this.mainHand = event.player.mainHand
				this.language = event.player.locale.language
				this.viewDistance = event.player.viewDistance.toInt()
				this.version = event.player.pendingConnection.version.toString() // ProtocolSupportAPI.getProtocolVersion(event.player).name

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
