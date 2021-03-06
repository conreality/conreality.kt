/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk

import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import com.zaxxer.hikari.HikariDataSource

private val ID = AtomicLong()

/**
 * An authenticated session with a Conreality master.
 *
 * @constructor
 * @property client
 * @property agentUUID
 * @property password
 */
class Session(val client: Client, val agentUUID: UUID, password: String = "") : AutoCloseable {
  private val connectionPool = HikariDataSource()

  val id = ID.incrementAndGet()

  val agent get() = Object(this, agentUUID)

  val isClosed get() = connectionPool.isClosed

  /**
   * The game this session belongs to.
   */
  val game = Game(this)

  init {
    connectionPool.setPoolName("org.conreality.sdk.Session:" + id)
    connectionPool.setJdbcUrl(client.connectionURL)
    connectionPool.setUsername(agentUUID.toString())
    connectionPool.setPassword(password)
    connectionPool.setAutoCommit(false)
  }

  /**
   * This method is idempotent.
   */
  @Throws(Exception::class)
  override fun close() {
    if (!isClosed) {
      connectionPool.close()
    }
  }

  /**
   * @suppress
   */
  @Throws(ConnectionException::class)
  fun getConnection(): Connection {
    // TODO: throw logic exception if isClosed == true ?
    try {
      return connectionPool.getConnection()
    }
    catch (error: SQLException) {
      throw ConnectionException(error)
    }
  }

  /**
   * Executes an action.
   */
  fun <T> execute(body: (Action) -> T): T {
    return body(Action(this))
  }

  /**
   * @suppress
   */
  fun executeSQL(sqlCommand: String) {
    val connection = connectionPool.getConnection()
    connection.use {
      val statement = connection.createStatement()
      statement.use {
        statement.execute(sqlCommand)
      }
    }
  }
}
