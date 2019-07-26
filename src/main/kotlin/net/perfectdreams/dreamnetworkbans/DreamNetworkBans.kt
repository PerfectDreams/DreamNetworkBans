package net.perfectdreams.dreamnetworkbans

import net.perfectdreams.dreamcorebungee.KotlinPlugin
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamnetworkbans.commands.*
import net.perfectdreams.dreamnetworkbans.listeners.LoginListener
import net.perfectdreams.dreamnetworkbans.tables.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import net.md_5.bungee.config.YamlConfiguration
import net.md_5.bungee.config.ConfigurationProvider
import net.perfectdreams.dreamcorebungee.utils.discord.DiscordWebhook
import net.perfectdreams.dreamnetworkbans.listeners.SocketListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class DreamNetworkBans : KotlinPlugin() {
	companion object {
		lateinit var INSTANCE: DreamNetworkBans
	}

	val youtubersFile by lazy { File(this.dataFolder, "youtubers.json") }
	var youtuberNames = mutableSetOf<String>()

	val staffIps by lazy { File(this.dataFolder, "staff_ips.json") }

	val config by lazy {
		ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(File(dataFolder, "config.yml"))
	}

	val adminChatWebhook by lazy {
		DiscordWebhook(config.getString("adminchat-webhook"))
	}

	val loggedInPlayers = Collections.newSetFromMap(ConcurrentHashMap<UUID, Boolean>())

	override fun onEnable() {
		super.onEnable()
		INSTANCE = this

		// Caso seja reload
		loggedInPlayers.addAll(this.proxy.players.map { it.uniqueId })

		this.dataFolder.mkdirs()
		registerCommand(BanCommand(this))
		registerCommand(CheckBanCommand(this))
		registerCommand(FingerprintCommand(this))
		registerCommand(DupeIpCommand(this))
		registerCommand(IPReportCommand(this))
		registerCommand(KickCommand(this))
		registerCommand(UnbanCommand(this))
		registerCommand(UnwarnCommand())
		registerCommand(WarnCommand(this))
		registerCommand(YouTuberAssistCommand(this))
		registerCommand(AdminChatCommand(this))
		registerCommand(DiscordCommand(this))
		registerCommand(GeoIpCommand(this))
		registerCommand(IpBanCommand(this))
		registerCommand(IpUnbanCommand(this))

		this.proxy.pluginManager.registerListener(this, LoginListener(this))
		this.proxy.pluginManager.registerListener(this, SocketListener(this))

		if (!youtubersFile.exists()) {
			youtubersFile.createNewFile()
			youtubersFile.writeText("[]")
		}

		if (!staffIps.exists()) {
			staffIps.createNewFile()
			staffIps.writeText("{}")
		}

		transaction(Databases.databaseNetwork) {
			SchemaUtils.createMissingTablesAndColumns(
					Bans,
					IpBans,
					Warns,
					Fingerprints,
					GeoLocalizations
			)
		}
	}
}
