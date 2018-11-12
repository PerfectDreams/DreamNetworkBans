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

class DreamNetworkBans : Plugin() {
	companion object {
		lateinit var INSTANCE: DreamNetworkBans
	}

	override fun onEnable() {
		super.onEnable()
		INSTANCE = this

		registerCommands()
		
		this.proxy.pluginManager.registerListener(this, LoginListener(this))

		transaction(Databases.databaseNetwork) {
			SchemaUtils.createMissingTablesAndColumns(
					Bans,
					IpBans,
					Warns,
					Fingerprints
			)
		}
	}
	
	fun registerCommands() {
		BanCommand(this).register(this)
		KickCommand().register(this)
		UnbanCommand(this).register(this)
		WarnCommand(this).register(this)
		CheckBanCommand(this).register(this)
		FingerprintCommand(this).register(this)
	}
}