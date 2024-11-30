package io.reverie.redis.network

opaque type Port = Int
object Port {
  def apply(value: Int): Port = value

  extension (port: Port) {
    def value: Int = port
  }
}
