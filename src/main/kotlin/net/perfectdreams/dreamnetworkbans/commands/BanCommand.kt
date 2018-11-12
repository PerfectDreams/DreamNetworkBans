package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.ArgumentType
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.InjectArgument
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.dao.Ban
import net.perfectdreams.dreamnetworkbans.dao.IpBan
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BanCommand(val m: DreamNetworkBans) : AbstractCommand("ban", permission = "dreamnetworkbans.ban", aliases = arrayOf("banir")) {
	@Subcommand
	fun ban(sender: CommandSender, playerName: String, @InjectArgument(ArgumentType.ARGUMENT_LIST) reason: String?) {
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
			}
		}

		// ...blah
		if (punishedUniqueId == null && punishedDisplayName == null) {
			sender.sendMessage("§cEu sei que você tá correndo para banir aquele mlk meliante... mas eu não conheço ninguém chamado §b$playerName§c... respira um pouco... fica calmo e VEJA O NOME NOVAMENTE!".toTextComponent())
			return
		}

		val effectiveReason = reason ?: "Sem motivo definido"

		val punisherDisplayName = if (sender is ProxiedPlayer) {
			sender.name
		} else { "Pantufa" }

		transaction(Databases.databaseNetwork) {
			Ban.new {
				this.player = player.uniqueId
				this.punishedBy = punishedUniqueId
				this.punishedAt = System.currentTimeMillis()
				this.reason = effectiveReason
				this.temporary = false
			}
		}

		// Vamos expulsar o player ao ser banido
		player.disconnect("""
			§cVocê foi banido!
			§cMotivo:
			
			§a$effectiveReason
			§cPor: ${sender.name}
        """.trimIndent().toTextComponent())

		sender.sendMessage("§b${punishedDisplayName}§a foi punido com sucesso, yay!! ^-^".toTextComponent())
		m.proxy.broadcast("§b${punisherDisplayName}§a baniu §c${punishedDisplayName}§a por §6\"§e${effectiveReason}§6\"§a!".toTextComponent())
	}
}