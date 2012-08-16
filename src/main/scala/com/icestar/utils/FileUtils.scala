package com.icestar.utils
import java.io.File
import java.io.PrintStream

/**
 * File utils
 * @author IceStar
 */
object FileUtils {
  def deleteTempDir(path: File): Boolean = {
    if (path.exists()) {
      val files = path.listFiles()
      files.foreach(f => {
        if (f.isFile())
          f.delete()
        else
          deleteTempDir(f)
      })
    }
    path.delete()
  }

  def createTempDir(namePrefix: String): File = {
    val d = File.createTempFile(namePrefix, "")
    d.delete
    d.mkdir
    d
  }

  def writeTextFile(path: File, content: String) {
    val out = new PrintStream(path, "UTF-8")
    out.print(content)
    out.flush()
    out.close()
  }
}