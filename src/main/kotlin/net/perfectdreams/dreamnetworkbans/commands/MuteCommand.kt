package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.ArgumentType
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.InjectArgument
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.PunishmentManager
import net.perfectdreams.dreamnetworkbans.dao.Mute
import net.perfectdreams.dreamnetworkbans.utils.fancyName
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class MuteCommand(val m: DreamNetworkBans) : AbstractCommand("mute", permission = "dreamnetworkbans.mute") {
	
	@Subcommand
	fun mute(sender: CommandSender, playerName: String, @InjectArgument(ArgumentType.ARGUMENT_LIST) reason: String?) {
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
		
		var effectiveReason = reason ?: "Sem motivo definido"
		
		var silent = false
		if (effectiveReason.endsWith("-s")) {
			silent = true
			
			effectiveReason = effectiveReason.substring(0, effectiveReason.length - "-s".length)
		}
		
		transaction(Databases.databaseNetwork) {
			Mute.new {
				this.player = punishedUniqueId!!
				this.punisher = (sender as? ProxiedPlayer)?.uniqueId
				this.punisherName = sender.fancyName
				this.reason = effectiveReason
				this.punishedAt = System.currentTimeMillis()
				this.temporary = false
			}
		}
		
		sender.sendMessage("§e${punishedDisplayName}§a foi silenciado com sucesso!".toTextComponent())
		if (silent) {
			DreamNetwork.PANTUFA.sendMessage(
					"506859824034611212",
					"**$playerName** foi silenciado!\nFazer o que né, não soube ler as regras!\n\n**Silenciado pelo:** ${sender.fancyName}\n**Motivo:** $effectiveReason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}"
			)
		} else {
			m.proxy.broadcast("§b${sender.fancyName}§a deu um aviso em §c${punishedDisplayName}§a por §6\"§e${effectiveReason}§6\"§a!".toTextComponent())
			DreamNetwork.PANTUFA.sendMessage(
					"378318041542426634",
					"**$playerName** foi silenciado!\nFazer o que né, não soube ler as regras!\n\n**Avisado pelo:** ${sender.fancyName}\n**Motivo:** $effectiveReason\n**Servidor:** ${player?.server?.info?.name ?: "Desconhecido"}"
			)
		}
	}
}