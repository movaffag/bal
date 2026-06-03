package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val name: String,
    val lastFetched: Long = 0L
)

@Entity(
    tableName = "v2ray_nodes",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subscriptionId"])]
)
data class V2RayNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionId: Int,
    val name: String,
    val protocol: String, // VMESS, VLESS, SS, TROJAN, UNKNOWN
    val address: String,
    val port: Int,
    val rawUrl: String,
    val latencyMs: Int = -1, // -1: Unchecked, -2: Timeout, >= 0: Latency in ms
    val lastChecked: Long = 0L
)

@Dao
interface V2RayDao {
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE url = :url LIMIT 1")
    suspend fun getSubscriptionByUrl(url: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity): Long

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: Int)

    @Query("UPDATE subscriptions SET lastFetched = :timestamp WHERE id = :id")
    suspend fun updateLastFetched(id: Int, timestamp: Long)

    @Query("SELECT * FROM v2ray_nodes ORDER BY CASE WHEN latencyMs >= 0 THEN latencyMs ELSE 999999 END ASC, id ASC")
    fun getAllNodesSorted(): Flow<List<V2RayNodeEntity>>

    @Query("SELECT * FROM v2ray_nodes WHERE subscriptionId = :subId ORDER BY CASE WHEN latencyMs >= 0 THEN latencyMs ELSE 999999 END ASC, id ASC")
    fun getNodesBySubscription(subId: Int): Flow<List<V2RayNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<V2RayNodeEntity>)

    @Query("DELETE FROM v2ray_nodes WHERE subscriptionId = :subId")
    suspend fun deleteNodesForSubscription(subId: Int)

    @Query("UPDATE v2ray_nodes SET latencyMs = :latency, lastChecked = :timestamp WHERE id = :id")
    suspend fun updateNodeLatency(id: Int, latency: Int, timestamp: Long)

    @Query("UPDATE v2ray_nodes SET latencyMs = -1")
    suspend fun resetAllLatencies()
}

@Database(entities = [SubscriptionEntity::class, V2RayNodeEntity::class], version = 1, exportSchema = false)
abstract class V2Database : RoomDatabase() {
    abstract fun v2rayDao(): V2RayDao
}
