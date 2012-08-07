package com.icestar
import java.io.File
import java.io.PrintStream

/**
 * File utils
 * @author IceStar
 */
object FileUtils {
  def deleteDirectory(path: File): Boolean = {
    if (path.exists()) {
      val files = path.listFiles()
      files.foreach(f => {
        if (f.isFile())
          f.delete()
        else
          deleteDirectory(f)
      })
    }
    path.delete()
  }

  def writeTextFile(path: File, content: String) {
    val out = new PrintStream(path, "UTF-8")
    out.print(content)
    out.flush()
    out.close()
  }
}