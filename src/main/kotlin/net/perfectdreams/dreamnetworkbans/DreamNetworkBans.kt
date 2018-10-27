package net.perfectdreams.dreamnetworkbans

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import net.md_5.bungee.api.plugin.Plugin
import net.perfectdreams.dreamcorebungee.DreamCoreBungee
import net.perfectdreams.dreamnetworkbans.commands.BanCommand
import net.perfectdreams.dreamnetworkbans.commands.KickCommand
import net.perfectdreams.dreamnetworkbans.commands.UnbanCommand
import net.perfectdreams.dreamnetworkbans.listeners.LoginListener
import net.perfectdreams.dreamnetworkbans.pojos.Ban
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider

class DreamNetworkBans : Plugin() {
	
	lateinit var mongo: MongoClient
	lateinit var mongoDatabase: MongoDatabase
	lateinit var bansColl: MongoCollection<Ban>
	
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
	}
	
	fun registerCommands() {
		BanCommand(this).register(this)
		KickCommand().register(this)
		UnbanCommand(this).register(this)
	}
}