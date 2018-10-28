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
		
		val effectiveReason = reason ?: "Sem motivo definido"
		
		m.proxy.broadcast("§c§l${sender.name}§c kickou §l${player.name}§c pelo motivo \"$effectiveReason\" no servidor ${player.server.info.name}".toTextComponent())
		DreamNetwork.PANTUFA.sendMessage(
				"378318041542426634",
				"**${player.name}** foi expulso!\nFazer o que né, não soube ler as regras!\n\n**Expulso pelo:** ${sender.name}\n**Motivo:** $effectiveReason\n**Servidor:** ${player.server.info.name}"
		)
		
		player.disconnect("""
            §cVocê foi expulso do servidor!
            §cMotivo:

            §a$effectiveReason
            §cPor: ${sender.name}
			§7Não se preocupe, você poderá voltar a jogar simplesmente entrando novamente no servidor!
		""".trimIndent().toTextComponent())
		sender.sendMessage("§a${player.name} (${player.uniqueId}) kickado com sucesso pelo motivo \"$effectiveReason\"".toTextComponent())
	}
}