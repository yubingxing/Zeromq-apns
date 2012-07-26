package com.icestar
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

  def llen(key: Any) = {
    clients.withClient(client => client.llen(key) get)
  }

  def set(key: Any, value: Any) = {
    clients.withClient(client => client.set(key, value))
  }

  def get(key: Any) = {
    clients.withClient(client => client.get(key) get)
  }

  def hset(key: Any, field: Any, value: Any) = {
    clients.withClient(client => client.hset(key, field, value))
  }

  def hget(key: Any, field: Any) = {
    clients.withClient(client => client.hget(key, field) get)
  }

  def hlen(key: Any) = {
    clients.withClient(client => client.hlen(key) get)
  }

  def flushdb() = {
    clients.withClient(client => client.flushdb)
  }
}