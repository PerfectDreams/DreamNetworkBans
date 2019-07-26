package net.perfectdreams.dreamnetworkbans.commands

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.set
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.commands.ArgumentType
import net.perfectdreams.commands.annotation.InjectArgument
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.DreamUtils
import net.perfectdreams.dreamcorebungee.utils.ParallaxEmbed
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.dao.IpBan
import net.perfectdreams.dreamnetworkbans.dao.Warn
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import net.perfectdreams.dreamnetworkbans.tables.Warns
import net.perfectdreams.dreamnetworkbans.utils.DateUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class WarnCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("warn", "avisar"), permission = "dreamnetworkbans.warn") {

	@Subcommand
	fun warn(sender: CommandSender, playerName: String, @InjectArgument(ArgumentType.ALL_ARGUMENTS) reason: String?) {
		val (punishedDisplayName, punishedUniqueId, player) = PunishmentManager.getPunishedInfoByString(playerName) ?: run {
			sender.sendMessage("§cEu sei que você tá correndo para avisar aquele mlk meliante... mas eu não conheço ninguém chamado §b$playerName§c... respira um pouco... fica calmo e VEJA O NOME NOVAMENTE!".toTextComponent())
			return
		}

		if (punishedUniqueId == null) {
			sender.sendMessage("§cNão conheço o UUID desse cara, sorry!".toTextComponent())
			return
		}

		var effectiveReason = reason ?: "Sem motivo definido"

		var silent = false
		if (effectiveReason.endsWith("-s")) {
			effectiveReason = effectiveReason.substring(0, effectiveReason.length - "-s".length)
			
			silent = true
		}

		val punisherDisplayName = PunishmentManager.getPunisherName(sender)

		transaction(Databases.databaseNetwork) {
			Warn.new {
				this.player = punishedUniqueId
				this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
				this.punishedAt = System.currentTimeMillis()
				this.reason = effectiveReason
			}
		}
		
		val warns = transaction(Databases.databaseNetwork) {
			Warn.find { Warns.player eq punishedUniqueId!! }.toList()
		}
		val count = warns.size
		
		val geoLocalization = transaction(Databases.databaseNetwork) {
			GeoLocalization.find { GeoLocalizations.player eq punishedUniqueId!! }.firstOrNull()
		}
		
		// IP do usuário, caso seja encontrado
		val ip = if (player != null)
			player.address.hostString
		else
			geoLocalization?.ip
		
		when (count) {
			2 -> {
				val player = m.proxy.getPlayer(punishedUniqueId!!)
				
				if (player != null) {
					player.disconnect("§cVocê está chegando ao limite de avisos, cuidado!\n§cTotal de avisos: §e$count".toTextComponent())

					PunishmentManager.sendPunishmentToDiscord(
							silent,
							punishedDisplayName ?: "Nome desconhecido",
							punishedUniqueId!!,
							"Expulso",
							punisherDisplayName,
							effectiveReason,
							(sender as? ProxiedPlayer)?.server?.info?.name,
							null
					)
					// announceKick(player.name, player.uniqueId, sender, effectiveReason, silent)
				}
			}
			
			3 -> {
				val player = m.proxy.getPlayer(punishedUniqueId!!)
				
				if (player != null) {
					player.disconnect("§cVocê está chegando ao limite de avisos, cuidado!\n§cTotal de avisos: §e$count".toTextComponent())

					PunishmentManager.sendPunishmentToDiscord(
							silent,
							punishedDisplayName ?: "Nome desconhecido",
							punishedUniqueId!!,
							"Expulso",
							punisherDisplayName,
							effectiveReason,
							(sender as? ProxiedPlayer)?.server?.info?.name,
							null
					)
					// announceKick(player.name, player.uniqueId, sender, effectiveReason, silent)
				}
			}
			
			4 -> {
				// Ban de 4 horas
				
				val expires = System.currentTimeMillis() + 14400000 // 4 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip

							this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a4 horas
				""".trimIndent().toTextComponent())

				// announceBan(player?.name ?: punishedDisplayName!!, player?.uniqueId ?: punishedUniqueId!!, sender, effectiveReason, silent, true, expires)
			}
			
			5 -> {
				// Ban de 12 horas
				
				val expires = System.currentTimeMillis() + 43200000 // 12 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a12 horas
				""".trimIndent().toTextComponent())


				// announceBan(player?.name ?: punishedDisplayName!!, player?.uniqueId ?: punishedUniqueId!!, sender, effectiveReason, silent, true, expires)
			}
			
			6 -> {
				// Ban de 1 dia
				
				val expires = System.currentTimeMillis() + 86400000 // 24 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a1 dia
				""".trimIndent().toTextComponent())

				// announceBan(player?.name ?: punishedDisplayName!!, player?.uniqueId ?: punishedUniqueId!!, sender, effectiveReason, silent, true, expires)
			}
			
			7 -> {
				// Ban de 3 dias
				val expires = System.currentTimeMillis() + 259200000 // 72 horas
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = true
						this.expiresAt = expires
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = expires
						}
					}
				}
				
				// TODO: Hard coded, remover depois
				player?.disconnect("""
					§cVocê foi temporariamente banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
					§cExpira em: §a3 dias
				""".trimIndent().toTextComponent())

				// announceBan(player?.name ?: punishedDisplayName!!, player?.uniqueId ?: punishedUniqueId!!, sender, effectiveReason, silent, true, expires)
			}
			
			8 -> {
				// Ban permanente
				
				transaction(Databases.databaseNetwork) {
					Ban.new {
						this.player = punishedUniqueId!!
						this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason
						
						this.temporary = false
					}
					
					if (ip != null) {
						IpBan.new {
							this.ip = ip
							this.punishedBy = if (sender is ProxiedPlayer) sender.uniqueId else null
							this.punishedAt = System.currentTimeMillis()
							this.reason = effectiveReason
							this.temporary = true
							this.expiresAt = PunishmentManager.DEFAULT_IPBAN_EXPIRATION
						}
					}
				}
				
				player?.disconnect("""
					§cVocê foi banido!
					§cMotivo:
					
					§a$effectiveReason
					§cPor: $punisherDisplayName
				""".trimIndent().toTextComponent())

				// announceBan(player?.name ?: punishedDisplayName!!, player?.uniqueId ?: punishedUniqueId!!, sender, effectiveReason, silent, false)
			}
		}

		sender.sendMessage("§b${punishedDisplayName}§a foi punido com sucesso, yay!! ^-^".toTextComponent())

		PunishmentManager.sendPunishmentToDiscord(
				silent,
				punishedDisplayName ?: "Nome desconhecido",
				punishedUniqueId,
				"Avisado",
				PunishmentManager.getPunisherName(sender),
				effectiveReason,
				(sender as? ProxiedPlayer)?.server?.info?.name,
				null
		)

		if (!silent) {
			m.proxy.broadcast("§b${(sender as? ProxiedPlayer)?.name ?: "Pantufa"}§a avisou §c$playerName§a por §6\"§e$reason§6\"§a!".toTextComponent())
		}
	}
}