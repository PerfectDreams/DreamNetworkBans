package net.perfectdreams.dreamnetworkbans

import net.md_5.bungee.api.plugin.Plugin
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamnetworkbans.commands.*
import net.perfectdreams.dreamnetworkbans.listeners.LoginListener
import net.perfectdreams.dreamnetworkbans.tables.Bans
import net.perfectdreams.dreamnetworkbans.tables.Fingerprints
import net.perfectdreams.dreamnetworkbans.tables.IpBans
import net.perfectdreams.dreamnetworkbans.tables.Warns
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class DreamNetworkBans : Plugin() {
	companion object {
		lateinit var INSTANCE: DreamNetworkBans
	}

	var youtuberNames = mutableSetOf<String>()

	override fun onEnable() {
		super.onEnable()
		INSTANCE = this

		registerCommands()

		this.proxy.pluginManager.registerListener(this, LoginListener(this))

		val youtubersFile = File("youtubers.json")
		if (!youtubersFile.exists()) {
			youtubersFile.createNewFile()
			youtubersFile.writeText("[]")
		}

		transaction(Databases.databaseNetwork) {
			SchemaUtils.create(
					Bans
					// IpBans,
					Warns,
					Fingerprints
			)
		}
	}

	fun registerCommands() {
		BanCommand(this).register(this)
		KickCommand(this).register(this)
		UnbanCommand(this).register(this)
		YouTuberAssistCommand(this).register(this)
		WarnCommand(this).register(this)
		CheckBanCommand(this).register(this)
		FingerprintCommand(this).register(this)
	}
}