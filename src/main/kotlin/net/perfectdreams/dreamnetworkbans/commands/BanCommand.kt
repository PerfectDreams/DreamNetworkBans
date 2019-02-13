package net.perfectdreams.dreamnetworkbans.commands

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
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import net.perfectdreams.dreamnetworkbans.utils.DateUtils
import net.perfectdreams.dreamnetworkbans.utils.convertToEpochMillisRelativeToNow
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BanCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("ban", "banir"), permission = "dreamnetworkbans.ban") {

	@Subcommand
	fun root(sender: CommandSender) {
		sender.sendMessage("§cUse /ban jogador motivo".toTextComponent())
	}
	
	@Subcommand
	fun withoutReason(sender: CommandSender, player: String) {
		ban(sender, player, null)
	}

	@Subcommand
	fun ban(sender: CommandSender, playerName: String, @InjectArgument(ArgumentType.ALL_ARGUMENTS) reason: String?) {
		var punishedUniqueId: UUID? = null
		var punishedDisplayName: String? = null

		// O nosso querido "player name" pode ser várias coisas...
		// Talvez seja um player online!
		val player = m.proxy.getPlayer(playerName)
		if (player != null) {
			punishedDisplayName = player.displayName
			punishedUniqueId = player.uniqueId
		} else {
			// Talvez seja o UUID de um player online!
			punishedUniqueId = try {
				UUID.fromString(playerName)
			} catch (e: IllegalArgumentException) {
				null
			}
			if (punishedUniqueId != null) {
				val playerByUuid = m.proxy.getPlayer(punishedUniqueId)
				if (playerByUuid != null) {
					punishedUniqueId = playerByUuid.uniqueId
					punishedDisplayName = playerByUuid.name
				} else {
					// ...tá, mas talvez seja o UUID de um player offline!
					// Caso o UUID seja != null, quer dizer que ele É UM UUID VÁLIDO!!
					punishedDisplayName = punishedUniqueId.toString()
				}
			} else {
				// Se não, vamos processar como se fosse um player mesmo
				punishedDisplayName = playerName
				punishedUniqueId = PunishmentManager.getUniqueId(playerName)
			}
		}

		// ...blah
		if (punishedUniqueId == null && punishedDisplayName == null) {
			sender.sendMessage("§cEu sei que você tá correndo para banir aquele mlk meliante... mas eu não conheço ninguém chamado §b$playerName§c... respira um pouco... fica calmo e VEJA O NOME NOVAMENTE!".toTextComponent())
			return
		}

		var effectiveReason = reason ?: "Sem motivo definido"
		
		var silent = false
		if (effectiveReason.contains("-s")) {
			silent = true
			
			effectiveReason = effectiveReason.replace("-s", "")
		}

		var ipBan = false
		if (effectiveReason.contains("-i")) {
			ipBan = true

			effectiveReason = effectiveReason.replace("-i", "")
		}

		var temporary = false
		var time = 0.toLong()
		if (effectiveReason.contains("-t")) {
			temporary = true

			val splitted = effectiveReason.split("-t")
			val timeSpec = splitted[1]

			val timeMillis = timeSpec.convertToEpochMillisRelativeToNow()
			if (timeMillis < System.currentTimeMillis()) { // :rolling_eyes:
				return
			}

			time = timeMillis
		}

		val punisherDisplayName = if (sender is ProxiedPlayer) {
			sender.name
		} else { "Pantufa" }

		val geoLocalization = transaction(Databases.databaseNetwork) {
			GeoLocalization.find { GeoLocalizations.player eq punishedUniqueId!! }.firstOrNull()
		}

		val ip = if (player != null)
			player.address.hostString
		else
			geoLocalization?.ip

		transaction(Databases.databaseNetwork) {
			if (ipBan) {
				if (ip == null) {
					sender.sendMessage("§cInfelizmente não há nenhum registro de IP do player §e$punishedDisplayName§c!".toTextComponent())
					return@transaction
				}

				transaction(Databases.databaseNetwork) {
					IpBan.new {
						this.ip = ip
						this.player = punishedUniqueId!!

						this.punisherName = punisherDisplayName
						this.punishedBy = (sender as? ProxiedPlayer)?.uniqueId
						this.punishedAt = System.currentTimeMillis()
						this.reason = effectiveReason

						this.temporary = temporary
						if (temporary) {
							this.expiresAt = time
						}
					}
				}
			}

			Ban.new {
				this.player = punishedUniqueId!!
				this.punisherName = punisherDisplayName
				this.punishedBy = (sender as? ProxiedPlayer)?.uniqueId
				this.punishedAt = System.currentTimeMillis()
				this.reason = effectiveReason

				this.temporary = temporary
				if (temporary) {
					this.expiresAt = time
				}
			}
		}
		
		if (ip != null && !ipBan && !temporary) {
			transaction(Databases.databaseNetwork) {
				IpBan.new {
					this.ip = ip
					this.player = punishedUniqueId!!

					this.punisherName = punisherDisplayName
					this.punishedBy = (sender as? ProxiedPlayer)?.uniqueId
					this.punishedAt = System.currentTimeMillis()
					this.reason = effectiveReason
					this.temporary = true
					this.expiresAt = System.currentTimeMillis() + PunishmentManager.DEFAULT_IPBAN_EXPIRATION
				}
			}
		}

		// Vamos expulsar o player ao ser banido
		player?.disconnect("""
			§cVocê foi banido!
			§cMotivo:

			§a$effectiveReason
			§cPor: $punisherDisplayName
        """.trimIndent().toTextComponent())

		sender.sendMessage("§b${punishedDisplayName}§a foi punido com sucesso, yay!! ^-^".toTextComponent())

		val embed = ParallaxEmbed()

		embed.title = "$punishedDisplayName | Banido ${if (temporary) "Temporariamente" else "Permanentemente"}"
		embed.description = "Fazer o que né, não soube ler as regras! <:sad_cat:419474182758334465>"

		embed.addField("Quem puniu", sender.name, true)
		embed.addField("Motivo", effectiveReason, true)
		embed.addField("Servidor", player?.server?.info?.name ?: "Desconhecido", true)

		if (temporary) {
			embed.addField("Duração", DateUtils.formatDateDiff(time), true)
		}

		embed.rgb = ParallaxEmbed.ParallaxColor(114, 137, 218)

		embed.footer = ParallaxEmbed.ParallaxEmbedFooter("UUID do usuário: $punishedUniqueId", null)
		embed.thumbnail = ParallaxEmbed.ParallaxEmbedImage("https://sparklypower.net/api/v1/render/avatar?name=$punishedDisplayName&scale=16")

		if (!silent) {
			m.proxy.broadcast("§b${punisherDisplayName}§a baniu §c${punishedDisplayName}§a por §6\"§e${effectiveReason}§6\"§a!".toTextComponent())

			DreamNetwork.PANTUFA.sendMessage("378318041542426634", DreamUtils.gson.toJson(embed))
		} else {
			DreamNetwork.PANTUFA.sendMessage("506859824034611212", DreamUtils.gson.toJson(embed))
		}
	}
}