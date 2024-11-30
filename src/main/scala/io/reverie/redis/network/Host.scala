package io.reverie.redis.network

opaque type Host = String
object Host {
  def apply(value: String): Host = value

  extension (host: Host) {
    def value: String = host
  }
}
