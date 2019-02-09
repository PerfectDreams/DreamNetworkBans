package net.perfectdreams.dreamnetworkbans.dao

import net.perfectdreams.dreamnetworkbans.tables.DiscordAccounts
import org.jetbrains.exposed.dao.*

class DiscordAccount(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DiscordAccount>(DiscordAccounts)

    var minecraftId by DiscordAccounts.minecraftId
    var discordId by DiscordAccounts.discordId
    var isConnected by DiscordAccounts.isConnected
}