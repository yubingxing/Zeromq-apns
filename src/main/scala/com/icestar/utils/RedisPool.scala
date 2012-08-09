package com.icestar.utils
import com.redis.RedisClientPool

/**
 * A simple redis client pool
 * @author IceStar
 */
object RedisPool {
  var clients: RedisClientPool = _
  var host: String = _
  var port: Int = 0

  def init(host: String, port: Int) {
    this.host = host
    this.port = port
    clients = new RedisClientPool(host, port)
  }

  def lp(key: Any, value: Any) = {
    clients.withClient(client => client.lpush(key, value))
  }

  def rp(key: Any, value: Any) = {
    clients.withClient(client => client.rpush(key, value))
  }

  def lpop(key: Any) = {
    clients.withClient(client => client.lpop(key))
  }

  def rpop(key: Any) = {
    clients.withClient(client => client.rpop(key))
  }

  def lrem(key: Any, count: Int, value: Any) = {
    clients.withClient(client => client.lrem(key, count, value))
  }

  def llen(key: Any) = {
    clients.withClient(client => client.llen(key) get)
  }

  def set(key: Any, value: Any) = {
    clients.withClient(client => client.set(key, value))
  }

  def get(key: Any) = {
    clients.withClient(client => client.get(key) get)
  }

  def hexists(key: Any, field: Any) = {
    clients.withClient(client => client.hexists(key, field))
  }

  def hset(key: Any, field: Any, value: Any) = {
    clients.withClient(client => client.hset(key, field, value))
  }

  def hget(key: Any, field: Any) = {
    clients.withClient(client => client.hget(key, field) get)
  }

  def hmset(key: Any, map: Map[String, String]) = {
    clients.withClient(client => client.hmset(key, map))
  }

  def hmget(key: Any, fields: Array[String]) = {
    clients.withClient(client => client.hmget(key, fields) get)
  }

  def hkeys(key: Any) = {
    clients.withClient(client => client.hkeys(key) get)
  }

  def hvals(key: Any) = {
    clients.withClient(client => client.hvals(key) get)
  }

  def hgetall[K, V](key: Any) = {
    clients.withClient(client => client.hgetall(key) get)
  }

  def hdel(key: Any, field: Any, fields: Any*) = {
    clients.withClient(client => client.hdel(key, field, fields))
  }

  def hlen(key: Any) = {
    clients.withClient(client => client.hlen(key) get)
  }

  def flushdb() = {
    clients.withClient(client => client.flushdb)
  }
}