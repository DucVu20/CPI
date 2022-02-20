package sislab.cpi

import chisel3._
import chisel3.experimental.BaseModule
import chisel3.iotesters._
import chisel3.util._
import firrtl.Implicits.int2WInt

import scala.io.Source

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

object CatDog {
  implicit val numberOfCats: Int = 3
  //implicit val numberOfDogs: Int = 5

  def tooManyCats(nDogs: Int)(implicit nCats: Int): Boolean = nCats > nDogs

  val imp = tooManyCats(2)    // Argument passed implicitly!
  val exp = tooManyCats(2)(5) // Argument passed explicitly!
}

class CPIPortIO1 extends Bundle{
  val SIOC    = Output(Bool())
  val SIOD    = Output(Bool())
  val pclk    = Input(Bool())
  val href    = Input(Bool())
  val vsync   = Input(Bool())
  val pixelIn = Input(UInt(8.W))
  val XCLK    = Output(Clock())
}

object rgb extends App{
  val pixel = 0x1b7d35
  val firstByte  = pixel >> 16
  val secondByte = (pixel & 0xFF00) >> 8
  val thirdByte  = pixel & 0xFF
  println(firstByte.toHexString, secondByte.toHexString, thirdByte.toHexString)
}

