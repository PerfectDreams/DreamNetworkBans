package net.perfectdreams.dreamnetworkbans.commands

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.set
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.commands.ArgumentType
import net.perfectdreams.commands.annotation.InjectArgument
import net.perfectdreams.commands.annotation.Subcommand
import net.perfectdreams.dreamcorebungee.commands.SparklyBungeeCommand
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.DreamUtils
import net.perfectdreams.dreamcorebungee.utils.ParallaxEmbed
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.DreamNetworkBans
import net.perfectdreams.dreamnetworkbans.utils.CustomArgument
import net.perfectdreams.dreamnetworkbans.utils.DateUtils
import net.perfectdreams.dreamnetworkbans.utils.InjectCustomArgument
import java.util.*

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
		
		announceKick(player.name, player.uniqueId, sender, effectiveReason, silent)
		
		player.disconnect("""
            §cVocê foi expulso do servidor!
            §cMotivo:

            §a$effectiveReason
            §cPor: ${sender.name}
			§7Não se preocupe, você poderá voltar a jogar simplesmente entrando novamente no servidor!
		""".trimIndent().toTextComponent())
		sender.sendMessage("§a${player.name} (${player.uniqueId}) kickado com sucesso pelo motivo \"$effectiveReason\"".toTextComponent())
	}
	
	fun announceKick(playerName: String, uuid: UUID, author: CommandSender, reason: String, silent: Boolean) {
		val embed = ParallaxEmbed()

		embed.title = "$playerName | Expulso"
		embed.description = "Fazer o que né, não soube ler as regras! <:sad_cat:419474182758334465>"

		embed.addField("Quem puniu", (author as? ProxiedPlayer)?.name ?: "Pantufa", true)
		embed.addField("Motivo", reason, true)
		embed.addField("Servidor", (author as? ProxiedPlayer)?.server?.info?.name ?: "Desconhecido", true)

		embed.rgb = ParallaxEmbed.ParallaxColor(114, 137, 218)

		embed.footer = ParallaxEmbed.ParallaxEmbedFooter("UUID do usuário: $uuid", null)
		embed.thumbnail = ParallaxEmbed.ParallaxEmbedImage("https://sparklypower.net/api/v1/render/avatar?name=$playerName&scale=16")

		val json = jsonObject(
				"type" to "sendMessage",
				"message" to " ",
				"embed" to DreamUtils.gson.toJsonTree(embed)
		)

		if (silent) {
			json["textChannelId"] = "506859824034611212"

			DreamNetwork.PANTUFA.sendAsync(json)
		} else {
			json["textChannelId"] = "378318041542426634"

			m.proxy.broadcast("§b${(author as? ProxiedPlayer)?.name ?: "Pantufa"}§a expulsou §c$playerName§a por §6\"§e$reason§6\"§a!".toTextComponent())
			DreamNetwork.PANTUFA.sendAsync(json)
		}
	}
}