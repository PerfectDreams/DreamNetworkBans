package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.ArgumentType
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.InjectArgument
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.pojos.Ban
import java.util.*

class BanCommand(val m: DreamNetworkBans) : AbstractCommand("ban", permission = "dreamnetworkbans.ban", aliases = arrayOf("banir")) {

	@Subcommand
	fun root(sender: CommandSender) {
		sender.sendMessage("§cUse /ban jogador motivo".toTextComponent())
	}
	
	@Subcommand
	fun withoutReason(sender: CommandSender, player: String) {
		ban(sender, player, null)
	}
	
    @Subcommand
    fun ban(sender: CommandSender, player: String, @InjectArgument(ArgumentType.ARGUMENT_LIST) reason: String?) {
		var effectiveReason = reason ?: "Sem motivo definido"
		
		var silent = false
		if (effectiveReason.endsWith("-s")) {
			silent = true
			
			effectiveReason = effectiveReason.substring(0, (effectiveReason.length - "-s".length) - 1)
		}
		
		val uuid = m.getOfflineUUID(player).toString()
		val ban = Ban(uuid)
	
		val proxiedPlayer = m.proxy.getPlayer(player)
		
		ban.author = (sender as? ProxiedPlayer)?.uniqueId?.toString() ?: "CONSOLE"
	
		ban.authorName = if (sender is ProxiedPlayer) sender.name else "Servidor"
		ban.reason = effectiveReason
		ban.playerName = player
	
		announceBan(player, sender, effectiveReason, silent)
	
		proxiedPlayer?.disconnect("""
			§cVocê foi banido!
			§cMotivo:
			
			§a$effectiveReason
			§cPor: ${sender.name}
        """.trimIndent().toTextComponent())
	
		sender.sendMessage("§a$player ($uuid) banido com sucesso!".toTextComponent())
		m.bansColl.insertOne(ban)
	}
	
	fun announceBan(playerName: String, author: CommandSender, reason: String, silent: Boolean) {
		if (silent) {
			DreamNetwork.PANTUFA.sendMessage(
					"506859824034611212",
					"**$playerName** foi banido permanentemente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${author.name}\n**Motivo:** $reason\n**Servidor:** ${(author as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}"
			)
		} else {
			m.proxy.broadcast("§c§l${author.name}§c baniu §l$playerName§c pelo motivo \"$reason\" no servidor ${(author as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}".toTextComponent())
			DreamNetwork.PANTUFA.sendMessage(
					"378318041542426634",
					"**$playerName** foi banido permanentemente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${author.name}\n**Motivo:** $reason\n**Servidor:** ${(author as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}"
			)
		}
	}

}