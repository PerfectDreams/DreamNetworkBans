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

class BanCommand(val m: DreamNetworkBans) : AbstractCommand("ban", permission = "dreamnetworkbans.ban", aliases = arrayOf("banir")) {

	@Subcommand
	fun root(sender: CommandSender) {
		sender.sendMessage("§cUse /ban jogador motivo".toTextComponent())
	}
	
	@Subcommand
	fun withoutReason(sender: CommandSender, @InjectArgument(ArgumentType.PLAYER) player: ProxiedPlayer?) {
		ban(sender, player, null)
	}
	
    @Subcommand
    fun ban(sender: CommandSender, @InjectArgument(ArgumentType.PLAYER) player: ProxiedPlayer?, @InjectArgument(ArgumentType.ARGUMENT_LIST) reason: String?) {
		if (player == null) {
			return sender.sendMessage("§cEste jogador não pôde ser encontrado!".toTextComponent())
		}
	
		val effectiveReason = reason ?: "Sem motivo definido"
	
		val ban = Ban(player.uniqueId.toString())
	
		ban.author = if (sender is ProxiedPlayer) {
			sender.uniqueId.toString()
		} else {
			"CONSOLE"
		}
	
		ban.authorName = if (sender is ProxiedPlayer) sender.name else "Servidor"
	
		ban.ip = player.address.hostString
		ban.reason = effectiveReason
	
		ban.playerName = player.name
	
		ban.isIpBan = false
	
		m.bansColl.insertOne(ban)
	
		m.proxy.broadcast("§c§l${sender.name}§c baniu §l${player.name}§c pelo motivo \"$effectiveReason\" no servidor ${player.server.info.name}".toTextComponent())
		DreamNetwork.PANTUFA.sendMessage(
				"378318041542426634",
				"**${player.name}** foi banido permanentemente!\nFazer o que né, não soube ler as regras!\n\n**Banido pelo:** ${sender.name}\n**Motivo:** $effectiveReason\n**Servidor:** ${player.server.info.name}"
		)
		
		player.disconnect("""
			§cVocê foi banido!
			§cMotivo:
			
			§a$effectiveReason
			§cPor: ${sender.name}
        """.trimIndent().toTextComponent())
		sender.sendMessage("§a${player.name} (${player.uniqueId}) banido com sucesso pelo motivo \"$effectiveReason\"".toTextComponent())
	}

}