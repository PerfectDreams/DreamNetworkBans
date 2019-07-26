package net.perfectdreams.dreamnetworkbans

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.set
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.perfectdreams.dreamcorebungee.network.DreamNetwork
import net.perfectdreams.dreamcorebungee.utils.DreamUtils
import net.perfectdreams.dreamcorebungee.utils.ParallaxEmbed
import net.perfectdreams.dreamcorebungee.utils.extensions.toTextComponent
import net.perfectdreams.dreamnetworkbans.utils.DateUtils
import java.util.*

object PunishmentManager {
	// 14 dias
	const val WARN_EXPIRATION = 1_209_600_000
	
	// 90 dias
	const val DEFAULT_IPBAN_EXPIRATION = 466_560_000_000
	
	fun getUniqueId(playerName: String): UUID {
		// UUIDs podem ser diferentes... mas já que a gente é um offline mode boi, complicar pra quê?
		return UUID.nameUUIDFromBytes("OfflinePlayer:$playerName".toByteArray(Charsets.UTF_8))
	}

	/**
	 * Sends the punishment to Discord
	 */
	fun sendPunishmentToDiscord(silent: Boolean, punishedDisplayName: String, punishedUniqueId: UUID, title: String, punisherDisplayName: String, effectiveReason: String? = null, server: String?, time: Long? = null) {
		val embed = ParallaxEmbed()

		embed.title = "$punishedDisplayName | $title"
		embed.description = "Fazer o que né, não soube ler as regras! <:sad_cat:419474182758334465>"

		embed.addField("Quem puniu", punisherDisplayName, true)
		embed.addField("Motivo", effectiveReason ?: "Não sei, não quero saber, e tenho raiva de quem sabe (Sem motivo definido)", true)
		embed.addField("Servidor", server ?: "Desconhecido", true)

		if (time != null) {
			embed.addField("Duração", DateUtils.formatDateDiff(time), true)
		}

		embed.rgb = ParallaxEmbed.ParallaxColor(114, 137, 218)

		embed.footer = ParallaxEmbed.ParallaxEmbedFooter("UUID do usuário: $punishedUniqueId", null)
		embed.thumbnail = ParallaxEmbed.ParallaxEmbedImage("https://sparklypower.net/api/v1/render/avatar?name=$punishedDisplayName&scale=16")

		val json = jsonObject(
				"type" to "sendMessage",
				"message" to " ",
				"embed" to DreamUtils.gson.toJsonTree(embed)
		)

		json["textChannelId"] = if (silent) "506859824034611212" else "378318041542426634"
		DreamNetwork.PANTUFA.sendAsync(json)
	}

	fun getPunisherName(sender: CommandSender): String {
		return sender.name.let {
			if (it == "Console")
				"Pantufa"
			else it
		}
	}

	fun getPunishedInfoByString(playerName: String): Punished? {
		var punishedUniqueId: UUID? = null
		var punishedDisplayName: String? = null

		// O nosso querido "player name" pode ser várias coisas...
		// Talvez seja um player online!
		var player = DreamNetworkBans.INSTANCE.proxy.getPlayer(playerName)

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
				val playerByUuid = DreamNetworkBans.INSTANCE.proxy.getPlayer(punishedUniqueId)
				if (playerByUuid != null) {
					player = playerByUuid
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
				punishedUniqueId = getUniqueId(playerName)
			}
		}

		if (punishedDisplayName == null && punishedUniqueId == null)
			return null

		return Punished(punishedDisplayName, punishedUniqueId, player)
	}

	data class Punished(
			val displayName: String?,
			val uniqueId: UUID?,
			val player: ProxiedPlayer?
	)
}