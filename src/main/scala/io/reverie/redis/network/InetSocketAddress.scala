package io.reverie.redis.network

final case class InetSocketAddress(host: Host, port: Port) {
  def asJava: java.net.InetSocketAddress =
    new java.net.InetSocketAddress(host.value, port.value)
}
