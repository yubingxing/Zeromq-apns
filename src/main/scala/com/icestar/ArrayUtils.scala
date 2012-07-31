package com.icestar

/**
 * Array utils
 * @author IceStar
 */
object ArrayUtils {
  def qsort(arr: Array[Int]): Array[Int] = {
    if (arr.length <= 1)
      arr;
    else {
      val pivot = arr(arr.length / 2)
      Array.concat(
        qsort(arr filter (pivot >)),
        arr filter (pivot ==),
        qsort(arr filter (pivot <)))
    }
  }
}