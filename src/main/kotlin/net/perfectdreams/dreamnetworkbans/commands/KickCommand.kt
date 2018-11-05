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

class KickCommand(val m: DreamNetworkBans) : AbstractCommand("kick", permission = "dreamnetworkbans.kick") {
	
	@Subcommand
    fun kick(sender: CommandSender, @InjectArgument(ArgumentType.PLAYER) player: ProxiedPlayer?, @InjectArgument(ArgumentType.ARGUMENT_LIST) reason: String?) {
		if (player == null) {
			return sender.sendMessage("§cEste jogador não pôde ser encontrado!".toTextComponent())
		}
		
		var effectiveReason = reason ?: "Sem motivo definido"
		
		var silent = false
		if (effectiveReason.endsWith("-s")) {
			silent = true
			
			effectiveReason = effectiveReason.substring(0, (effectiveReason.length - "-s".length) - 1)
		}
		
		announceKick(player.name, sender, effectiveReason, silent)
		
		player.disconnect("""
            §cVocê foi expulso do servidor!
            §cMotivo:

            §a$effectiveReason
            §cPor: ${sender.name}
			§7Não se preocupe, você poderá voltar a jogar simplesmente entrando novamente no servidor!
		""".trimIndent().toTextComponent())
		sender.sendMessage("§a${player.name} (${player.uniqueId}) kickado com sucesso pelo motivo \"$effectiveReason\"".toTextComponent())
	}
	
	fun announceKick(playerName: String, author: CommandSender, reason: String, silent: Boolean) {
		if (silent) {
			DreamNetwork.PANTUFA.sendMessage(
					"506859824034611212",
					"**$playerName** foi expulso!\nFazer o que né, não soube ler as regras!\n\n**Expulso pelo:** ${author.name}\n**Motivo:** $reason\n**Servidor:** ${(author as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}"
			)
		} else {
			m.proxy.broadcast("§c§l${author.name}§c expulsou §l$playerName§c pelo motivo \"$reason\" no servidor ${(author as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}".toTextComponent())
			DreamNetwork.PANTUFA.sendMessage(
					"378318041542426634",
					"**$playerName** foi expulso!\nFazer o que né, não soube ler as regras!\n\n**Expulso pelo:** ${author.name}\n**Motivo:** $reason\n**Servidor:** ${(author as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido"}"
			)
		}
	}
}