package net.perfectdreams.dreamnetworkbans.listeners

import com.github.salomonbrys.kotson.nullObj
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.obj
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.event.SettingsChangedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.DreamUtils
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
import java.util.regex.Pattern

class LoginListener(val m: DreamNetworkBans) : Listener {
	@EventHandler
	fun onLogin(event: LoginEvent) {
		val pattern = Pattern.compile("[a-zA-Z0-9_]{3,16}")
		val matcher = pattern.matcher(event.connection.name)

		if (!matcher.find()) {
			event.isCancelled = true
			event.setCancelReason("""
				§cSeu nickname não atende aos critérios necessários!

				§cProvavelmente:
				§c - Seu nickname contém caracteres especiais (como $, #, @, %, etc)
				§c - Seu nickname contém espaços
				§c - Seu nickname contém mais de 16 caracteres
				§c - Seu nickname contém menos de 3 caracteres
			""".trimIndent().toTextComponent())
			return
		}

		if (event.connection.name.toLowerCase() in m.youtuberNames) {
			event.isCancelled = true
			event.setCancelReason("""
				§eVocê parece ser alguém famoso...
					|
					|§aCaso você seja §b${event.connection.name}§a, por favor, mande um email confirmando a sua identidade para §3leonardomalaman@gmail.com§a, obrigado! :)
					|
					|§aSei que é chato, mas sempre existem aquelas pessoas mal intencionadas que tentam se passar por YouTubers... :(
					""".trimMargin().toTextComponent())
			return
		}

		event.registerIntent(m)
		
		m.proxy.scheduler.runAsync(m) {
			val staffIps = DreamUtils.jsonParser.parse(m.staffIps.readText(Charsets.UTF_8)).obj
			val entry = staffIps[event.connection.name.toString()].nullString

			if (entry != null) {
				if (event.connection.virtualHost.hostString != entry) {
					transaction(Databases.databaseNetwork) {
						IpBan.new {
							this.ip = event.connection.address.hostString
							this.punisherName = "Pantufa"
							this.punishedBy = null
							this.punishedAt = System.currentTimeMillis()
							this.reason = "Tentar entrar com uma conta de um membro da equipe.\nMais sorte da próxima vez!"
						}

						val geoLocalizations = GeoLocalization.find {
							GeoLocalizations.ip eq event.connection.address.hostString
						}.toList()

						val uids = geoLocalizations.distinctBy { it.player }.map { it.player }
						uids.forEach {
							Ban.new {
								this.player = it
								this.punisherName = "Pantufa"
								this.punishedBy = null
								this.punishedAt = System.currentTimeMillis()
								this.reason = "Tentar entrar com uma conta de um membro da equipe.\nMais sorte da próxima vez!"
							}
						}
					}

					event.completeIntent(m)
					return@runAsync
				}
			}

			val geoLocalization = transaction(Databases.databaseNetwork) {
				GeoLocalization.find { GeoLocalizations.player eq event.connection.uniqueId }.firstOrNull()
			}
			
			if (geoLocalization == null) {
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
			
			val ban = transaction(Databases.databaseNetwork) {
				Ban.find { Bans.player eq event.connection.uniqueId }.firstOrNull()
			}

			if (ban != null) {
				if (ban.temporary && ban.expiresAt!! < System.currentTimeMillis()) {
					transaction(Databases.databaseNetwork) {
						ban.delete()
					}
					
					event.completeIntent(m)
					return@runAsync
				}
				
				event.isCancelled = true
				event.setCancelReason("""
					§cVocê foi ${if (ban.temporary) "temporariamente " else ""}banido!
					§cMotivo:
					
					§a${ban.reason}
					§cPor: ${ban.punisherName}
					${if (ban.temporary) "§c Expira em: §e${DateUtils.formatDateDiff(ban.expiresAt!!)}" else ""}
				""".trimIndent().toTextComponent())
				
				event.completeIntent(m)
				return@runAsync
			}
			
			val ipBan = transaction(Databases.databaseNetwork) {
				IpBan.find { IpBans.ip eq event.connection.address.hostString }.firstOrNull()
			}
			
			if (ipBan != null) {
				if (ipBan.temporary && ipBan.expiresAt!! < System.currentTimeMillis()) {
					transaction(Databases.databaseNetwork) {
						ipBan.delete()
					}
					
					event.completeIntent(m)
					return@runAsync
				}
				
				event.isCancelled = true
				event.setCancelReason("""
					§cVocê foi ${if (ipBan.temporary) "temporariamente " else ""}banido!
					§cMotivo:
					
					§a${ipBan.reason}
					§cPor: ${ipBan.punisherName}
					${if (ipBan.temporary) "§c Expira em: §e${DateUtils.formatDateDiff(ipBan.expiresAt!!)}" else ""}
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
				this.createdAt = System.currentTimeMillis()
				this.isForgeUser = event.player.isForgeUser
				this.chatMode = event.player.chatMode
				this.mainHand = event.player.mainHand
				this.language = event.player.locale.language
				this.viewDistance = event.player.viewDistance.toInt()
				this.version = ProtocolSupportAPI.getProtocolVersion(event.player).name
				
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
