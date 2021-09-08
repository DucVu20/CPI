package CPI
import chisel3._
import chisel3.util._

class base_converter(x: Array[Int]) {
  def bin2dec(in: Array[Int]): Int = {
    val array_length = in.length
    var dec = 0
    for (idx <- 0 until array_length) {
      dec = dec + in(idx) * scala.math.pow(2, idx).toInt
    }
    dec
  }

  def print_state(): Unit = {
    println("Hello ")
  }
}
class referenceFrame(){

  def generateRandomFrame(ImageResolution:Int, ImageFormat: Int): Array[Int]={
    if(ImageFormat==0){
      val refFrame=Array.fill(ImageResolution){scala.util.Random.nextInt(255)}
      return refFrame
    }
    else {
      val refFrame=Array.fill(ImageResolution){scala.util.Random.nextInt(65535)}
      return refFrame
    }
  }

  def pixelStream(idx:Int,refFrame: Array[Int],
                           ImageFormat:Int,
                           pclk: Int): Int ={
    if(ImageFormat==0){
      return refFrame(idx)
    }
    else {
      var firstByte = refFrame(idx)>>8
      var secondByte = refFrame(idx)&0xFF
      if (pclk == 0) {
        return firstByte
      }
      else {
        return secondByte
      }
    }
  }

  def validate(idx: Int , refFrame: Array[Int]): Int={
    return refFrame(idx)
  }
}
import scala.reflect.macros.Context
import scala.language.experimental.macros


object play_around extends App{
  println(System.getProperty("user.dir"))

}