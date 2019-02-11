package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.commands.ArgumentType
import net.perfectdreams.commands.annotation.InjectArgument
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.utils.CustomArgument
import net.perfectdreams.dreamnetworkbans.utils.InjectCustomArgument

class KickCommand(val m: DreamNetworkBans) : SparklyBungeeCommand(arrayOf("kick"), permission = "dreamnetworkbans.kick") {
	
	@Subcommand
    fun kick(sender: CommandSender, playerName: String, @InjectArgument(ArgumentType.ALL_ARGUMENTS) reason: String?) {
		val player = m.proxy.getPlayer(playerName) ?: return sender.sendMessage("§cEste jogador não pôde ser encontrado!".toTextComponent())

		var effectiveReason = reason ?: "Sem motivo definido"
		
		var silent = false
		if (effectiveReason.endsWith("-f")) {
			player.disconnect("Internal Exception: java.io.IOException: An existing connection was forcibly closed by the remote host".trimIndent().toTextComponent())

			sender.sendMessage("§a${player.name} (${player.uniqueId}) kickado com sucesso pelo motivo \"$effectiveReason\"".toTextComponent())
			return
		}
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