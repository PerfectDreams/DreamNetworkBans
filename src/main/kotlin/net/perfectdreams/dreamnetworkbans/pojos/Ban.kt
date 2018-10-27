package net.perfectdreams.dreamnetworkbans.pojos

import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonProperty

class Ban @BsonCreator constructor(@BsonProperty("_id") _id: String) {

    @BsonProperty("_id")
    val uuid = _id
	var playerName = ""

    val timestamp = System.currentTimeMillis()

    var author = "" // UUID do usuário que baniu
	var authorName = "" // Nickname do usuário que baniu
	
    var reason = ""

    var ip = ""
	var isIpBan = false

}