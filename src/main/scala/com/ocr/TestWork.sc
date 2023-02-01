import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.io.File
import scala.sys.process.BasicIO.BufferSize

 def drainStream(source: InputStream): Array[Byte] = {
  val byteArrays = mutable.ListBuffer[Array[Byte]]()

  val buffer = new Array[Byte](BufferSize)
  while (true) {
    val n = source.read(buffer)

    if (n > 0) {
      byteArrays.+=(buffer.slice(0, n))
    } else if (n == -1) {
      source.close

      val nBytes = byteArrays.foldLeft(0) { (s, arr) => s + arr.length }
      val ret = new Array[Byte](nBytes) // We are copying all the item from each list item into the Array object
      val result = byteArrays.foldLeft(ret) {
        (state: Array[Byte],item)=>  state ++ item
      }
      return result
    }
  }

  ???
}

