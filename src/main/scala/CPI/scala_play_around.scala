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

object Hello {
  def main(args: Array[String]) = {
    val A=Array.fill(8){1}
    val cvt=new base_converter(A)
    println(cvt.getClass)
    println(cvt.bin2dec(A))
    println(cvt.print_state())

  }

  def bin2dec(in:Array[Int]): Int={
    val array_length=in.length
    var dec=0
    for(idx<-0 until array_length){
      dec=dec+in(idx)*scala.math.pow(2,idx).toInt
    }
    dec
  }
}
object play_around extends App{
  //val sccb=Module(new SCCB_interface(450,35))
  println(scala.math.pow(4,2).toInt)

}