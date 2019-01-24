package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.commands.ArgumentType
import net.perfectdreams.commands.annotation.InjectArgument
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.GeoLocalization
import net.perfectdreams.dreamnetworkbans.dao.IpBan
import net.perfectdreams.dreamnetworkbans.tables.GeoLocalizations
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BanCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("ban", "banir"), permission = "dreamnetworkbans.ban") {

	@Subcommand
	suspend fun root(sender: CommandSender) {
		sender.sendMessage("§cUse /ban jogador motivo".toTextComponent())
	}
	
	@Subcommand
	suspend fun withoutReason(sender: CommandSender, player: String) {
		ban(sender, player, null)
	}

	@Subcommand
	suspend fun ban(sender: CommandSender, playerName: String, @InjectArgument(ArgumentType.ALL_ARGUMENTS) reason: String?) {
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
		if (effectiveReason.endsWith("-s")) {
			silent = true
			
			effectiveReason = effectiveReason.substring(0, (effectiveReason.length - "-s".length) - 1)
		}

		val punisherDisplayName = if (sender is ProxiedPlayer) {
			sender.name
		} else { "Pantufa" }

		transaction(Databases.databaseNetwork) {
			Ban.new {
				this.player = punishedUniqueId!!
				this.punisherName = punisherDisplayName
				this.punishedBy = punishedUniqueId
				this.punishedAt = System.currentTimeMillis()
				this.reason = effectiveReason
				this.temporary = false
			}
		}
		
		val geoLocalization = transaction(Databases.databaseNetwork) {
			GeoLocalization.find { GeoLocalizations.player eq punishedUniqueId!! }.firstOrNull()
		}
		
		val ip = if (player != null)
			player.address.hostString
		else
			geoLocalization?.ip
		
		if (ip != null) {
			transaction(Databases.databaseNetwork) {
				IpBan.new {
					this.ip = ip
					this.punisherName = punisherDisplayName
					this.punishedBy = punishedUniqueId
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
		m.proxy.broadcast("§b${punisherDisplayName}§a baniu §c${punishedDisplayName}§a por §6\"§e${effectiveReason}§6\"§a!".toTextComponent())

		if (silent) {
			DreamNetwork.PANTUFA.sendMessage(
					"506859824034611212",
					"**$playerName** foi banido permanentemente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${punisherDisplayName}\n**Motivo:** $reason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}"
			)
		} else {
			m.proxy.broadcast("§c§l${punisherDisplayName}§c baniu §l$playerName§c por \"$reason\" no servidor ${player?.server?.info?.name ?: "Desconhecido"}".toTextComponent())
			DreamNetwork.PANTUFA.sendMessage(
					"378318041542426634",
					"**$playerName** foi banido permanentemente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${punisherDisplayName}\n**Motivo:** $reason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}"
			)
		}
	}
}