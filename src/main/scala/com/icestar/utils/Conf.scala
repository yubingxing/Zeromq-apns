package com.icestar.utils
import scala.io.Source

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject

/**
 * Reading json conf util
 * @author IceStar
 */
object Conf {
  var data: JSONObject = _

  def read(confPath: String) = {
    data = JSON.parseObject(Source.fromFile(confPath, "utf-8").getLines mkString)
  }

  def get(key: String) = {
    data get key
  }

  def get(key: String, field: String) = {
    data.get(key).asInstanceOf[JSONObject].get(field)
  }
}