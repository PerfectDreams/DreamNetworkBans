package net.perfectdreams.dreamnetworkbans

import net.perfectdreams.dreamcorebungee.KotlinPlugin
import net.perfectdreams.dreamcorebungee.utils.Databases
import net.perfectdreams.dreamnetworkbans.commands.*
import net.perfectdreams.dreamnetworkbans.listeners.LoginListener
import net.perfectdreams.dreamnetworkbans.tables.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class DreamNetworkBans : KotlinPlugin() {

	val youtubersFile by lazy { File(this.dataFolder, "youtubers.json") }
	var youtuberNames = mutableSetOf<String>()

 	val staffIps by lazy { File(this.dataFolder, "staffips.json") }
	
	override fun onEnable() {
		super.onEnable()

		this.dataFolder.mkdirs()
		registerCommand(BanCommand(this))
		registerCommand(CheckBanCommand(this))
		registerCommand(FingerprintCommand(this))
		registerCommand(IPReportCommand(this))
		registerCommand(KickCommand(this))
		registerCommand(UnbanCommand(this))
		registerCommand(UnwarnCommand())
		registerCommand(WarnCommand(this))
		registerCommand(YouTuberAssistCommand(this))

		this.proxy.pluginManager.registerListener(this, LoginListener(this))

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
