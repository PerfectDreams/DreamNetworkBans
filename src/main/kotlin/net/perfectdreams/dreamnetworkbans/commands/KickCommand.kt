package net.perfectdreams.dreamnetworkbans.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.dreamcorebungee.utils.commands.AbstractCommand
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.ArgumentType
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.InjectArgument
import net.perfectdreams.dreamcorebungee.utils.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent

class KickCommand : AbstractCommand("kick", permission = "dreamnetworkbans.kick") {
	
	@Subcommand
    fun kick(sender: CommandSender, @InjectArgument(ArgumentType.PLAYER) player: ProxiedPlayer?, @InjectArgument(ArgumentType.ARGUMENT_LIST) reason: String?) {
		if (player == null) {
			return sender.sendMessage("§cEste jogador não pôde ser encontrado!".toTextComponent())
		}
		
		val effectiveReason = reason ?: "Sem motivo definido"
		
		player.disconnect("""
            §cVocê foi kickado!
            §cMotivo:

            §a$effectiveReason
            §cPor: ${sender.name}
			§7Não se preocupe, você poderá voltar a jogar simplesmente entrando novamente no servidor!
		""".trimIndent().toTextComponent())
		sender.sendMessage("§a${player.name} (${player.uniqueId}) kickado com sucesso pelo motivo \"$effectiveReason\"".toTextComponent())
	}
}