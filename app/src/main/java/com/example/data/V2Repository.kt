package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class V2Repository(private val v2rayDao: V2RayDao) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val subscriptions: Flow<List<SubscriptionEntity>> = v2rayDao.getAllSubscriptions()
    val allNodes: Flow<List<V2RayNodeEntity>> = v2rayDao.getAllNodesSorted()

    /**
     * Fetches a subscription link, parses its nodes, and inserts everything into the database.
     */
    suspend fun fetchAndAddSubscription(url: String, customName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "v2rayNG/1.8.5 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
            }

            val bodyText = response.body?.string() ?: ""
            if (bodyText.trim().isEmpty()) {
                return@withContext Result.failure(Exception("Empty subscription response"))
            }

            // Clean-up name
            val finalName = customName.ifEmpty {
                val originHost = try {
                    java.net.URI(url).host ?: "Subscription"
                } catch (e: Exception) {
                    "Subscription"
                }
                originHost
            }

            // Insert sub entity
            val subEntity = SubscriptionEntity(
                url = url,
                name = finalName,
                lastFetched = System.currentTimeMillis()
            )
            val subId = v2rayDao.insertSubscription(subEntity).toInt()

            // Parse nodes
            val parsedNodes = V2Parser.parseSubscription(subId, bodyText)
            if (parsedNodes.isNotEmpty()) {
                // Delete old nodes first for this subscription
                v2rayDao.deleteNodesForSubscription(subId)
                // Insert new nodes
                v2rayDao.insertNodes(parsedNodes)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No valid proxy nodes found in subscription"))
            }
        } catch (e: Exception) {
            Log.e("V2Repository", "Error fetching subscription", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a subscription and its associated nodes automatically via cascade delete.
     */
    suspend fun deleteSubscription(subId: Int) = withContext(Dispatchers.IO) {
        v2rayDao.deleteSubscription(subId)
    }

    /**
     * Resets latency values for all nodes.
     */
    suspend fun resetLatencies() = withContext(Dispatchers.IO) {
        v2rayDao.resetAllLatencies()
    }

    /**
     * Runs socket latency (direct TCP ping) on all nodes in parallel with bounded concurrency.
     */
    suspend fun pingAllNodes(nodes: List<V2RayNodeEntity>, progressCallback: ((index: Int, total: Int) -> Unit)? = null) = withContext(Dispatchers.IO) {
        if (nodes.isEmpty()) return@withContext

        val semaphore = Semaphore(20) // Allow up to 20 parallel pings
        val total = nodes.size
        var completedCount = 0

        coroutineScope {
            val jobs = nodes.map { node ->
                async {
                    val latency = semaphore.withPermit {
                        pingNodeTcp(node.address, node.port)
                    }
                    v2rayDao.updateNodeLatency(node.id, latency, System.currentTimeMillis())
                    
                    synchronized(this) {
                        completedCount++
                        progressCallback?.invoke(completedCount, total)
                    }
                }
            }
            jobs.awaitAll()
        }
    }

    /**
     * Measures TCP handshake connection speed to the specific host and port.
     */
    private fun pingNodeTcp(address: String, port: Int): Int {
        val startTime = System.currentTimeMillis()
        return try {
            val socket = Socket()
            // We use a 2.5 second timeout to declare a node timed out quickly
            socket.connect(InetSocketAddress(address, port), 2500)
            socket.close()
            val endTime = System.currentTimeMillis()
            (endTime - startTime).toInt()
        } catch (e: Exception) {
            -2 // -2 represents timeout or error
        }
    }
}
