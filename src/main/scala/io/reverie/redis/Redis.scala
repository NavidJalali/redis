package io.reverie.redis

import io.reverie.redis.commands.RedisRequest
import io.reverie.redis.commands.RedisResponse
import io.reverie.redis.commands.RedisRequest.Ping

final class Redis { self =>
  def request(in: RedisRequest): RedisResponse = {
    in match {
      case in: Ping.type => self.ping(in)
    }
  }

  def ping(in: Ping.type): RedisResponse.Pong.type = RedisResponse.Pong
}
