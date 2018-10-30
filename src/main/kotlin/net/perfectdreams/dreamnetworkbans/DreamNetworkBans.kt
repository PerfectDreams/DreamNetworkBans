package net.perfectdreams.dreamnetworkbans

import net.md_5.bungee.api.plugin.Plugin
import net.perfectdreams.dreamcorebungee.DreamCoreBungee
import net.perfectdreams.dreamnetworkbans.commands.BanCommand
import net.perfectdreams.dreamnetworkbans.commands.KickCommand
import net.perfectdreams.dreamnetworkbans.commands.UnbanCommand
import net.perfectdreams.dreamnetworkbans.commands.YouTuberAssistCommand
import net.perfectdreams.dreamnetworkbans.listeners.LoginListener
import net.perfectdreams.dreamnetworkbans.pojos.Ban
import net.perfectdreams.libs.com.mongodb.MongoClient
import net.perfectdreams.libs.com.mongodb.MongoClientOptions
import net.perfectdreams.libs.com.mongodb.client.MongoCollection
import net.perfectdreams.libs.com.mongodb.client.MongoDatabase
import net.perfectdreams.libs.org.bson.codecs.configuration.CodecRegistries
import net.perfectdreams.libs.org.bson.codecs.pojo.PojoCodecProvider
import java.io.File
import java.util.*

class DreamNetworkBans : Plugin() {
	
	lateinit var mongo: MongoClient
	lateinit var mongoDatabase: MongoDatabase
	lateinit var bansColl: MongoCollection<Ban>
	
	var youtuberNames = mutableSetOf<String>()
	
	override fun onEnable() {
		super.onEnable()
		registerCommands()
		
		this.proxy.pluginManager.registerListener(this, LoginListener(this))
		
		val pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()))
		
		val options = MongoClientOptions.builder()
				.codecRegistry(pojoCodecRegistry)
				.connectionsPerHost(750)
				.build()
		
		mongo = MongoClient(DreamCoreBungee.dreamConfig.mongoDbIp, options)
		mongoDatabase = mongo.getDatabase(DreamCoreBungee.dreamConfig.serverDatabaseName)
		bansColl = mongoDatabase.getCollection("bans", Ban::class.java)
		
		val youtubersFile = File("youtubers.json")
		if (!youtubersFile.exists()) {
			youtubersFile.createNewFile()
			youtubersFile.writeText("[]")
		}
	}
	
	fun registerCommands() {
		BanCommand(this).register(this)
		KickCommand(this).register(this)
		UnbanCommand(this).register(this)
		YouTuberAssistCommand(this).register(this)
	}
	
	fun getOfflineUUID(name: String) = UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray())
	
}