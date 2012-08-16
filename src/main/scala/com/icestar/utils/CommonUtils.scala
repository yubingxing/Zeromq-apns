package com.icestar.utils

/**
 * Some common utils
 * @author IceStar
 *
 */
object CommonUtils {
  def getOrElse(data: Any, el: Any): Any = {
    if (data == null) el else data
  }
  
  def getOrElse(str: String, el: String = ""): String = {
    if (str == null) el else str
  }
}