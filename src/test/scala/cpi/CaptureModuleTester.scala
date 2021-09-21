package sislab.cpi

import chisel3._
import org.scalatest._
import chiseltest._

class referenceFrame{

  def generateRandomFrame(ImageResolution:Int, ImageFormat: Int): Array[Int]={
    if(ImageFormat == 0){
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(255)}
      return refFrame
    }
    else {
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(65535)}
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


class CaptureModuleChiselTest extends FlatSpec with ChiselScalatestTester{
  behavior of "Capture module"

  def CaptureModuleTest[T <: CaptureModule](dut: T, n :Int)={

    val width = dut.w
    val height = dut.h
    val pclock = n
    var nTestPassed = 0

    println("generate a random frame")
    val tp = 2 * pclock
    val t_line = 10 * pclock
    //====================synthesized timing========================//

    for (imageFormat <- 0 until(2)) {
      dut.io.imageFormat.poke(imageFormat.U)
      val refFrame = new referenceFrame().generateRandomFrame(height * width, imageFormat)

      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(10)
      dut.io.capture.poke(true.B)
      dut.clock.step(2)
      dut.io.capture.poke(false.B)
      dut.io.vsync.poke(true.B)
      dut.io.href.poke(false.B)
      dut.clock.step(3 * t_line)
      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(t_line)

      var idx = 0
      var pclk = true
      for (col <- 0 until(width)){
        dut.io.href.poke(true.B)
        for (row <- 0 until(height)){
          for (plkClock<- 0 until(imageFormat+1)){
            var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
              imageFormat, plkClock)

            dut.io.href.poke(true.B)
            dut.io.vsync.poke(false.B)
            pclk = !pclk
            dut.io.pclk.poke(pclk.asBool())
            if (pclk == false){
              dut.io.pixelIn.poke(pixelIn.asUInt())
            }
            dut.clock.step(pclock/2)
            pclk = !pclk
            dut.io.pclk.poke(pclk.asBool())
            dut.clock.step(pclock/2)
          }
          idx = idx + 1
        }
        dut.io.href.poke(false.B)
        dut.clock.step(2*tp)
      }
      dut.clock.step(tp)
      dut.io.vsync.poke(true.B)
      dut.clock.step(tp)

      //====================validation=======================//

      while (dut.io.frameFull.peek.litToBoolean) {
        dut.io.readFrame.poke(true.B)
        dut.clock.step(1)

        var idx_out = dut.io.pixelAddr.peek.litValue.toInt // pixel_address
        var refPixelVal = new referenceFrame().validate(idx_out, refFrame).toInt
        dut.io.pixelOut.expect(refPixelVal.U)
        if(refPixelVal==dut.io.pixelOut.peek.litValue.toInt){
          nTestPassed += 1
        }
      }
      dut.io.readFrame.poke(false.B)
      dut.clock.step(50)
    }
    Console.out.println(Console.YELLOW+"CPI testing result: "+nTestPassed+
      " tests passed over "+ Console.YELLOW+width*height*2+ " being tested"+
      Console.RESET)
  }

  it should "pass" in {
    test(new CaptureModule(15, 12, 2, 30*32))
    { dut => CaptureModuleTest(dut,4)}
  }
}