package com.icestar.utils.crypto

object MD5 {
  def hash(s: String, digit: Int = 32) = {
    val m = java.security.MessageDigest.getInstance("MD5")
    val b = s.getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(digit)
  }
}